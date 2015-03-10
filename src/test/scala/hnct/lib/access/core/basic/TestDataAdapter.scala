package hnct.lib.access.core.basic

import hnct.lib.access.api.DataAdapter
import hnct.lib.access.api.User

/**
 * A test data adapter which returns a user when the username start
 * with a character from 'a' to 'm'
 * 
 * The password will be the same as the username
 */
class TestDataAdapter extends DataAdapter {
	
	def findUserByUsername(username: String): Option[User] = {
		if (('a' to 'm') exists { c => 
			username.startsWith(s"$c") 
		}) Some(new User(username, username))
		else None

	}
	
}

object TestDataAdapter {
	
	def main(args : Array[String]) : Unit = {
		val adapter = new TestDataAdapter
		
		adapter.findUserByUsername("test")
		println("\n")
		adapter.findUserByUsername("bryan")
	}
	
}