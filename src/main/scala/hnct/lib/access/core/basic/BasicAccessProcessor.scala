package hnct.lib.access.core.basic

import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.User
import hnct.lib.access.api.LogoutResult
import hnct.lib.access.api.LoginResult
import hnct.lib.access.api.LoginResultCode
import hnct.lib.session.api._
import hnct.lib.access.core.util.AccessKeyGenerator
import java.util.Date

class BasicAccessProcessor extends AccessProcessor {

	type ConfigType = BasicAccessProcessorConfig
	type AccessRequestType = BasicAccessRequest
	type UserType = User
	
	final val TOKEN_KEY = "_token"

	def authenticate(req: AccessRequestType): Boolean = {
		loginSessionAccessor(req).map { accessor =>
			accessor.read[String](TOKEN_KEY).map {
				_.value.equals(req.token)
			} .getOrElse(false)
		} .getOrElse(false)
	}

	/**
	 * Do the login for the access request
	 * Depending on whether session is used (configured in the config file)
	 * the access token will be written into the session
	 */
	def login(req: AccessRequestType): LoginResult[AccessRequestType, UserType] = {
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
	protected def loginPass(req : AccessRequestType, user : UserType) : Boolean = {
		req.token.equals(user.password)
	}
	
	/**
	 * Generate token from the access request and from user data if there is any
	 * This method is used when the user logged in successfully and
	 * we want to calculate the token for writing into the session.
	 * 
	 * Sub class can override this method to customize the way the token is calculated
	 */
	protected def calculateToken(req : AccessRequestType, user : Option[UserType]) : String = {
		
		AccessKeyGenerator.timeToken(req.username, req.token, new Date(System.currentTimeMillis()))
		
	}
	
	/**
	 * Write the user information to session
	 * This method allow the inheriting class to customize the way to write the user
	 * information into session when login successful
	 */
	protected def writeSessionOnSuccessLogin(result : LoginResult[AccessRequestType, UserType]) : Unit = {
		
		val session = config.sessionUnit.fold(SessionFactory.getSession())(SessionFactory.getSession(_))
		
		session.map { sess =>
			
			val accessor = sess.accessor(SessionAccessorSpecification(config.sessionNamespace, result.request.username))
			
			result.token.map { token =>
				val timeout = if (result.request.timeout == -1) loginTimeout else result.request.timeout
				
				if (timeout == -1) accessor.write(TOKEN_KEY, token)	// implicit conversion to session value is used here
				else accessor.write(TOKEN_KEY, (token, timeout)) 	// implicit conversion to session value is used here
			}
			
		}
		
	}

	def loginSessionAccessor(req: AccessRequestType): Option[SessionAccessor] = {
		if (config.useSession) {
			config.sessionUnit.fold(SessionFactory.getSession())(SessionFactory.getSession(_)).map { 
				_.accessor(SessionAccessorSpecification(config.sessionNamespace, req.username))
			}
		}
		
		None
	}

	def loginTimeout: Long = config.loginTimeout

	def loginTimeout_=(timeout: Long): Unit = config.loginTimeout = timeout

	def logout(req: AccessRequestType): LogoutResult[AccessRequestType] = {
		???
	}

	def renewLogin(req: AccessRequestType): Unit = {
		???
	}

}