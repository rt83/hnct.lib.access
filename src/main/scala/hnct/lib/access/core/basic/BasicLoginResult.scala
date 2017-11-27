package hnct.lib.access.core.basic

import hnct.lib.access.api.results.LoginResult
import hnct.lib.access.api.User
import hnct.lib.access.api.results.LoginResultCode
import hnct.lib.access.api.results.ActionResultCode

case class BasicLoginResult(
		override val request : Option[BasicAccessRequest],
		override val user : Option[User], 
		override val status : ActionResultCode,
		override val token : Option[String]) extends LoginResult

object BasicLoginResult {
	
	def apply(req : Option[BasicAccessRequest]) = new BasicLoginResult(req, None, LoginResultCode.FAILED_INVALID_CREDENTIALS, None)
	
	def apply(req : Option[BasicAccessRequest], status : LoginResultCode) = new BasicLoginResult(req, None, status, None)
	
}