package hnct.lib.access.api

import hnct.lib.session.api.SessionAccessor

/**
 * AccessManager provide an API to process a certain type of access request
 * and a certain type of user data. AccessRequest to be passed into the AccessProcessor
 * is built by the caller of the AccessProcessor
 */
trait AccessProcessor {

	/**
	 * The type of configuration of this access processor
	 */
	type ConfigType <: AccessProcessorConfig
	/**
	 * The type of user data this access processor need
	 */
	type UserType <: User
	/**
	 * The type of access request this access processor will process
	 */
	type AccessRequestType <: AccessRequest
	
	var _da : DataAdapter = null
	
	protected var _config : ConfigType
	
	/**
	 * Check if an access request is authenticated
	 * An access request is authenticated if it is logged in before using 
	 * the login method of the access processor
	 */
	def authenticate(req : AccessRequestType) : Boolean
	
	/**
	 * Perform a login given an AccessRequest. 
	 * @return the login result, whether the login is successful or not
	 */
	def login(req : AccessRequestType) : LoginResult[AccessRequestType, UserType]
	
	/**
	 * When the access processor perform a Login it might have set a timeout
	 * as of when a successful login expires. If the user wants to renew its login
	 * call this method of the access processor
	 */
	def renewLogin(req : AccessRequestType) : Unit
	
	/**
	 * Get the login timeout of this access processor
	 * If this value is set, when the user login successfully, the login
	 * will expire within this amount of time
	 */
	def loginTimeout : Long
	
	/**
	 * Set the login timeout
	 */
	def loginTimeout_=(timeout : Long) : Unit
	
	/**
	 * Perform the logout
	 */
	def logout(req : AccessRequestType) : LogoutResult[AccessRequestType]

	/**
	 * Configure this access processor with a configuration object
	 * All classes implementing this method should call the super.configure method
	 */
	def configure(config : ConfigType) = {
		_config = config
	}
	
	/**
	 * Retrieving the configuration
	 */
	def config : ConfigType = _config
	
	/**
	 * Get the user data adapter
	 */
	def userDataAdapter : DataAdapter = _da
	
	/**
	 * Set the user data adapter
	 */
	def userDataAdapter_=(adapter : DataAdapter) : Unit = _da = adapter
	
	/**
	 * Get the login session accessor corresponding to an access request
	 * Session accessor is the entry point to access the data stored
	 * inside the session of the input access request
	 * 
	 * Example: when an user is login with BasicAccessProcessor
	 * he will have a login session corresponding to his username, i.e.
	 * the session data store is identified by his username
	 * When he is login using SessionTokenAccessProcessor, his session store
	 * is identified by both username and an session id. This allow multiple
	 * login and data of the same user.
	 */
	def loginSessionAccessor(req : AccessRequestType) : SessionAccessor
	
	/* 
	 * TODO: API for access right checking
	 * Access right checking allow caller to check whether an access request
	 * have the access to certain resources
	 */
}