package hnct.lib.access.api

/**
 * PasswordHasher hashes the password submitted in an access request
 * into the correct form so that it can be compared with the hash
 * stored in the user data.
 * 
 * The conversion might need data associated with the user to hash the
 * password, for example, salt, and repeat as described in
 * 
 * https://nakedsecurity.sophos.com/2013/11/20/serious-security-how-to-store-your-users-passwords-safely/
 * 
 * if we want to store the passwords really securely
 * 
 * Normally, PasswordHasher has to work with a suitable DataAdapter and AccessRequestBuilder
 * The data adapter and request builder are the ones knowing how to get user data from the database and build
 * suitable access request to be used for the PasswordHasher
 * 
 * All classes implement this trait should have a constructor with empty parameter
 * list
 */
trait PasswordHasher {
	
	/**
	 * Hash the password stored inside the AccessRequest using information
	 * in the access request, together with the user data
	 */
	def hash(request : AccessRequest, userData : User) : String
	
}