package hnct.lib.access.api

/**
 * Access request builder is the interface that take an input
 * of IType and produce an access request of OType
 */
trait AccessRequestBuilder[IType, OType <: AccessRequest] {
	
	def build(input : IType) : OType
	
}