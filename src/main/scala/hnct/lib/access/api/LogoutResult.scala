package hnct.lib.access.api

/**
 * A LoginResult stores the result of logging in using
 * an AccessRequest. Each AccessProcessor might return a different type of
 * LoginResult
 *  
 */
trait LogoutResult[T <: AccessRequest] {
	
	protected var _request : T
	protected var _successful : Boolean
	
	/**
	 * Get the request that associated with this logout
	 */
	def request = _request
	/**
	 * Set the request that associated with this logout
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
	
}