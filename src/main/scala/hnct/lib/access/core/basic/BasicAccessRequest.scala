package hnct.lib.access.core.basic

import hnct.lib.access.api.AccessRequest

/**
 * A basic access request
 * This access request represent the login request or an authentication request
 * 
 * When it is a login request, the token will have to be the user password
 * 
 * If timeout is set, it define how long the login session will last. If it is set to -1
 * the timeout defined by the AccessProcessor will be used. 
 */
class BasicAccessRequest(
		override val username : String, 
		override val token : String, 
		val timeout : Long) extends AccessRequest {
	
	def this(un : String, t : String) = this(un, t, -1)
	
}