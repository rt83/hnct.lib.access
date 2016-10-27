package hnct.lib.access.api

import hnct.lib.session.api.SessionAccessor
import hnct.lib.access.api.results.LogoutResult
import hnct.lib.access.api.results.LoginResult
import hnct.lib.access.api.results.ActionResult
import scala.concurrent.Future

/**
 * The factory that can create access processor. This is provided by the 
 * implementation of each type of access processor.
 * 
 * Each access processor should be implemented within a GUICE module, which
 * provide the actual factory. All factory will be provided in a map binding which
 * will be used by the Container to create the necessary access processor.
 * 
 * Refer to the container to see how the map binding is used.
 */
trait AccessProcessorFactory {
	
	def create(config : AccessProcessorConfig) : AccessProcessor;
	
}

/**
 * AccessManager provide an API to process a certain type of access request
 * and a certain type of user data. AccessRequest to be passed into the AccessProcessor
 * is built by the caller of the AccessProcessor
 */
trait AccessProcessor {

	val ART	: Class[_ <: AccessRequest]// implementation needs to specify the type of the access request and user it is handling
	val UT : Class[_ <: User]

	/**
	 * The data adapter used to retrieve data
	 */
	var dataAdapter : DataAdapter = _
	
	protected[this] val _config : AccessProcessorConfig
	
	var hasher : PasswordHasher = _
	
	/**
	 * Check if an access request is authenticated
	 * An access request is authenticated if it is logged in before using 
	 * the login method of the access processor
	 */
	def authenticate(req : AccessRequest) : Future[ActionResult]
	
	/**
	 * Perform a login given an AccessRequest. 
	 * @return the login result, whether the login is successful or not
	 */
	def login(req : AccessRequest) : Future[LoginResult]
	
	/**
	 * When the access processor perform a Login it might have set a timeout
	 * as of when a successful login expires. If the user wants to renew its login
	 * call this method of the access processor
	 */
	def renewLogin(req : AccessRequest) : Future[ActionResult]
	
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
	def logout(req : AccessRequest) : Future[LogoutResult]
	
	/**
	 * Retrieving the configuration
	 */
	def config : AccessProcessorConfig = _config
	
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
	def loginSessionAccessor(req : AccessRequest) : Option[SessionAccessor]
	
	/* 
	 * TODO: API for access right checking
	 * Access right checking allow caller to check whether an access request
	 * have the access to certain resources
	 */
}