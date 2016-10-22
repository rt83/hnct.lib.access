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
import hnct.lib.access.core.basic.BasicAccessProcessor
import scala.concurrent.ExecutionContext.Implicits.global
import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.AccessProcessorContainer
import com.google.inject.Inject
import play.data.FormFactory

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
	var credentialSource = CredentialSource.COOKIE
	
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

object CredentialSource extends Enumeration {
	val COOKIE, FORM = CredentialSource
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
class Authenticate[UT <: User, ART <: AccessRequest](ap : AccessProcessor[UT, ART], config : AuthenticationConfig) 
	extends ActionFilter[PlayAccessRequest] {

	def filter[A](request: PlayAccessRequest[A]): Future[Option[Result]] = {
		
		ap.authenticate(request.accessRequest.asInstanceOf[ART]) map { authResult =>
		
			if (authResult.status != ActionResultCode.SUCCESSFUL) {
				
				if (config.rememberLastUrl) {	// if remembering last URL is set, we will record the current URL before returning the result
					// TODO: remembering the last visited URL to cookie / session
				}
				
				if (config.failHandler == null)
					Some(Results.Redirect(config.redirectionPath))	// redirect to other page when failed
				else 
					Some(config.failHandler(request))
				
			} else None	// successfully authenticate, return None so that we can continue
		
		}

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
class PlayARBuilder[UT <: User] (
		config : AuthenticationConfig, 
		processor : AccessProcessor[UT, BasicAccessRequest], 
		formFactory : FormFactory)
		
	extends AccessRequestBuilder[UT, Request[_], BasicAccessRequest] with ActionTransformer[Request, PlayAccessRequest] {
	
	def build(request : Request[_], processor : AccessProcessor[UT, BasicAccessRequest]) = {
		
		config.credentialSource match {
			case CredentialSource.COOKIE => buildFromCookie(request, processor);
			case _ => Future.failed(new RuntimeException(s"Unknow credential source is found!"));
		}

	}
	
	private def buildFromCookie(req : Request[_], processor : AccessProcessor[UT, BasicAccessRequest]) : Future[BasicAccessRequest] = {
		
		val uname = req.session.get(Const.COOKIE_USERNAME_FIELD)
		val token = req.session.get(Const.COOKIE_TOKEN_FIELD);
		
		if (uname.isEmpty || token.isEmpty) Future.failed(new RuntimeException("""Unable to find username or access token while 
		  building access request from cookie"""))
		else
			processor match {
				case _ : BasicAccessProcessor => Future.successful(new BasicAccessRequest(uname.get, token.get))
				case _ => Future.failed(new RuntimeException(s"Unsupported processor type $classOf[processor]"))
			}
		
	}
	
	/*private def buildFromForm(req : Request[_], processor : AccessProcessor[UT, BasicAccessRequest]) : Future[BasicAccessRequest] = {
		
	}*/

	def transform[A](request: Request[A]): Future[PlayAccessRequest[A]] = {
		build(request, processor) map { new PlayAccessRequest(request, _) }
	}
	
}

class PlayAuth {
	
	@Inject var formFactory : FormFactory = _
	
	/**
	 * Create an action that check if the user is logged in
	 * before invoking the block. Use the default AuthenticationConfig
	 * 
	 * The access processor can either be supplied explicitly or provided implicitly from 
	 * the user of this class. See example for more details
	 */
	def apply[UT <: User](implicit ap : AccessProcessor[UT, BasicAccessRequest]) = {
		
		val conf = new AuthenticationConfig
		
		(new PlayARBuilder(conf, ap, formFactory)) andThen (new Authenticate(ap, conf))
		
	}
	
	/**
	 * Create an action that check if the user is logged in
	 * with a particular config.
	 */
	def apply[UT <: User](implicit ap : AccessProcessor[UT, BasicAccessRequest], config : AuthenticationConfig) = {
		new PlayARBuilder(config, ap, formFactory) andThen new Authenticate(ap, config)
	}
	
	/**
	 * In many cases, access processor are created and configured through the access processor container (so that
	 * no manual binding is required), the access processor can be retrieved through its name. This method
	 * create the play auth action using the access processor retrieved using the specified name.
	 */
	def apply[UT <: User](apName : String)(implicit apc : AccessProcessorContainer) = {
		
		val ap = apc.get[UT, BasicAccessRequest](apName).getOrElse(throw new RuntimeException("Unable to find the access processor "+apName))
		val conf = new AuthenticationConfig
		
		new PlayARBuilder(conf, ap, formFactory) andThen new Authenticate(ap, conf)
		
	}
	
	/**
	 * In many cases, access processor are created and configured through the access processor container (so that
	 * no manual binding is required), the access processor can be retrieved through its name. This method
	 * create the play auth action using the access processor retrieved using the specified name.
	 */
	def apply[UT <: User](apName : String)(implicit apc : AccessProcessorContainer, conf : AuthenticationConfig) = {
		
		val ap = apc.get[UT, BasicAccessRequest](apName).getOrElse(throw new RuntimeException("Unable to find the access processor "+apName))
		
		new PlayARBuilder(conf, ap, formFactory) andThen new Authenticate(ap, conf)
		
	}
	
}