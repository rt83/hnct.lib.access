package hnct.lib.access.core.basic

import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.User
import hnct.lib.access.api.LogoutResult
import hnct.lib.session.api.SessionAccessor
import hnct.lib.access.api.LoginResult
import hnct.lib.access.api.LoginResultCode

class BasicAccessProcessor extends AccessProcessor {

	type ConfigType = BasicAccessProcessorConfig
	type AccessRequestType = BasicAccessRequest
	type UserType = User
	
	override var _config : ConfigType = null

	def authenticate(req: AccessRequestType): Boolean = {
		???
	}
	
	def login(req: AccessRequestType): LoginResult[AccessRequestType, UserType] = {
		val u = this.userDataAdapter.findUserByUsername(req.username)
		
		u.fold(new BasicLoginResult(req, None, LoginResultCode.FAILED_USER_NOT_FOUND))({user =>
			
			if (loginPass(req, user)) {
				
				val result = new BasicLoginResult(req, Some(user), LoginResultCode.SUCCESSFUL)
				
				if (config.useSession)	// if we use session, write the user login info session
					writeSessionOnSuccessLogin(result)
				
				result
			} else new BasicLoginResult(req, Some(user), LoginResultCode.FAILED_INVALID_PASSWORD)			
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
	 * Write the user information to session
	 * This method allow the inheriting class to customize the way to write the user
	 * information into session when login successful
	 */
	protected def writeSessionOnSuccessLogin(result : LoginResult[AccessRequestType, UserType]) : Unit = {
		
		//TODO : write to session
		
	}

	def loginSessionAccessor(req: AccessRequestType): SessionAccessor = {
		???
	}

	def loginTimeout: Long = {
		???
	}

	def loginTimeout_=(timeout: Long): Unit = {
		???
	}

	def logout(req: AccessRequestType): LogoutResult[AccessRequestType] = {
		???
	}

	def renewLogin(req: AccessRequestType): Unit = {
		???
	}

}