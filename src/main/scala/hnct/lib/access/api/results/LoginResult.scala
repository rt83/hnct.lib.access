package hnct.lib.access.api.results

import hnct.lib.access.api.AccessRequest
import hnct.lib.access.api.User


/**
 * A LoginResult stores the result of logging in using
 * an AccessRequest. Each AccessProcessor might return a different type of
 * LoginResult
 *  
 */

class LoginResultCode extends ActionResultCode {
	// When user is not found from data source
	final case object FAILED_USER_NOT_FOUND extends LoginResultCode
	// When username validation doesn't pass
	final case object FAILED_INVALID_USERNAME extends LoginResultCode
	// When password validation doesn't pass or password doesn't match
	final case object FAILED_INVALID_PASSWORD extends LoginResultCode
	// When we don't want to reveal the cause of failed login
	final case object FAILED_INVALID_CREDENTIALS extends LoginResultCode
	// When some errors happen
	final case object FAILED_SYSTEM_ERROR extends LoginResultCode
}

object LoginResultCode extends LoginResultCode

trait LoginResult extends ActionResult {
	
	val UNLIMITED = -1
	
	/**
	 * Get the token correspond to the login
	 *
	 * The token generated if the login is successful
	 * AccessProcessor can decide whether to generate a token for a login
	 */
	def token : Option[String] = None
	
	/**
	 * The time until the login is timeout, default is never time out
	 */
	def timeout : Long = UNLIMITED
	
	/**
	 * Return the user data correspond to the username within the access request
	 */
	def user : Option[User] = None
	
}