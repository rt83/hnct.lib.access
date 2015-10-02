package hnct.lib.access.playframework

/**
 * @author Ryan
 * 
 * These are all the related exceptions
 */
case class AccessProcessorNotFound(message : String, apUnitName : String, cause : Throwable) extends Exception(message, cause) {
	
	def this(message : String, apUnit : String) = this(message, apUnit, null)
	
}