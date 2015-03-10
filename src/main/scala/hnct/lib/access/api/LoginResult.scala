package hnct.lib.access.api

/**
 * A LoginResult stores the result of logging in using
 * an AccessRequest. Each AccessProcessor might return a different type of
 * LoginResult
 *  
 */

class LoginResultCode extends Enumeration {
	final val 
		SUCCESSFUL, 
		FAILED_USER_NOT_FOUND, 			// When user is not found from data source
		FAILED_INVALID_USERNAME,		// When username validation doesn't pass
		FAILED_INVALID_PASSWORD,		// When password validation doesn't pass or password doesn't match
		FAILED_INVALID_CREDENTIALS,		// When we don't want to reveal the cause of failed login
		FAILED_SYSTEM_ERROR
		= Value
}

object LoginResultCode extends LoginResultCode

trait LoginResult[T <: AccessRequest, U <: User] {
	
	val UNLIMITED = -1
	
	private[this] var _request : T = _
	private[this] var _user : Option[U] = None
	private[this] var _status : LoginResultCode.Value = LoginResultCode.FAILED_INVALID_CREDENTIALS
	private[this] var _timeout : Long = UNLIMITED
	
	/**
	 * The token generated if the login is successful
	 * AccessProcessor can decide whether to generate a token for a login
	 */
	private[this] var _token : Option[String] = None
	
	/**
	 * Get the request that associated with this login
	 */
	def request = _request
	/**
	 * Set the request that associated with this login
	 */
	protected def request_=(req : T) = _request = req
	
	/**
	 * Get the token correspond to the login
	 */
	def token = _token
	/**
	 * Set the token
	 */
	protected def token_=(t : Option[String]) = _token = t
	
	/**
	 * Tell whether a particular login with an AccessRequest is successful
	 */
	def status = _status
	/**
	 * When an AccessProcess log a user in and return
	 * a LoginResult, it has to tell the caller whether the login is successful
	 * by calling this setter
	 */
	protected def status_=(s : LoginResultCode.Value) = {_status = s}
	
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