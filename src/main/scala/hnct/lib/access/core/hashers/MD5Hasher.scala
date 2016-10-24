package hnct.lib.access.core.hashers

import hnct.lib.access.api.PasswordHasher
import hnct.lib.access.api.AccessRequest
import hnct.lib.access.api.User
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

/**
 * Simply return a MD5 hash of the password
 */
class MD5Hasher extends PasswordHasher {
	def hash(request: AccessRequest, userData: User): String = {
		
		val md = MessageDigest.getInstance("MD5")
		val digested = md.digest(request.token.getBytes("utf-8"))
		
		new String(Hex.encodeHex(digested))
		
	}
}