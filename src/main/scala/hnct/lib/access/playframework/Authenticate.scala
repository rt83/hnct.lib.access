package hnct.lib.access.playframework

import play.api.mvc._

import scala.concurrent.Future
import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.AccessProcessorFactory
import hnct.lib.access.api.results.{ActionResultCode, LoginResult, LoginResultCode}
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
	var failedAuthHandler : (PlayHTTPRequest[_]) => Future[Result] = _

	var failedLoginHandler : (PlayHTTPRequest[_]) => Future[Result] = _


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
class Authenticate[UT <: User, ART <: BasicAccessRequest](ap : AccessProcessor[UT, ART], config : AuthenticationConfig)
	extends ActionFilter[PlayHTTPRequest] {

	def filter[A](request: PlayHTTPRequest[A]): Future[Option[Result]] = {

		ap.authenticate(request.accessRequest.asInstanceOf[ART]) flatMap { authResult =>

			request.authResult = Some(authResult)

			if (authResult.status != ActionResultCode.SUCCESSFUL) {

				if (config.rememberLastUrl) {	// if remembering last URL is set, we will record the current URL before returning the result
					// TODO: remembering the last visited URL to cookie / session
				}

				if (config.failedAuthHandler == null)
					Future.successful(Some(Results.Redirect(config.redirectionPath)))	// redirect to other page when failed
				else
					config.failedAuthHandler(request) map { result => Some(result) }
				
			} else Future.successful(None)	// successfully authenticate, return None so that we can continue
		
		}

	}
  
}

class DoLogin[U <: User, AR <: BasicAccessRequest](ap : AccessProcessor[U, AR], config : AuthenticationConfig)
	extends ActionFilter[PlayHTTPRequest] {

	override protected def filter[A](request: PlayHTTPRequest[A]): Future[Option[Result]] = {
		ap.login(request.accessRequest.asInstanceOf[AR]) flatMap { lr =>

			request.loginResult = Some(lr)

			if (lr.status != LoginResultCode.SUCCESSFUL) {

				if (config.failedLoginHandler == null)
					Future.successful(Some(Results.BadRequest("Login Failed!")))
				else config.failedAuthHandler(request) map { Some(_) }

			} else Future.successful(None)
		}
	}

}

/**
	* This is the concrete builder that build from request and processor to access request.
	*
	* This trait define the shape of the builder. We can provide implementation for each type of access request
	* which will be imported and implicitly passed to the PlayARBuilder build method
	*
	* @tparam UT
	* @tparam ART
	*/
trait ConcreteRequestBuilder[UT <: User, ART <: AccessRequest] {
	def buildFromCookie(req : Request[_], processor : AccessProcessor[UT, ART]) : Future[ART]
}

/**
	* Provide the building implementation for a class of user type, to be used with BasicAccessRequest
	*/
class BasicAccessRequestBuilder extends ConcreteRequestBuilder[User, BasicAccessRequest] {

	override def buildFromCookie(req: Request[_], processor: AccessProcessor[User, BasicAccessRequest]): Future[BasicAccessRequest] = {
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
class PlayARBuilder[UT <: User, ART <: BasicAccessRequest] (
		config : AuthenticationConfig, 
		processor : AccessProcessor[UT, ART],
		formFactory : FormFactory)(implicit cb : ConcreteRequestBuilder[UT, ART])
		
	extends AccessRequestBuilder[UT, Request[_], ART] with ActionTransformer[Request, PlayHTTPRequest] {
	
	def build(request : Request[_], processor : AccessProcessor[UT, ART]) = {
		buildInternal(request, processor)
	}

	private def buildInternal(request : Request[_], processor : AccessProcessor[UT, ART])(implicit cb : ConcreteRequestBuilder[UT, ART]) = {
		config.credentialSource match {
			case CredentialSource.COOKIE => cb.buildFromCookie(request, processor);
			case _ => Future.failed(new RuntimeException(s"Unknow credential source is found!"));
		}
	}

	def transform[A](request: Request[A]): Future[PlayHTTPRequest[A]] = {
		build(request, processor) map { new PlayHTTPRequest(request, _) }
	}

}

class PlayAuth {

	@Inject var formFactory : FormFactory = _

	/**
	 * Create an action that check if the user is logged in
	 * before invoking the block.
	 * 
	 * The access processor and configuration can either be supplied explicitly or provided implicitly from
	 * the user of this class. See example for more details
	 */
	def apply[UT <: User, ART <: BasicAccessRequest]()(
		implicit ap : AccessProcessor[UT, ART],
							config : AuthenticationConfig,
							cb : ConcreteRequestBuilder[UT, ART]): ActionFunction[Request, PlayHTTPRequest] = {

		new PlayARBuilder(config, ap, formFactory) andThen new Authenticate(ap, config)

	}

	/**
	 * In many cases, access processor are created and configured through the access processor container (so that
	 * no manual binding is required), the access processor can be retrieved through its name. This method
	 * create the play auth action using the access processor retrieved using the specified name.
	 */
	def apply[UT <: User, ART <: BasicAccessRequest](apName : String)(implicit apc : AccessProcessorContainer, conf : AuthenticationConfig, cb : ConcreteRequestBuilder[UT, ART]): ActionFunction[Request, PlayHTTPRequest] = {
		
		implicit val ap = apc.get[UT, ART](apName).getOrElse(throw new RuntimeException("Unable to find the access processor "+apName))
		
		apply()

	}
	
}

object PlayAuth {
	implicit val basicARTBuilder = new BasicAccessRequestBuilder()
}