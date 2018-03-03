package hnct.lib.access.playframework

import hnct.lib.access.api.{AccessProcessor, AccessRequest, AccessRequestBuilder}
import hnct.lib.access.core.basic.{BasicAccessProcessor, BasicAccessRequest}
import hnct.lib.access.core.session.{SessionAccessProcessor, SessionAccessRequest}
import hnct.lib.utility.Logable
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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
	def buildFromCookie(req: Request[_], processor: AccessProcessor, config : AuthenticationConfig): Future[AccessRequest]

	def buildFromForm(req: Request[_], processor: AccessProcessor, config : AuthenticationConfig): Future[AccessRequest]
	
	def buildFromJson(req: Request[_], processor: AccessProcessor, config : AuthenticationConfig): Future[AccessRequest]
}

class BasicAccessRequestBuilder extends ConcreteRequestBuilder with Logable {

	case class BARForm(username: Option[String], token: Option[String])

	override def buildFromCookie(req: Request[_], processor: AccessProcessor, config : AuthenticationConfig): Future[AccessRequest] = {

		val uname = req.session.get(Const.COOKIE_USERNAME_FIELD)
		val token = req.session.get(Const.COOKIE_TOKEN_FIELD)

		if (uname.isEmpty || token.isEmpty) Future.failed(new RuntimeException(
			"""Unable to find username or access token while
		  building access request from cookie"""))
		else
			processor match {
				case _: BasicAccessProcessor => Future.successful(new BasicAccessRequest(uname, token))
				case _ => Future.failed(new RuntimeException(s"Unsupported processor type $classOf[processor]"))
			}
	}

