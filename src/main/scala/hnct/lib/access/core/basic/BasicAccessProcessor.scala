package hnct.lib.access.core.basic

import java.util.Date

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import hnct.lib.access.api._
import hnct.lib.access.api.results._
import hnct.lib.access.core.util.AccessKeyGenerator
import hnct.lib.session.api._
import hnct.lib.utility.Logable

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * A basic implementation of access processor. It requires a configuration to be made available through
 * assisted injection.
 * 
 * Beside, it requires set of available DataAdapter and PasswordHaser. Application that use this BasicAccessProcessor
 * should provide the instances of DataAdapter and PasswordHasher through a set binding.
 */
class BasicAccessProcessor @Inject() (
		
		@Assisted() override protected[this] val _config : AccessProcessorConfig,
		private val sessionContainer : SessionContainer,
		val adapters : java.util.Set[DataAdapter],
		val hashers : java.util.Set[PasswordHasher]
		
	) extends AccessProcessor with Logable {

	override val ART : Class[_ <: AccessRequest] = classOf[BasicAccessRequest]
	override val UT = classOf[User]

	implicit private def convertRequest(req : AccessRequest) : BasicAccessRequest = req.asInstanceOf[BasicAccessRequest]

	final val TOKEN_KEY = "_token"
	
	{
		if (!_config.isInstanceOf[BasicAccessProcessorConfig]) 
			throw new RuntimeException("BasicAccessProcessor needs BasicAccessProcessorConfig."+ 
				" Receiving "+_config.getClass.getName+" instead");
		
		val c = _config.asInstanceOf[BasicAccessProcessorConfig];
		
		/** retrieve the data adapters and the password hasher **/
		
		this.dataAdapter = asScalaSet(adapters).find(_.getClass().equals(c.dataAdapterClass)).getOrElse(
			throw new RuntimeException("No Data adapter is bound for "+c.dataAdapterClass.getName())
		)
		
		/** Retrieve password hasher if one is requested in the config. If not, use a default password hasher which don't hash at all **/
		this.hasher = c.hasher.map { x => asScalaSet(hashers).find(_.getClass().equals(x)).getOrElse(
				throw new RuntimeException("No Password hasher is bound for "+x.getName())
			)
		} getOrElse {
			// the default hasher when there is no hasher class defined returns an unchanged token
			new PasswordHasher {
				
				def hash(request : AccessRequest, user : User) = request.token.getOrElse("")
				
			}
		}
	}
	
	
	/********************************************************/
	
	/**
	 * Authenticate take a basic access request and tell whether the 
	 * the authentication is successful. It does this by retrieving the login session
	 * of the request. If there is no login session, the user didn't login before.
	 * If there is a login session, the access token must be present in the session and must be
	 * the same as what the user is submitting.
	 */
	override def authenticate(req: Option[AccessRequest]): Future[BasicActionResult] = {
		req map { r =>
			
			val br = r.asInstanceOf[BasicAccessRequest]
			
			loginSessionAccessor(br).        // retrieve the login session, if any.
				fold(Future.successful(BasicActionResult(Some(br), AuthenticateResultCode.FAILED_SESSION_NOT_FOUND, "No accessor available")))  // fold = if there is no session, we return failed result
			{
				accessor => accessor.read[String](TOKEN_KEY) map {  // if have accessor, read the token, this is a future

					_.map { sv =>      // the future hold an Option[SessionValue[String]], have to map it to the action result
						if (r.token.isDefined && sv.value.equals(r.token.get))
							BasicActionResult(Some(br), ActionResultCode.SUCCESSFUL, "Authentication successful!")
						else BasicActionResult(Some(br), "Access token is not correct!")

					} getOrElse (BasicActionResult(Some(br), "Access token not found!"))  // if the read doesn't return any session value, we return failed by default

				}
			}
		} getOrElse {
			Future.successful(BasicActionResult(None, AuthenticateResultCode.FAILED_REQUEST_NOT_FOUND, "No access request found"))
		}
	}

	/**
	 * Do the login for the access request
	 * Depending on whether session is used (configured in the config file)
	 * the access token will be written into the session
	 */
	override def login(req: Option[AccessRequest]): Future[LoginResult] = {
		req map { r =>
			
			val br = r.asInstanceOf[BasicAccessRequest]
			
			br.username.fold(Future.successful(BasicLoginResult(Some(br), LoginResultCode.FAILED_USER_NOT_FOUND)))(uname =>
				dataAdapter.findUserByUsername(uname) map { u =>
					
					u.fold(BasicLoginResult(Some(br), LoginResultCode.FAILED_USER_NOT_FOUND))(user =>
						
						if (loginPass(br, user)) {
							
							val result = BasicLoginResult(Some(br), Some(user), LoginResultCode.SUCCESSFUL, Some(calculateToken(br, Some(user))))
							
							if (config.useSession)  // if we use session, write the user login info session
								writeSessionOnSuccessLogin(result)
							
							result
						} else BasicLoginResult(Some(br), Some(user), LoginResultCode.FAILED_INVALID_PASSWORD, None)
					
					)
					
				}
			)
		} getOrElse {
			Future.successful(BasicLoginResult(None, None, LoginResultCode.FAILED_REQUEST_NOT_FOUND, None))
		}


	}
	
	/**
	 * This method assess whether the particular access request match with a particular
	 * user. This method allow inheriting class to customize the way to check if a 
	 * login pass the check
	 */
	protected def loginPass(req : BasicAccessRequest, user : User) : Boolean = {
		hasher.hash(req, user).equals(user.password)
	}
	
	/**
	 * Generate token from the access request and from user data if there is any
	 * This method is used when the user logged in successfully and
	 * we want to calculate the token for writing into the session.
	 * 
	 * Sub class can override this method to customize the way the token is calculated
	 */
	protected def calculateToken(req : BasicAccessRequest, user : Option[User]) : String = {
		
		AccessKeyGenerator.timeToken(req.username.get, req.token.get, new Date(System.currentTimeMillis()))
		
	}
	
	/**
	 * Write the user information to session
	 * This method allow the inheriting class to customize the way to write the user
	 * information into session when login successful
		*
		* This method should only be called when user has successfully logged in.
	 */
	protected def writeSessionOnSuccessLogin(result : LoginResult) : Future[Boolean] = {
		
		loginSessionAccessor(result.request.get) map { accessor =>

			result.token map { token =>
				val timeout = if (result.request.get.timeout == -1) loginTimeout else result.request.get.timeout
				if (timeout == -1) accessor.write(TOKEN_KEY, token)	// implicit conversion to session value is used here
				else accessor.write(TOKEN_KEY, (token, timeout)) 	// implicit conversion to session value is used here
				
			} getOrElse(Future.failed(new RuntimeException("Invalid login result being written!")))
			
		} getOrElse(Future.failed(new RuntimeException("Unable to get login session accessor of the request!")))
		
	}

	def loginSessionAccessor(req: AccessRequest): Option[SessionAccessor] = {
		if (config.useSession) {
			if (req.username.isEmpty) None
			else config.sessionUnit.fold(sessionContainer.getSession())(sessionContainer.getSession(_)).map { ses =>
				ses.accessor(AccessorDescriptor(config.sessionNamespace, req.username.get))
			}
		} else None
	}

	def loginTimeout: Long = config.asInstanceOf[BasicAccessProcessorConfig].loginTimeout

	def loginTimeout_=(timeout: Long): Unit = config.asInstanceOf[BasicAccessProcessorConfig].loginTimeout = timeout

	override def logout(req: Option[AccessRequest]): Future[LogoutResult] = {
		
		req map { r =>
			val br = r.asInstanceOf[BasicAccessRequest]
			
			authenticate(req) flatMap { actionResult =>
				
				if (actionResult.status != ActionResultCode.SUCCESSFUL)
					Future.successful(new BasicLogoutResult(Some(br), LogoutResultCode.FAILED_NOT_AUTHENTICATED))
				else loginSessionAccessor(br) map { accessor =>    // if we have the accessor
					
					accessor.delete(TOKEN_KEY) map { deleteResult =>
						if (deleteResult) new BasicLogoutResult(Some(br), LogoutResultCode.SUCCESSFUL)
						else new BasicLogoutResult(Some(br), LogoutResultCode.FAILED_UNABLE_TO_REMOVE_SESSION_KEY)
					}
					
				} getOrElse (Future.failed(new RuntimeException("No session accessor available for the access request! Is the session lib configured correctly?")))
			}
		} getOrElse {
			Future.successful(new BasicLogoutResult(None, LogoutResultCode.FAILED_REQUEST_NOT_FOUND))
		}
	}
	
	/**
	 * renew the login
	 */
	override def renewLogin(req: Option[AccessRequest]): Future[BasicActionResult] = {
		
		req map { r =>
			val br = r.asInstanceOf[BasicAccessRequest]
			authenticate(req) flatMap { actionResult =>
				if (actionResult.status != ActionResultCode.SUCCESSFUL)
					Future.successful(BasicActionResult(Some(br), LogoutResultCode.FAILED_NOT_AUTHENTICATED, "Cannot renew login for unauthenticated request!"))
				
				else loginSessionAccessor(br) map { accessor =>
					accessor.renew(TOKEN_KEY) map { renewResult =>  // renew the time to live of the token in the session
						if (renewResult) BasicActionResult(Some(br), ActionResultCode.SUCCESSFUL, "Successfully renew the login!")
						else BasicActionResult(Some(br), "Unable to renew to login session!")
					}
				} getOrElse (Future.failed(new RuntimeException("No session accessor available for the access request! Is the session lib configured correctly?")))
			}
		} getOrElse {
			Future.successful(BasicActionResult(None, ActionResultCode.FAILED_REQUEST_NOT_FOUND, "Unable to find the access request"))
		}
	}
}