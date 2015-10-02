package hnct.lib.access.api.results

import hnct.lib.access.api.AccessRequest

/**
 * A LoginResult stores the result of logging in using
 * an AccessRequest. Each AccessProcessor might return a different type of
 * LoginResult
 *  
 */
class LogoutResultCode extends ActionResultCode {
	
	// occurs when the user is already logged out
	final case object FAILED_ALREADY_LOGGED_OUT extends LogoutResultCode

	// when we can't remove the token key from the session
	final case object FAILED_UNABLE_TO_REMOVE_SESSION_KEY extends LogoutResultCode
	
}

object LogoutResultCode extends LogoutResultCode {

}

trait LogoutResult[T <: AccessRequest] extends ActionResult[T] {
	
}