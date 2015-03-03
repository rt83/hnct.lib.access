package hnct.lib.access.api

/**
 * Represent a configuration of an AccessProcessor
 * 
 */
abstract class AccessProcessorConfig(var tokenHashMethod : String) {

	var _userDataAdapterClass : Class[_ <: UserDataAdapter[_]] = null
	
	def this() = this(AccessProcessorConfig.DEFAULT_TOKEN_HASH_METHOD)
	
	def userDataAdapterClass = _userDataAdapterClass
	def userDataAdapterClass_=(className : String) = {
		try {
			_userDataAdapterClass = Class.forName(className, true, Thread.currentThread().getContextClassLoader).asInstanceOf
		}  catch {
			case e : ClassNotFoundException => 
				throw new RuntimeException(s"Cannot find the $className in the class path", e)
		}
	}
	
}

object AccessProcessorConfig {
	
	val DEFAULT_TOKEN_HASH_METHOD = "PLAIN"
	
}