package hnct.lib.access.playframework

import play.api.mvc._
import scala.concurrent.Future
import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.AccessProcessorFactory

/**
 * Store the configuration for the authentication process we want to
 * perform
 */
class AuthenticationConfig {
	
	/**
	 * The login page which the user will see if he is not logged in
	 */
	var loginPage = ""
	
	/**
	 * The message that returned to the 
	 */
	var unauthorizedMessages = ""
	
	/**
	 * Whether or not we will remember the last URL the user access.
	 */
	var rememberLastUrl = true
	
	/**
	 * Source of data for user identification
	 */
	var credentialSource = "COOKIE"
	
	/**
	 * The access processor unit name
	 */
	var apUnit = ""
}

/**
 * The authenticate filter accepts a 
 * 
 */
class Authenticate(config : AuthenticationConfig) extends ActionFilter[WrappedAccessRequest] {

	def filter[A](request: WrappedAccessRequest[A]): Future[Option[Result]] = {
		
		val ap = AccessProcessorFactory.get(config.apUnit)	// obtain the access processor
		
		ap getOrElse
		
	}
  
}

object Authenticate {
	
	/**
	 * Create an action that check if the user is logged in
	 * before invoking the block. Use the default AuthenticationConfig
	 */
	def apply() = new Authenticate(new AuthenticationConfig)
	
	/**
	 * Create an action that check if the user is logged in
	 * with a particular config, before invoking the block.
	 */
	def apply(config : AuthenticationConfig) = new Authenticate(config)
	
}