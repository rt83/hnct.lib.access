package hnct.lib.access.example

import hnct.lib.access.api.DataAdapter
import hnct.lib.access.api.User
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex
import scala.concurrent.Future

/**
 * A test data adapter which returns a user when the username start
 * with a character from 'a' to 'm'
 * 
 * The password will be the same as the username
 */
class TestDataAdapter extends DataAdapter {
	
	def md5(s : String) = {
		val md = MessageDigest.getInstance("MD5")
		val digested = md.digest(s.getBytes("utf-8"))
		
		new String(Hex.encodeHex(digested))
	}
	
	def findUserByUsername(username: String): Future[Option[User]] = {
		Future.successful(
			if (('a' to 'm') exists { c => 
				username.startsWith(s"$c") 
			}) Some(new User(username, md5(username)))
			else None
		)

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