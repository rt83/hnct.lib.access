package hnct.lib.access.api

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import com.fasterxml.jackson.annotation.JsonTypeInfo.As

/**
 * Represent a configuration of an AccessProcessor
 * 
 */
@JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY, property="_class")
abstract class AccessProcessorConfig() {

	/**
	 * The class name of the user data adapter. This class is used
	 * to initialize the adapter which help retrieve user information / access data
	 */
	var dataAdapterClass : Class[_ <: DataAdapter] = null
	
	/**
	 * The name of the session unit being use for the AccessProcessor
	 * Can be None if use the default session unit
	 */
	var sessionUnit : Option[String] = None
	
	/**
	 * Whether or not we should use session to store the user login session
	 * In some access manager, session is not needed
	 */
	var useSession = true
	
	/**
	 * If we use session, what is the namespace for the session
	 * Session might be use to store many things for many users / services
	 * So we might want to namespace it. For example, user data might be stored
	 * in name space "usersession" while service data might be stored in names pace
	 * "servicesession"
	 */
	var sessionNamespace : String = "usersession"
	
	/**
	 * The tokenHashMethod define how the input token will be hashed before used for comparison with
	 * token retrieved from data source. The reason is that passwords are often hashed to other form
	 * within the database to prevent leakage
	 */
	var hasher : Option[Class[_ <: PasswordHasher[_, _]]] = None
	
}