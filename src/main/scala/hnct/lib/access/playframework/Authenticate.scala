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
import hnct.lib.access.core.session.SessionAccessRequest
import hnct.lib.utility.Logable
import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._
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
	val COOKIE, FORM = Value
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
class Authenticate(ap : AccessProcessor, config : AuthenticationConfig)
	extends ActionFilter[PlayHTTPRequest] {

	def filter[A](request: PlayHTTPRequest[A]): Future[Option[Result]] = {

		ap.authenticate(request.accessRequest) flatMap { authResult =>

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

class DoLogin(ap : AccessProcessor, config : AuthenticationConfig)
	extends ActionFunction[PlayHTTPRequest, PlayHTTPRequest] with Logable {

	def filter[A](request: PlayHTTPRequest[A]): Future[Option[Result]] = {
		ap.login(request.accessRequest) flatMap { lr =>

			request.loginResult = Some(lr)  // store the login result

			if (lr.status != LoginResultCode.SUCCESSFUL) {

				if (config.failedLoginHandler == null)
					Future.successful(Some(Results.BadRequest("Login Failed!")))
				else config.failedLoginHandler(request) map { Some(_) }

			} else {
        Future.successful(None)
      }
		}
	}

  override def invokeBlock[A](request: PlayHTTPRequest[A], block: PlayHTTPRequest[A] => Future[Result]) : Future[Result]= {
    filter(request) flatMap (filterResult => {
      if (filterResult.isDefined) Future.successful(filterResult.get)
      else {
        block(request) map { blockResult =>

          // add login result information into the cookie if needed
          request.loginResult.map({ lr =>

            if (lr.status == LoginResultCode.SUCCESSFUL && config.credentialSource == CredentialSource.COOKIE) {  // login successfully
              var result = blockResult.withSession(
								request.session + (Const.COOKIE_USERNAME_FIELD -> request.accessRequest.username)
																+ (Const.COOKIE_TOKEN_FIELD -> lr.token.get)
							)  // when login successfully, we can assume we have the token already)

							if (request.accessRequest.isInstanceOf[SessionAccessRequest])
								result = result.withSession(
									request.session + (Const.COOKIE_SESSION_ID_FIELD -> request.accessRequest.asInstanceOf[SessionAccessRequest].sessionId)
								)

							result
            } else blockResult
          }) get

        }
      }
    })
  }

}

/**
	* This is the concrete builder that build from request and processor to access request.
	*
	* This trait define the shape of the builder. Implementations will be provided through dependency
	* injection. The two helper PlayAuthARBuilder & PlayLoginARBuilder will obtain instance of the
	* concrete builder through map binding. Default implementation for BasicAccessProcessor and SessionAccessProcessor
	* are provided below, and are binded in the DefaultRequestBuilders module
	*
	*/
trait ConcreteRequestBuilder {
	def buildFromCookie(req : Request[_], processor : AccessProcessor) : Future[AccessRequest]

	def buildFromForm(req : Request[_], processor : AccessProcessor) : Future[AccessRequest]
}

/**
	* Provide the building implementation for a class of user type, to be used with BasicAccessRequest
	*/
class BasicAccessRequestBuilder extends ConcreteRequestBuilder with Logable {

	case class BARForm(username : String, token : String)

	override def buildFromCookie(req: Request[_], processor: AccessProcessor): Future[AccessRequest] = {

    log.debug("Building access request from COOKIE")

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

	override def buildFromForm(req: Request[_], processor: AccessProcessor): Future[AccessRequest] = {

    log.debug("Building access request from FORM")

		val reqForm = Form(mapping(
			"username" -> text.verifying(nonEmpty),
			"token" -> text.verifying(nonEmpty)
		)(BARForm.apply)(BARForm.unapply)).bindFromRequest()(req)

		if (reqForm.hasErrors) Future.failed(new RuntimeException("Unable to find access request from submitted form"))
		else {
			val fv = reqForm.get
			Future.successful(new BasicAccessRequest(fv.username, fv.token))
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
abstract class PlayARBuilder (
		config : AuthenticationConfig, 
		processor : AccessProcessor, cb : ConcreteRequestBuilder)
		
	extends AccessRequestBuilder[Request[_]] with ActionRefiner[Request, PlayHTTPRequest] {

	def refine[A](request: Request[A]): Future[Either[Result, PlayHTTPRequest[A]]] = {
		build(request, processor) map { ar =>
      Right(new PlayHTTPRequest(request, ar))
    } fallbackTo(Future.successful(Left(Results.BadRequest("Cannot find access request!"))))
	}

}

class PlayAuthARBuilder (
    config : AuthenticationConfig,
    processor : AccessProcessor, cb : ConcreteRequestBuilder) extends PlayARBuilder(config, processor, cb) {

  def build(request : Request[_], processor : AccessProcessor) = {
    config.credentialSource match {
      case CredentialSource.COOKIE => cb.buildFromCookie(request, processor);
      case CredentialSource.FORM => cb.buildFromForm(request, processor);
      case _ => Future.failed(new RuntimeException(s"Unknown credential source is found!"));
    }
  }

}

/**
  * For login request, we always look into the form to get the login information. The credential source passed from the configuration is ignored.
  * @param config
  * @param processor
  * @param cb
  */
class PlayLoginARBuilder (
    config : AuthenticationConfig,
    processor : AccessProcessor, cb : ConcreteRequestBuilder) extends PlayARBuilder(config, processor, cb) {

  def build(request : Request[_], processor : AccessProcessor) = cb.buildFromForm(request, processor)

}

class PlayAuth {

	@Inject var reqBuilder : java.util.Map[Class[_ <: AccessRequest], ConcreteRequestBuilder] = _

	/**
	 * Create an action that check if the user is logged in
	 * before invoking the block.
	 * 
	 * The access processor and configuration can either be supplied explicitly or provided implicitly from
	 * the user of this class. See example for more details
	 */
	def apply()(
		implicit ap : AccessProcessor,
							config : AuthenticationConfig): ActionFunction[Request, PlayHTTPRequest] = {

		val cb = reqBuilder.get(ap.ART)

		new PlayAuthARBuilder(config, ap, cb) andThen new Authenticate(ap, config)

	}

	/**
	 * In many cases, access processor are created and configured through the access processor container (so that
	 * no manual binding is required), the access processor can be retrieved through its name. This method
	 * create the play auth action using the access processor retrieved using the specified name.
	 */
	def apply(apName : String)(implicit apc : AccessProcessorContainer, conf : AuthenticationConfig): ActionFunction[Request, PlayHTTPRequest] = {
		
		implicit val ap = apc.get(apName).getOrElse(throw new RuntimeException("Unable to find the access processor "+apName))
		
		apply()

	}
	
}

class PlayLogin {

	@Inject var reqBuilder : java.util.Map[Class[_ <: AccessRequest], ConcreteRequestBuilder] = _

  /**
    * Create an action that check if the user is logged in
    * before invoking the block.
    *
    * The access processor and configuration can either be supplied explicitly or provided implicitly from
    * the user of this class. See example for more details
    */
  def apply()(
    implicit ap : AccessProcessor,
    config : AuthenticationConfig): ActionFunction[Request, PlayHTTPRequest] = {

		val cb = reqBuilder.get(ap.ART)

    new PlayLoginARBuilder(config, ap, cb) andThen new DoLogin(ap, config)

  }

  /**
    * In many cases, access processor are created and configured through the access processor container (so that
    * no manual binding is required), the access processor can be retrieved through its name. This method
    * create the play auth action using the access processor retrieved using the specified name.
    */
  def apply(apName : String)(implicit apc : AccessProcessorContainer, conf : AuthenticationConfig): ActionFunction[Request, PlayHTTPRequest] = {

    implicit val ap = apc.get(apName).getOrElse(throw new RuntimeException("Unable to find the access processor "+apName))

    apply()

  }

}