package hnct.lib.access.core.basic

import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.User
import hnct.lib.access.api.results.LogoutResult
import hnct.lib.access.api.results.LoginResult
import hnct.lib.access.api.results.LoginResultCode
import hnct.lib.session.api._
import hnct.lib.access.core.util.AccessKeyGenerator
import java.util.Date
import hnct.lib.utility.Logable
import hnct.lib.access.api.results.ActionResultCode
import hnct.lib.access.api.results.LogoutResultCode

class BasicAccessProcessor extends AccessProcessor[BasicAccessProcessorConfig, User, BasicAccessRequest] with Logable {

	override type ConfigType = BasicAccessProcessorConfig
	
	final val TOKEN_KEY = "_token"

	def authenticate(req: BasicAccessRequest): BasicActionResult = {
		
		val failed = new BasicActionResult(req)
		
		loginSessionAccessor(req).fold(failed) { accessor =>
			accessor.read[String](TOKEN_KEY).fold (failed) { sessionValue =>
				if (sessionValue.value.equals(req.token))
					new BasicActionResult(req, ActionResultCode.SUCCESSFUL)
				else failed
			}
		}
	}

	/**
	 * Do the login for the access request
	 * Depending on whether session is used (configured in the config file)
	 * the access token will be written into the session
	 */
	def login(req: BasicAccessRequest): LoginResult[BasicAccessRequest, User] = {
		val u = dataAdapter.findUserByUsername(req.username)
		
		u.fold(new BasicLoginResult(req, LoginResultCode.FAILED_USER_NOT_FOUND))({user =>
			
			if (loginPass(req, user)) {
				
				val result = new BasicLoginResult(req, Some(user), LoginResultCode.SUCCESSFUL, Some(calculateToken(req, Some(user))))
				
				if (config.useSession)	// if we use session, write the user login info session
					writeSessionOnSuccessLogin(result)
				
				result
			} else new BasicLoginResult(req, Some(user), LoginResultCode.FAILED_INVALID_PASSWORD, None)			
		})

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
		
		AccessKeyGenerator.timeToken(req.username, req.token, new Date(System.currentTimeMillis()))
		
	}
	
	/**
	 * Write the user information to session
	 * This method allow the inheriting class to customize the way to write the user
	 * information into session when login successful
	 */
	protected def writeSessionOnSuccessLogin(result : LoginResult[BasicAccessRequest, User]) : Unit = {
		
		loginSessionAccessor(result.request).map { accessor =>
			
			result.token.map { token =>
				val timeout = if (result.request.timeout == -1) loginTimeout else result.request.timeout
				
				if (timeout == -1) accessor.write(TOKEN_KEY, token)	// implicit conversion to session value is used here
				else accessor.write(TOKEN_KEY, (token, timeout)) 	// implicit conversion to session value is used here			
			}
			
		}
		
	}

	def loginSessionAccessor(req: BasicAccessRequest): Option[SessionAccessor] = {
		if (config.useSession) {
			config.sessionUnit.fold(SessionFactory.getSession())(SessionFactory.getSession(_)).map { 
				_.accessor(SessionAccessorConfig(config.sessionNamespace, req.username))
			}
		} else None
	}

	def loginTimeout: Long = config.loginTimeout

	def loginTimeout_=(timeout: Long): Unit = config.loginTimeout = timeout

	def logout(req: BasicAccessRequest): LogoutResult[BasicAccessRequest] = {
		
		if (authenticate(req).status != ActionResultCode.SUCCESSFUL) 
			return new BasicLogoutResult(req, LogoutResultCode.FAILED_NOT_AUTHENTICATED)
		
		loginSessionAccessor(req).
			// we already authenticated the request above, but then its session is not found
			// probably some other thread has logged out the user already
			fold(new BasicLogoutResult(req, LogoutResultCode.FAILED_ALREADY_LOGGED_OUT))
			{ accessor =>
			
				if (accessor.delete(TOKEN_KEY))
					new BasicLogoutResult(req, LogoutResultCode.SUCCESSFUL)
				else new BasicLogoutResult(req, LogoutResultCode.FAILED_UNABLE_TO_REMOVE_SESSION_KEY)
			}
	}
	
	/**
	 * renew the login
	 */
	def renewLogin(req: BasicAccessRequest): BasicActionResult = {
		if (authenticate(req).status != ActionResultCode.SUCCESSFUL) 
			return new BasicActionResult(req, LogoutResultCode.FAILED_NOT_AUTHENTICATED)
		
		loginSessionAccessor(req).
			
			fold(new BasicActionResult(req, ActionResultCode.FAILED)) { accessor =>
				if (accessor.renew(TOKEN_KEY))	// renew the time to live of the token in the session
					new BasicActionResult(req, ActionResultCode.SUCCESSFUL)
				else new BasicActionResult(req)
			}
	}

}