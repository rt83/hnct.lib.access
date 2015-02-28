package hnct.lib.access.api

/**
 * AccessManager provide an API to process a certain type of access request
 * and a certain type of user data. AccessRequest to be passed into the AccessProcessor
 * is built by the caller of the AccessProcessor
 */
trait AccessProcessor[T <: AccessRequest, U <: User] {
	
	/**
	 * Check if an access request is authenticated
	 * An access request is authenticated if it is logged in before using 
	 * the login method of the access processor
	 */
	def authenticate(req : T) : Boolean
	
	/**
	 * Perform a login given an AccessRequest. 
	 * @return the login result, whether the login is successful or not
	 */
	def login(req : T) : LoginResult[T, U]
	
	/**
	 * When the access processor perform a Login it might have set a timeout
	 * as of when a successful login expires. If the user wants to renew its login
	 * call this method of the access processor
	 */
	def renewLogin(req : T) : Unit
	
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
	def logout(req : T) : LogoutResult[T]

	/**
	 * Configure this access processor with a configuration object
	 * TODO: specify generic to avoid using AnyRef
	 */
	def configure(config : AnyRef)
	
	/**
	 * Get the user data adapter
	 */
	def userDataAdapter[D <: UserDataAdapter[T, U]] : D
	
	/**
	 * Set the user data adapter
	 */
	def userDataAdapter_=[D <: UserDataAdapter[T, U]](adapter : D) : Unit
	
	/*
	 * TODO: API for getting the login session
	 * Login session allows caller to set and get data specific to a user session
	 */
	
	/* 
	 * TODO: API for access right checking
	 * Access right checking allow caller to check whether an access request
	 * have the access to certain resources
	 */
}