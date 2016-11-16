package hnct.lib.access.playframework

import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.results.{ActionResultCode, LoginResultCode}
import hnct.lib.access.core.session.SessionAccessRequest
import hnct.lib.utility.Logable
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

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
	  * What to do when we have authentication failure.
	  * If this is set to false, we will either redirect or invoke the auth failed handler.
	  *
	  * If set to true, the controller method will be invoked
	  */
	var continueOnAuthFailed = false

	/**
	  * The handler which will be called when authentication failed
	  *
	  * This handler should provide the final result to be returned to the client
	  */
	var failedAuthHandler: (PlayHTTPRequest[_]) => Future[Result] = _

	var failedLoginHandler: (PlayHTTPRequest[_]) => Future[Result] = _
	
	/**
	  * Building access request might fail. We might want to send results back to users
	  * instead of just send a fixed fail message
	  */
	var requestBuildFailedHandler: (Throwable => Future[Result]) = _
	
	/**
	  * This is used together with the SessionAccessRequestBuilder
	  * Whether or not to initialize the session id if there is none
	  * If this is set to true, the SessionAccessRequestBuilder will initialize
	  * the session id using the SessionAccessProcessor
	  */
	var initializeSessionId = false
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
class Authenticate(ap: AccessProcessor, config: AuthenticationConfig)
	extends ActionFilter[PlayHTTPRequest] {

	def filter[A](request: PlayHTTPRequest[A]): Future[Option[Result]] = {

		ap.authenticate(request.accessRequest) flatMap { authResult =>

			request.authResult = Some(authResult)

			if (authResult.status != ActionResultCode.SUCCESSFUL) {

				if (config.rememberLastUrl) {
					// if remembering last URL is set, we will record the current URL before returning the result
					// TODO: remembering the last visited URL to cookie / session
				}

				if (config.continueOnAuthFailed)	// if this is true, we don't invoke redirection or fail handler
					Future.successful(None)
				else {
					if (config.failedAuthHandler == null)
						Future.successful(Some(Results.Redirect(config.redirectionPath))) // redirect to other page when failed
					else
						config.failedAuthHandler(request) map { result => Some(result) }
				}
			} else Future.successful(None) // successfully authenticate, return None so that we can continue

		}

	}

}

class DoLogin(ap: AccessProcessor, config: AuthenticationConfig)
	extends ActionFunction[PlayHTTPRequest, PlayHTTPRequest] with Logable {

	def filter[A](request: PlayHTTPRequest[A]): Future[Option[Result]] = {
		ap.login(request.accessRequest) flatMap { lr =>

			request.loginResult = Some(lr) // store the login result

			if (lr.status != LoginResultCode.SUCCESSFUL) {

				if (config.failedLoginHandler == null)
					Future.successful(Some(Results.BadRequest("Login Failed!")))
				else config.failedLoginHandler(request) map {
					Some(_)
				}

			} else {
				Future.successful(None)
			}
		}
	}

	override def invokeBlock[A](request: PlayHTTPRequest[A], block: PlayHTTPRequest[A] => Future[Result]): Future[Result] = {
		filter(request) flatMap (filterResult => {
			if (filterResult.isDefined) Future.successful(filterResult.get)
			else {
				block(request) map { blockResult =>

					// add login result information into the cookie if needed
					request.loginResult.map({ lr =>

						if (lr.status == LoginResultCode.SUCCESSFUL && config.credentialSource == CredentialSource.COOKIE) {
							
							// login successfully
							var result = blockResult.addingToSession(
								(Const.COOKIE_USERNAME_FIELD -> request.accessRequest.username.get),
									(Const.COOKIE_TOKEN_FIELD -> lr.token.get)
							)(request) // when login successfully, we can assume we have the token already)
							if (request.accessRequest.isInstanceOf[SessionAccessRequest])
								result = result.addingToSession(
									(Const.COOKIE_SESSION_ID_FIELD -> request.accessRequest.asInstanceOf[SessionAccessRequest].sessionId.get)
								)(request)

							result
						} else blockResult
					}) get

				}
			}
		})
	}

}
