package hnct.lib.access.playframework

import hnct.lib.access.api.AccessRequestBuilder
import play.api.mvc.Request
import hnct.lib.access.api.AccessRequest
import hnct.lib.access.api.AccessProcessor
import play.api.mvc.ActionTransformer
import scala.concurrent.Future
import hnct.lib.access.core.basic.BasicAccessRequest

/**
 * Default implementation of a request builder used for the provided implementations
 * of access processors. The 
 * 
 * Support:
 * - BasicAccessRequest
 * - SessionAccessRequest
 */
class PlayRequestBuilder(config : AuthenticationConfig, accessProcessor : AccessProcessor) 
extends AccessRequestBuilder[Request[_], AccessRequest] with ActionTransformer[Request, WrappedAccessRequest] {
	
	def build(request : Request[_]) = {
		if (config.credentialSource.equals("COOKIE")) {	// build the credentials from COOKIE
			
			// TODO: to extract information from the original request and build the access request
			new BasicAccessRequest("COOKIE", "COOKIE")	// TEST CODE, TO BE REMOVED
			
		} else {	// build credentials from FORM
			
			// TODO: to extract information from the original request and build the access request
			new BasicAccessRequest("FORM", "FORM")		// TEST CODE, TO BE REMOVED
			
		}
	}

	def transform[A](request: Request[A]): Future[WrappedAccessRequest[A]] = {
		val ar = build(request)
		
		Future.successful(new WrappedAccessRequest(request, ar))
	}
	
}