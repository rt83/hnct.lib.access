package hnct.lib.access.playframework

import com.google.inject.Inject
import hnct.lib.access.api.{AccessProcessor, AccessProcessorContainer, AccessRequest}
import play.api.mvc.{ActionFunction, Request}

/**
  * This file contains the two utility class so that play application can conveniently build
  * the authentication and login action. These utility class should be obtained through GUICE so that
  * concrete access request builder can be injected. These utility classes take in the access processor,
  * the configuration for access checking in play and then return the combined action builder for authentication
  * or login by combining the PlayARBuilder action function with the Authenticate or DoLogin action function.
  */

/**
  * Utility for authentication. The resulting action will authenticate and save the
  * result into the PlayHTTPRequest instance built by the play access request builders
  */
class PlayAuth {

	@Inject var reqBuilder: java.util.Map[Class[_ <: AccessRequest], ConcreteRequestBuilder] = _

	/**
	  * Create an action that check if the user is logged in
	  * before invoking the block.
	  *
	  * The access processor and configuration can either be supplied explicitly or provided implicitly from
	  * the user of this class. See example for more details
	  */
	def apply()(
		implicit ap: AccessProcessor,
		config: AuthenticationConfig): ActionFunction[Request, PlayHTTPRequest] = {

		val cb = reqBuilder.get(ap.ART)

		new PlayAuthARBuilder(config, ap, cb) andThen new Authenticate(ap, config)
	}

	/**
	  * In many cases, access processor are created and configured through the access processor container (so that
	  * no manual binding is required), the access processor can be retrieved through its name. This method
	  * create the play auth action using the access processor retrieved using the specified name.
	  */
	def apply(apName: String)(implicit apc: AccessProcessorContainer, conf: AuthenticationConfig): ActionFunction[Request, PlayHTTPRequest] = {

		implicit val ap = apc.get(apName).getOrElse(throw new RuntimeException("Unable to find the access processor " + apName))

		apply()

	}

}

/**
  * Utility for Logging in. The resulting action will handle the login form and save the
  * result into the PlayHTTPRequest instance built by the login access request builders. Note that, for login
  * request, the builder will always build the access request from Form
  */
class PlayLogin {

	@Inject var reqBuilder: java.util.Map[Class[_ <: AccessRequest], ConcreteRequestBuilder] = _

	/**
	  * Create an action that check if the user is logged in
	  * before invoking the block.
	  *
	  * The access processor and configuration can either be supplied explicitly or provided implicitly from
	  * the user of this class. See example for more details
	  */
	def apply()(
		implicit ap: AccessProcessor,
		config: AuthenticationConfig): ActionFunction[Request, PlayHTTPRequest] = {

		val cb = reqBuilder.get(ap.ART)

		new PlayLoginARBuilder(config, ap, cb) andThen new DoLogin(ap, config)

	}

	/**
	  * In many cases, access processor are created and configured through the access processor container (so that
	  * no manual binding is required), the access processor can be retrieved through its name. This method
	  * create the play auth action using the access processor retrieved using the specified name.
	  */
	def apply(apName: String)(implicit apc: AccessProcessorContainer, conf: AuthenticationConfig): ActionFunction[Request, PlayHTTPRequest] = {

		implicit val ap = apc.get(apName).getOrElse(throw new RuntimeException("Unable to find the access processor " + apName))

		apply()

	}

}