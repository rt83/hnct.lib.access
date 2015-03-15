package hnct.lib.access.core.basic

import hnct.lib.access.api.ActionResult
import hnct.lib.access.api.ActionResultCode

class BasicActionResult(req : BasicAccessRequest, s : ActionResultCode) extends ActionResult[BasicAccessRequest] {
	
	def this(req : BasicAccessRequest) = this(req, ActionResultCode.FAILED)
	
	request = req
	status = s
	
}