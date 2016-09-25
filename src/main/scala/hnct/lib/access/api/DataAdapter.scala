package hnct.lib.access.api

import scala.concurrent.Future

/**
 * DataAdapter is the trait which allows AccessProcessor
 * to retrieve user data from a particular sources. A data adapter
 * work with certain type of access request. It provides an interface
 * to query for user's related data, such as the User's profile
 * user's access permissions, resource and permission list, etc..
 * 
 * All DataAdapter should have a constructor with empty parameter list
 * so that it can be initialized by the AccessProcessorFactory
 */
trait DataAdapter {
	
	/**
	 * Find user from the data source and convert it to the 
	 * standard User data structure.
	 * @param username the user name of the user being looking for
	 * @return Some[User] if the user is found, None if no user is found
	 */
	def findUserByUsername(username : String) : Future[Option[User]]
	
}