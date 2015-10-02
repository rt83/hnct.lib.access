package hnct.lib.access.core.basic

import hnct.lib.access.api.results.LoginResult
import hnct.lib.access.api.User
import hnct.lib.access.api.results.LoginResultCode
import hnct.lib.access.api.results.ActionResultCode

class BasicLoginResult(
		override val request : BasicAccessRequest, 
		override val user : Option[User], 
		override val status : ActionResultCode,
		override val token : Option[String]) extends LoginResult[BasicAccessRequest, User] {

	def this(req : BasicAccessRequest) = this(req, None, LoginResultCode.FAILED_INVALID_CREDENTIALS, None) 
	
	def this(req : BasicAccessRequest, status : LoginResultCode) = this(req, None, status, None)
	
}