package hnct.lib.access.api

/**
 * User is the base class for all classes representing user data
 * It contains two basic pieces of information namely, username and password
 */
class User(var username : String, var password : String) {
	
	def this(username : String) = this(username, "")
	
	def this() = this("", "")
	
}