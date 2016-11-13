package hnct.lib.access.playframework

import hnct.lib.access.api.{AccessProcessor, AccessRequest, AccessRequestBuilder}
import hnct.lib.access.core.basic.{BasicAccessProcessor, BasicAccessRequest}
import hnct.lib.access.core.session.{SessionAccessProcessor, SessionAccessRequest}
import hnct.lib.utility.Logable
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{ActionRefiner, Request, Result, Results}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * This is the concrete builder that have the actual logic to build from request and processor to access request.
  *
  * This trait define the shape of the builder. Implementations will be provided through dependency
  * injection. The two helper PlayAuthARBuilder & PlayLoginARBuilder are the two builders that actually implemented
  * the access api's AccessRequestBuilder trait and will obtain instance of the
  * concrete builder through map binding. Default implementation for BasicAccessProcessor and SessionAccessProcessor
  * are provided below, and are binded in the DefaultRequestBuilders module
  *
  */
trait ConcreteRequestBuilder {
	def buildFromCookie(req: Request[_], processor: AccessProcessor): Future[AccessRequest]

	def buildFromForm(req: Request[_], processor: AccessProcessor): Future[AccessRequest]
}

class BasicAccessRequestBuilder extends ConcreteRequestBuilder with Logable {

	case class BARForm(username: Option[String], token: Option[String])

	override def buildFromCookie(req: Request[_], processor: AccessProcessor): Future[AccessRequest] = {

		log.debug("Building access request from COOKIE")

		val uname = req.session.get(Const.COOKIE_USERNAME_FIELD)
		val token = req.session.get(Const.COOKIE_TOKEN_FIELD);

		if (uname.isEmpty || token.isEmpty) Future.failed(new RuntimeException(
			"""Unable to find username or access token while
		  building access request from cookie"""))
		else
			processor match {
				case _: BasicAccessProcessor => Future.successful(new BasicAccessRequest(uname, token))
				case _ => Future.failed(new RuntimeException(s"Unsupported processor type $classOf[processor]"))
			}
	}

	override def buildFromForm(req: Request[_], processor: AccessProcessor): Future[AccessRequest] = {

		log.debug("Building access request from FORM")

		val reqForm = Form(mapping(
			"username" -> optional(text),
			"token" -> optional(text)
		)(BARForm.apply)(BARForm.unapply)).bindFromRequest()(req)

		if (reqForm.hasErrors) Future.failed(new RuntimeException("Unable to find access request from submitted form"))
		else {
			val fv = reqForm.get
			Future.successful(new BasicAccessRequest(fv.username, fv.token))
		}
	}
}

class SessionAccessRequestBuilder extends ConcreteRequestBuilder with Logable {

	case class SARForm(username: Option[String], token: Option[String], sid : Option[String])
	
	/**
	  * Build access request from Cookie. This will fail
	  * @param req
	  * @param processor
	  * @return
	  */
	override def buildFromCookie(req: Request[_], processor: AccessProcessor): Future[AccessRequest] = {

		log.debug("Building access request from COOKIE")

		val uname = req.session.get(Const.COOKIE_USERNAME_FIELD)
		val token = req.session.get(Const.COOKIE_TOKEN_FIELD)
		val sid = req.session.get(Const.COOKIE_SESSION_ID_FIELD)

		if (sid.isEmpty) Future.failed(new RuntimeException(
			"""Unable to find the session id while building access request from cookie"""))
		else
			processor match {
				case _: SessionAccessProcessor => Future.successful(new SessionAccessRequest(uname, token, sid))
				case _ => Future.failed(new RuntimeException(s"Unsupported processor type $classOf[processor]"))
			}
	}

	override def buildFromForm(req: Request[_], processor: AccessProcessor): Future[AccessRequest] = {

		log.debug("Building access request from FORM")

		val reqForm = Form(mapping(
			"username" -> optional(text),
			"token" -> optional(text),
			"sid" -> optional(text)
		)(SARForm.apply)(SARForm.unapply)).bindFromRequest()(req)
		
		/**
		  * There are cases where some user access the page, without logging in and we want to track their session.
		  * To do this, we create a session id without they logging in and probably save it into the cookie.
		  * Because of this, when user submit login form (or authenticate using form) the access request need to use
		  * the session that already created. Hence, if the session id is not available in the login form
		  * we will build the request with pre-created session id from cookie.
		  */
		val sid = req.session.get(Const.COOKIE_SESSION_ID_FIELD)

		if (reqForm.hasErrors) Future.failed(new RuntimeException("Unable to find access request from submitted form"))
		else {
			val fv = reqForm.get
			Future.successful(new SessionAccessRequest(fv.username, fv.token, fv.sid orElse sid))
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
abstract class PlayARBuilder
(
	config: AuthenticationConfig,
	processor: AccessProcessor, cb: ConcreteRequestBuilder
) extends AccessRequestBuilder[Request[_]] with ActionRefiner[Request, PlayHTTPRequest] {

	def refine[A](request: Request[A]): Future[Either[Result, PlayHTTPRequest[A]]] = {
		build(request, processor) map { ar =>
			Right(new PlayHTTPRequest(request, ar))
		} fallbackTo (Future.successful(Left(Results.BadRequest("Cannot find access request!"))))
	}

}

class PlayAuthARBuilder
(
	config: AuthenticationConfig,
	processor: AccessProcessor, cb: ConcreteRequestBuilder
) extends PlayARBuilder(config, processor, cb) {

	def build(request: Request[_], processor: AccessProcessor) = {
		config.credentialSource match {
			case CredentialSource.COOKIE => cb.buildFromCookie(request, processor);
			case CredentialSource.FORM => cb.buildFromForm(request, processor);
			case _ => Future.failed(new RuntimeException(s"Unknown credential source is found!"));
		}
	}

}

/**
  * For login request, we always look into the form to get the login information. The credential source passed from the configuration is ignored.
  * Note in future, we can extend this to build from other format such as json.
  *
  * @param config
  * @param processor
  * @param cb
  */
class PlayLoginARBuilder
(
	config: AuthenticationConfig,
	processor: AccessProcessor, cb: ConcreteRequestBuilder
) extends PlayARBuilder(config, processor, cb) {

	def build(request: Request[_], processor: AccessProcessor) = cb.buildFromForm(request, processor)

}