package hnct.lib.access.api

import scala.concurrent.Future

/**
 * Access request builder is the interface that take an input
 * of IType and produce an access request of OType or an exception if
 * the build is not working
 */
trait AccessRequestBuilder[IType] {
	
	def build(input : IType, processor : AccessProcessor) : Future[AccessRequest]	// asynchronously build the access request
	
}