	override def buildFromForm(req: Request[_], processor: AccessProcessor, config : AuthenticationConfig): Future[AccessRequest] = {

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
	
	override def buildFromJson(req: Request[_], processor: AccessProcessor, config : AuthenticationConfig): Future[AccessRequest] = {
		
		req.asInstanceOf[Request[AnyContent]].body.asJson map { js =>
			((__ \ "username").readNullable[String] ~ (__ \ "token").readNullable[String])(BARForm.apply _).reads(js) match {
				case x : JsSuccess[BARForm] => Future.successful(new BasicAccessRequest(x.get.username, x.get.token))
				case _ => Future.failed(new RuntimeException("Malformed json."))
			}
			
		} getOrElse Future.failed(new RuntimeException("Unable to get request. No json found."))
	
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
	override def buildFromCookie(req: Request[_], processor: AccessProcessor, config: AuthenticationConfig): Future[AccessRequest] = {

		val uname = req.session.get(Const.COOKIE_USERNAME_FIELD)
		val token = req.session.get(Const.COOKIE_TOKEN_FIELD)
		val sid = req.session.get(Const.COOKIE_SESSION_ID_FIELD)

		processor match {
			case p: SessionAccessProcessor => {
				
				if (sid.isEmpty) {
					if (!config.initializeSessionId) Future.failed(new RuntimeException(
						"""Unable to find the session id while building access request from cookie"""))
					else Future.successful(new SessionAccessRequest(uname, token, Some(p.randomSessionId)))
				} else Future.successful(new SessionAccessRequest(uname, token, sid))
			}
			case _ => Future.failed(new RuntimeException(s"Unsupported processor type $classOf[processor]"))
		}
			
	}
	
	/**
		* There are cases where some user access the page, without logging in and we want to track their session.
		* To do this, we create a session id without they logging in and probably save it into the cookie.
		* Because of this, when user submit login form (or authenticate using form) the access request need to use
		* the session that already created. Hence, if the session id is not available in the login form
		* we will build the request with pre-created session id from cookie. If the cookie itself don't have, we will
		* try to use the access processor to build it, if the access processor is a SessionAccessProcessor
		*/
	private def initializeSid(csid : Option[String],config :  AuthenticationConfig, req : Request[_], processor : AccessProcessor) : Option[String] = {
		csid orElse {
			/**
				* When we login, and we allow re-using the previous login session id then
				* we have to extract the session id from Cookie
				*/
			if (!config.isLogin || config.reuseOldSessionOnLogin)
				req.session.get(Const.COOKIE_SESSION_ID_FIELD)
			else None
		} orElse {
			if (config.initializeSessionId)
				processor match {
					case x : SessionAccessProcessor => Some(x.randomSessionId)
					case _ => None
				}
			else None
		}
	}

	override def buildFromForm(req: Request[_], processor: AccessProcessor, config: AuthenticationConfig): Future[AccessRequest] = {

		val reqForm = Form(mapping(
			"username" -> optional(text),
			"token" -> optional(text),
			"sid" -> optional(text)
		)(SARForm.apply)(SARForm.unapply)).bindFromRequest()(req)

		if (reqForm.hasErrors) Future.failed(new RuntimeException("Unable to find access request from submitted form"))
		else {
			val fv = reqForm.get
			val sid = initializeSid(fv.sid, config, req, processor)
			
			if (sid.isEmpty) Future.failed(throw new RuntimeException("Unable to find session id from the submitted request"))
			else Future.successful(new SessionAccessRequest(fv.username, fv.token, sid))
		}
	}
	
	override def buildFromJson(req: Request[_], processor: AccessProcessor, config : AuthenticationConfig): Future[AccessRequest] = {
		
		req.asInstanceOf[Request[AnyContent]].body.asJson map { js =>
			(
				(__ \ "username").readNullable[String] ~
				(__ \ "token").readNullable[String] ~
				(__ \ "sid").readNullable[String]
			)(SARForm.apply _).reads(js) match {
				case x : JsSuccess[SARForm] => {
					val sid = initializeSid(x.get.sid, config, req, processor)
					
					if (sid.isEmpty) Future.failed(throw new RuntimeException("Unable to find session id from the submitted request"))
					else Future.successful(new SessionAccessRequest(x.get.username, x.get.token, sid))
				}
				case _ => Future.failed(new RuntimeException("Malformed json."))
			}
			
		} getOrElse Future.failed(new RuntimeException("Unable to get request. No json found."))
		
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
)(implicit override val executionContext : ExecutionContext) extends AccessRequestBuilder[Request[_]] with ActionFunction[Request, PlayHTTPRequest] {
	
	override def invokeBlock[A](request : Request[A], block : (PlayHTTPRequest[A] => Future[Result])) = {
		refine(request).flatMap { refined =>	// refine the request
			refined.fold(Future.successful(_),	// if the refining process return a result, use it
			{ implicit req =>							// if not, we invoke the block

				req.accessRequest map { actualReq =>
					actualReq match {
						// if the access request is not null, and is a session access request, and it has a non-empty session id registered
						// write the session id to the cookie of the returning result
						case x : SessionAccessRequest if (x != null && !x.sessionId.isEmpty) => block(req) map { result =>
							val currentSessionId = req.session.get(Const.COOKIE_SESSION_ID_FIELD)
							if (currentSessionId.isEmpty || currentSessionId.get != x.sessionId.get)
								result.addingToSession(Const.COOKIE_SESSION_ID_FIELD -> x.sessionId.get)
							else result
						}
						case _ => block(req)
					}
				} getOrElse(block(req))
			})
		}
	}

	def refine[A](request: Request[A]): Future[Either[Result, PlayHTTPRequest[A]]] = {
		
		val f : Future[Either[Result, PlayHTTPRequest[A]]] = build(request, processor) map { ar =>
			Right(new PlayHTTPRequest(request, ar))
		}
		
		f.recoverWith {
			case e : Throwable => {
				
				if (config.requestBuildFailedHandler != null)
					config.requestBuildFailedHandler(e).map(Left(_))
				else if (config.continueOnRequestBuildFailed) {
					val newReq = new PlayHTTPRequest(request)
					Future.successful(Right(newReq))
				}
				else throw e
			}
		}
	}

}

class PlayAuthARBuilder
(
	config: AuthenticationConfig,
	processor: AccessProcessor, cb: ConcreteRequestBuilder
)(implicit ec : ExecutionContext) extends PlayARBuilder(config, processor, cb) {

	def build(request: Request[_], processor: AccessProcessor) = {
		config.credentialSource match {
			case CredentialSource.COOKIE => cb.buildFromCookie(request, processor, config);
			case CredentialSource.FORM => cb.buildFromForm(request, processor, config);
			case CredentialSource.JSON => cb.buildFromJson(request, processor, config)
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
)(implicit ec : ExecutionContext) extends PlayARBuilder(config, processor, cb) {

	def build(request: Request[_], processor: AccessProcessor) = {
		config.credentialSource match {
			case CredentialSource.COOKIE => Future.failed(new RuntimeException(s"When login, cookie can't be used as credential source."))
			case CredentialSource.FORM => cb.buildFromForm(request, processor, config)
			case CredentialSource.JSON => cb.buildFromJson(request, processor, config)
			case _ => Future.failed(new RuntimeException(s"Unknown credential source is found!"))
		}
	}

}