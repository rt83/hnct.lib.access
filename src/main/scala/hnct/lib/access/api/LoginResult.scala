package hnct.lib.access.api

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

trait LoginResult[T <: AccessRequest, U <: User] extends ActionResult[T] {
	
	val UNLIMITED = -1
	
	private[this] var _user : Option[U] = None
	private[this] var _timeout : Long = UNLIMITED
	
	/**
	 * The token generated if the login is successful
	 * AccessProcessor can decide whether to generate a token for a login
	 */
	private[this] var _token : Option[String] = None
	
	/**
	 * Get the token correspond to the login
	 */
	def token = _token
	/**
	 * Set the token
	 */
	protected def token_=(t : Option[String]) = _token = t
	
	/**
	 * The time until the login is timeout
	 */
	def timeout = _timeout
	/**
	 * When an AccessProcessor log a user in successfully and return a LoginResult
	 * it needs to set the timeout to let the caller know when the login will expire
	 */
	protected def timeout_=(duration : Long) = {_timeout = duration}
	
	/**
	 * Return the user data correspond to the username within the access request
	 */
	def user = _user
	/**
	 * Set the user data. After the AccessProcessor log a user in successfully and return
	 * a LoginResult, it needs to set the user data corresponding to the username in the access
	 * request. This allow the caller to have the user data without having to query for it
	 * explicitly again.
	 */
	def user_=(u : Option[U]) = _user = u
	
}