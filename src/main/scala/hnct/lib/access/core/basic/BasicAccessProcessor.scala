package hnct.lib.access.core.basic

import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.User
import hnct.lib.access.api.LogoutResult
import hnct.lib.session.api.SessionAccessor
import hnct.lib.access.api.LoginResult

class BasicAccessProcessor extends AccessProcessor {

	type ConfigType = BasicAccessProcessorConfig
	type AccessRequestType = BasicAccessRequest
	type UserType = User

	def authenticate(req: BasicAccessRequest): Boolean = {
		???
	}

	def configure(config: BasicAccessProcessorConfig): Unit = {
		???
	}

	def login(req: BasicAccessRequest): BasicLoginResult = {
		val u = this.userDataAdapter.findUserByUsername(req.username)
		
		/*u.fold(new BasicLoginResult(req, u, false))({
			
			
			
		})*/
		
		???
	}

	def loginSessionAccessor(req: BasicAccessRequest): SessionAccessor = {
		???
	}

	def loginTimeout: Long = {
		???
	}

	def loginTimeout_=(timeout: Long): Unit = {
		???
	}

	def logout(req: BasicAccessRequest): LogoutResult[BasicAccessRequest] = {
		???
	}

	def renewLogin(req: BasicAccessRequest): Unit = {
		???
	}

}