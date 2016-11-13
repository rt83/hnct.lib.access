package hnct.lib.access.api.results

/**
  * Created by ryan on 11/11/2016.
  */
class AuthenticateResultCode extends ActionResultCode {

	final case object FAILED_SESSION_NOT_FOUND extends AuthenticateResultCode
	final case object FAILED_NOT_AUTHENTICATED extends AuthenticateResultCode

}

object AuthenticateResultCode extends AuthenticateResultCode

trait AuthenticateResult extends ActionResult {

}
