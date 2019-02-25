package hnct.lib.access.playframework

import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.results.{ActionResultCode, LoginResultCode, LogoutResultCode}
import hnct.lib.access.core.session.SessionAccessRequest
import hnct.lib.utility.Logable
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

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
		* Where to write the credential data to after login successfully
		*/
	var credentialTarget = CredentialSource.COOKIE

	/**
	  * Source of data for user identification when build access request
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
		* What to do when we can't build access request from client's provided information
		*
		* This parameter is only important when requestBuildFailedHandler is not specified.
		*
		* Specifically, if this is set to true and no request can be built, the authentication / login
		* process will continue and set an action result in the http request.
		* The Option[AccessRequest] within the http request and within the action
		* result will be NONE. The authentication / login / logout result will have code FAILED_REQUEST_NOT_FOUND. If this is
		* an authentication request, continueOnAuthFailed will be checked and rule for auth failed will apply.
		*
		* If this flag is set to false, and no handler present, exception will be thrown.
		*
		* By default this is set to true, so that we consider the case of not able to build access request as
		* a normal authentication failure.
		*
		*/
	var continueOnRequestBuildFailed = true

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
	
	/**
		* When login, there might be old session being submitted. This parameter
		* controls whether we are going to reuse it or not
		*
		* Applicable to SessionAccessProcessor
		*/
	var reuseOldSessionOnLogin = false
	
	/**
		* Whether or not this configuration is being used for login or authentication
		*
		* Applicable to SessionAccessProcessor (used in SessionAccessRequestBuilder)
		*/
	var isLogin = false

	/**
		* Remember me duration is the session time to live when remember me option is selected
		* when building access request. If this value is not -1 and remember me is selected when
		* login request is submitted then the access request builder should set it as the login
		* timeout. When set, it takes priority over the login timeout set for the access processor which
		* takes priority over the timeout set at the session library level.
		*/
	var rememberMeDuration = 14 * 24 * 3600 * 1000	// default is 14 days
}

object CredentialSource extends Enumeration {
	val COOKIE, FORM, JSON = Value
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
class Authenticate(ap: AccessProcessor, config: AuthenticationConfig)(implicit override val executionContext: ExecutionContext)
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

/**
	*
	* @param ap
	* @param config
	*/
class DoLogout(ap: AccessProcessor, config: AuthenticationConfig)(implicit override val executionContext: ExecutionContext)
	extends ActionFunction[PlayHTTPRequest, PlayHTTPRequest] {
	
	def refine[A](request: PlayHTTPRequest[A]): Future[Either[Result, PlayHTTPRequest[A]]] = {
		
		ap.logout(request.accessRequest) flatMap { logoutResult =>
			
			request.logoutResult = Some(logoutResult)

			if (logoutResult.status != LogoutResultCode.SUCCESSFUL) {
				
				if (logoutResult.status == LogoutResultCode.FAILED_NOT_AUTHENTICATED) {
					if (config.continueOnAuthFailed)  // if this is true, we don't invoke redirection or fail handler
						Future.successful(Right(request))
					else {
						if (config.failedAuthHandler == null)
							Future.successful(Left(Results.Redirect(config.redirectionPath))) // redirect to other page when failed
						else
							config.failedAuthHandler(request) map { result => Left(result) }
					}
				} else Future.successful(Right(request)) // if not successfully logged out, and the reason is not because of authentication... currently we don't handle, the controller will have to handle this.
				
			} else Future.successful(Right(request)) // successfully logged out, return the request itself so that we can continue

		}

	}
	
	override def invokeBlock[A](request: PlayHTTPRequest[A], block: PlayHTTPRequest[A] => Future[Result]): Future[Result] = {
		
		refine(request) flatMap (filterResult => {
			if (filterResult.isLeft) Future.successful(filterResult.left.get)
			else {
				block(request) map { blockResult =>
					
					// remove the cookie information if needed
					request.logoutResult.map({ lr =>
						
						if (lr.status == LogoutResultCode.SUCCESSFUL && config.credentialTarget == CredentialSource.COOKIE) {
							// logout successfully, removing the possible cookies
							blockResult.removingFromSession(Const.COOKIE_USERNAME_FIELD,
								Const.COOKIE_TOKEN_FIELD, Const.COOKIE_SESSION_ID_FIELD)(request)
							
						} else blockResult
					}) get
					
				}
			}
		})
	}
	
}


class DoLogin(ap: AccessProcessor, config: AuthenticationConfig)(implicit override val executionContext: ExecutionContext)
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

						if (lr.status == LoginResultCode.SUCCESSFUL && config.credentialTarget == CredentialSource.COOKIE) {
							
							// login successfully
							var result = blockResult.addingToSession(
								(Const.COOKIE_USERNAME_FIELD -> request.accessRequest.get.username.get),
									(Const.COOKIE_TOKEN_FIELD -> lr.token.get)
							)(request) // when login successfully, we can assume we have the token already)
							if (request.accessRequest.get.isInstanceOf[SessionAccessRequest])
								result = result.addingToSession(
									(Const.COOKIE_SESSION_ID_FIELD -> request.accessRequest.get.asInstanceOf[SessionAccessRequest].sessionId.get)
								)(request)

							result
						} else blockResult
					}) get

				}
			}
		})
	}

}
