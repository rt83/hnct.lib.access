package hnct.lib.access.api

/**
 * A LoginResult stores the result of logging in using
 * an AccessRequest. Each AccessProcessor might return a different type of
 * LoginResult
 *  
 */

class LoginResultCode 


trait LoginResult[T <: AccessRequest, U <: User] {
	
	val UNLIMITED = -1
	
	protected var _request : T
	protected var _user : Option[U]
	protected var _successful : Boolean
	protected var _timeout : Long = UNLIMITED
	
	/**
	 * Get the request that associated with this login
	 */
	def request = _request
	/**
	 * Set the request that associated with this login
	 */
	protected def request_=(req : T) = _request = req
	
	/**
	 * Tell whether a particular login with an AccessRequest is successful
	 */
	def successful = _successful
	/**
	 * When an AccessProcess log a user in and return
	 * a LoginResult, it has to tell the caller whether the login is successful
	 * by calling this setter
	 */
	protected def successful_=(s : Boolean) = {_successful = s}
	
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