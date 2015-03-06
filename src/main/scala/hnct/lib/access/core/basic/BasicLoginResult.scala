package hnct.lib.access.core.basic

import hnct.lib.access.api.LoginResult
import hnct.lib.access.api.User
import hnct.lib.access.api.LoginResultCode

class BasicLoginResult(req : BasicAccessRequest, user : Option[User], status : LoginResultCode.Value) extends LoginResult[BasicAccessRequest, User] {
	
	var _request = req
	var _user = user
	var _status = status
	
}