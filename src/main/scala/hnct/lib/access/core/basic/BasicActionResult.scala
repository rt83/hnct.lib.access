package hnct.lib.access.core.basic

import hnct.lib.access.api.results.ActionResult
import hnct.lib.access.api.results.ActionResultCode

class BasicActionResult(
		override val request : BasicAccessRequest, 
		override val status : ActionResultCode) extends ActionResult[BasicAccessRequest] {
	
	def this(req : BasicAccessRequest) = this(req, ActionResultCode.FAILED)
	
}