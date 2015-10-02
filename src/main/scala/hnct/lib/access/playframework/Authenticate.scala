package hnct.lib.access.playframework

import play.api.mvc._
import scala.concurrent.Future
import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.AccessProcessorFactory
import hnct.lib.access.api.results.LoginResult
import hnct.lib.access.api.results.ActionResultCode
import hnct.lib.access.api.AccessRequest
import hnct.lib.access.api.User
import hnct.lib.access.api.AccessRequestBuilder
import hnct.lib.access.core.basic.BasicAccessRequest

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
	
	/**
	 * The path which we will redirect to if authentication is not 
	 * successful. This path will be used if no failed handler is specified. The default is 
	 * the home page.
	 */
	var redirectionPath = "/";
	
	/**
	 * The handler which will be called when authentication failed
	 * 
	 * This handler should provide the final result to be returned to the client
	 */
	var failHandler : (Request[_]) => Result = _
}

/**
 * The authenticate filter accepts a configuration, saying how it use a access processor
 * 
 * It also needs a access factory to produce the needed access processor
 * 
 * Since the authenticate action filter is called and initialized inside a play "container"
 * we cannot explicitly supply the needed parameters to it and have to use dependency injection
 * support from play.
 */
class Authenticate[UT <: User, ART <: AccessRequest](accessFactory : AccessProcessorFactory, config : AuthenticationConfig) extends ActionFilter[PlayAccessRequest] {

	def filter[A](request: PlayAccessRequest[A]): Future[Option[Result]] = {
		
		val ap = accessFactory
					.get(config.apUnit)
					.getOrElse(throw new AccessProcessorNotFound("Couldn't find the needed access processor", config.apUnit))	// obtain the access processor
					.asInstanceOf[AccessProcessor[_, _, ART]]
		
		val authenticationResult = ap.authenticate(request.accessRequest.asInstanceOf[ART])
		
		if (authenticationResult.status != ActionResultCode.SUCCESSFUL) {
			
			if (config.rememberLastUrl) {	// if remembering last URL is set, we will record the current URL before returning the result
				// TODO: remembering the last visited URL to cookie / session
			}
			
			if (config.failHandler == null)
				Future.successful(Some(Results.Redirect(config.redirectionPath)))	// redirect to other page when failed
			else 
				Future.successful(Some(config.failHandler(request)))
			
		} else Future.successful(None)	// successfully authenticate, return None so that we can continue

	}
  
}

/**
 * Default implementation of a request builder used for the default provided implementations
 * of access processors. The builder will build suitable access requests used by the other action / action filters
 * such as the Authenticate action filter or the DoLogin action
 * 
 * The access request is wrapped in the WrappedAccessRequest and passed to the next action filter / action
 * 
 * Support:
 * - BasicAccessRequest
 * - SessionAccessRequest
 */
class BasicARBuilder(config : AuthenticationConfig, apFactory : AccessProcessorFactory) 
extends AccessRequestBuilder[Request[_], AccessRequest] with ActionTransformer[Request, PlayAccessRequest] {
	
	def build(request : Request[_]) = {
		if (config.credentialSource.equals("COOKIE")) {	// build the credentials from COOKIE
			
			// TODO: to extract information from the original request and build the access request
			new BasicAccessRequest("COOKIE_USER", "COOKIE_PASSWORD")	// TEST CODE, TO BE REMOVED
			
		} else {	// build credentials from FORM
			
			// TODO: to extract information from the original request and build the access request
			new BasicAccessRequest("FORM_USER", "FORM_PASSWORD")		// TEST CODE, TO BE REMOVED
			
		}
	}

	def transform[A](request: Request[A]): Future[PlayAccessRequest[A]] = {
		val ar = build(request)
		
		Future.successful(new PlayAccessRequest(request, ar))
	}
	
}

object Authenticate {
	
	/**
	 * The default AccessProcessorFactory, which reads configuration from default files
	 * For testing purpose, the Authenticate action can be injected using a different access
	 * processor factory
	 */
	private val factory : AccessProcessorFactory = AccessProcessorFactory()
	
	/**
	 * Create an action that check if the user is logged in
	 * before invoking the block. Use the default AuthenticationConfig
	 */
	def apply() = new Authenticate(factory, new AuthenticationConfig)
	
	/**
	 * Create an action that check if the user is logged in
	 * with a particular config, before invoking the block.should
	 */
	def apply(config : AuthenticationConfig) = new Authenticate(factory, config)
	
}