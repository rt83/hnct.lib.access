package hnct.lib.access.playframework

import hnct.lib.access.api.AccessRequest
import play.api.mvc.Request

/**
 * This class wrapped around an original HTTP request, and the corresponding access request
 * built from the original HTTP request. By default, user can use PlayRequestBuilder to build
 * the access request. Since the PlayRequestBuilder is also an ActionTransformer, it will also
 * transform the original request to WrappedAccessRequest.
 * 
 * This class extends Request[A] so that user can treat this as a normal request if wanted, and
 * it'll be more convenient to write code that doesn't relate to access checking
 */
class WrappedAccessRequest[A](originalRequest : Request[A], accessRequest : AccessRequest) extends Request[A] {

	// Members declared in play.api.mvc.Request
	def body: A = originalRequest.body

	// Members declared in play.api.mvc.RequestHeader
	def headers: play.api.mvc.Headers = originalRequest.headers
	def id: Long = originalRequest.id
	def method: String = originalRequest.method
	def path: String = originalRequest.path
	def queryString: Map[String, Seq[String]] = originalRequest.queryString
	def remoteAddress: String = originalRequest.remoteAddress
	def secure: Boolean = originalRequest.secure
	def tags: Map[String, String] = originalRequest.tags
	def uri: String = originalRequest.uri
	def version: String = originalRequest.version

}