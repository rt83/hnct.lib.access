package hnct.lib.access.api

import hnct.lib.session.api.SessionAccessor
import hnct.lib.access.api.results.LogoutResult
import hnct.lib.access.api.results.LoginResult
import hnct.lib.access.api.results.ActionResult

/**
 * AccessManager provide an API to process a certain type of access request
 * and a certain type of user data. AccessRequest to be passed into the AccessProcessor
 * is built by the caller of the AccessProcessor
 */
trait AccessProcessor[CT <: AccessProcessorConfig, UT <: User, ART <: AccessRequest] {

	/**
	 * The type of configuration of this access processor
	 */
	type ConfigType = CT
	
	/**
	 * The data adapter used to retrieve data
	 */
	var dataAdapter : DataAdapter = _
	
	private[this] var _config : CT = _
	
	var hasher : PasswordHasher[ART, UT] = _
	
	/**
	 * Check if an access request is authenticated
	 * An access request is authenticated if it is logged in before using 
	 * the login method of the access processor
	 */
	def authenticate(req : ART) : ActionResult[ART]
	
	/**
	 * Perform a login given an AccessRequest. 
	 * @return the login result, whether the login is successful or not
	 */
	def login(req : ART) : LoginResult[ART, UT]
	
	/**
	 * When the access processor perform a Login it might have set a timeout
	 * as of when a successful login expires. If the user wants to renew its login
	 * call this method of the access processor
	 */
	def renewLogin(req : ART) : ActionResult[ART]
	
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
	def logout(req : ART) : LogoutResult[ART]

	/**
	 * Configure this access processor with a configuration object
	 * All classes implementing this method should call the super.configure method
	 */
	def configure(config : ConfigType) = {
		_config = config
		
		// use the data adapter class to initialize
		// a data adapter
		dataAdapter = config.dataAdapterClass.newInstance()
		
		hasher = config.hasher.map { hasherClass =>
			hasherClass.asInstanceOf[Class[PasswordHasher[ART, UT]]].newInstance()
		} getOrElse {
			// the default hasher when there is no hasher class defined returns an unchanged token
			new PasswordHasher[ART, UT] {
				
				def hash(request : ART, user : UT) = request.token
				
			}
		}
	}
	
	/**
	 * Retrieving the configuration
	 */
	def config : CT = _config
	
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
	 * 
	 * Since the access processor can be configured to use Session
	 * this method returns an option instead of returning the instance of SessionAccessor 
	 * directly
	 */
	def loginSessionAccessor(req : ART) : Option[SessionAccessor]
	
	/* 
	 * TODO: API for access right checking
	 * Access right checking allow caller to check whether an access request
	 * have the access to certain resources
	 */
}