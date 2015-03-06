package hnct.lib.access.core.basic

import hnct.lib.access.api.LoginResult
import hnct.lib.access.api.User

class BasicLoginResult(req : BasicAccessRequest, user : User, successful : Boolean) extends LoginResult[BasicAccessRequest, User] {
	
	var _request = req
	var _user = user
	var _successful = successful
	
}