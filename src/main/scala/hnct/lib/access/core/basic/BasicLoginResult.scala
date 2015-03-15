package hnct.lib.access.core.basic

import hnct.lib.access.api.LoginResult
import hnct.lib.access.api.User
import hnct.lib.access.api.LoginResultCode
import hnct.lib.access.api.ActionResultCode

class BasicLoginResult(_request : BasicAccessRequest, 
		_user : Option[User], 
		_status : ActionResultCode,
		_token : Option[String]) extends LoginResult[BasicAccessRequest, User] {
	
	request = _request
	user = _user
	status = _status
	token = _token

	def this(req : BasicAccessRequest) = this(req, None, LoginResultCode.FAILED_INVALID_CREDENTIALS, None) 
	
	def this(req : BasicAccessRequest, status : LoginResultCode) = this(req, None, status, None)
	
}