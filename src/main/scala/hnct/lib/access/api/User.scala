package hnct.lib.access.api

/**
 * User is the base class for all classes representing user data
 * It contains two basic pieces of information namely, username and password
 */
class User {
	
	private var _username, _password : String = ""
	
	/**
	 * Getter for username
	 */
	def username = _username
	/**
	 * Setter for username
	 */
	def username_=(un : String) = _username = un
	
	/**
	 * Getter for password
	 */
	def password = _password
	/**
	 * Setter for password
	 */
	def password_=(pass : String) = _password = pass
	
}