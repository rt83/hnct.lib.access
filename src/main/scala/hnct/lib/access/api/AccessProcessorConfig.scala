package hnct.lib.access.api

/**
 * Represent a configuration of an AccessProcessor
 * 
 */
abstract class AccessProcessorConfig(var tokenHashMethod : String) {

	/**
	 * The class name of the user data adapter. This class is used
	 * to initialize the adapter which help retrieve user information / access data
	 */
	var _userDataAdapterClass : Class[_ <: DataAdapter] = null
	
	/**
	 * The name of the session unit being use for the AccessProcessor
	 * Can be None if use the default session unit
	 */
	var _sessionUnit : Option[String] = None
	
	/**
	 * Whether or not we should use session to store the user login session
	 * In some access manager, session is not needed
	 */
	var _useSession = true
	
	/**
	 * If we use session, what is the namespace for the session
	 * Session might be use to store many things for many users / services
	 * So we might want to namespace it. For example, user data might be stored
	 * in name space "usersession" while service data might be stored in names pace
	 * "servicesession"
	 */
	var _sessionNamespace : String = "usersession"
	
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
	
	def sessionUnit = _sessionUnit
	def sessionUnit_=(name : String) = { 
		_sessionUnit = if (name == null || name.isEmpty()) None else Some(name)
	}
	
	def useSession = _useSession
	def useSession_=(use : Boolean) = { 
		_useSession = use
	}
	
	def sessionNamespace = _sessionNamespace
	def sessionNamespace_=(namespace : String) = { 
		_sessionNamespace = namespace
	}
	
}

object AccessProcessorConfig {
	
	val DEFAULT_TOKEN_HASH_METHOD = "PLAIN"
	
}