package hnct.lib.access.core.util

import java.util.Date
import java.text.SimpleDateFormat
import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

object AccessKeyGenerator {
	
	/**
	 * Generate a token base on username password and date
	 */
	def timeToken(username : String, password : String, date : Date) : String = {
		
		val df = new SimpleDateFormat("ddMMyyyy-hh:mm")
		
		val dateString = df.format(date)
		
		val s = "%s:%s:%s".format(username, password, dateString)
				
		val md = MessageDigest.getInstance("MD5")
		val digested = md.digest(s.getBytes("utf-8"))
		
		new String(Hex.encodeHex(digested))
		
	}
	
}