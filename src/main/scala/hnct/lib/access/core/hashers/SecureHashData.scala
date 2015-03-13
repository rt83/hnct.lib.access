package hnct.lib.access.core.hashers

/**
 * Secure hash for user.
 * 
 * Any UserType wants to have secure hash should implement this 
 * trait to provide methods that supply secure hash generation information
 * to the password hasher. The information provided by the UserType user data
 * (as specified by this trait) conform the suggestion in this article
 * 
 * https://nakedsecurity.sophos.com/2013/11/20/serious-security-how-to-store-your-users-passwords-safely/
 * 
 */
trait SecureHashData[UserType <: SecureHashData[UserType]] {
	
	var useSecure = false
	
	var salt : Option[String] = None
	
	var hashAlgorithm : Option[String] = None
	
	var hashIteration : Option[Int] = None
	
}