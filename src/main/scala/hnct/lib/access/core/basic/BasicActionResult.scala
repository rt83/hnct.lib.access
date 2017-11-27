package hnct.lib.access.core.basic

import hnct.lib.access.api.results.ActionResult
import hnct.lib.access.api.results.ActionResultCode

case class BasicActionResult(
		override val request : Option[BasicAccessRequest],
		override val status : ActionResultCode,
		val message : String) extends ActionResult

object BasicActionResult {
	
	def apply(req : Option[BasicAccessRequest], message : String) = new BasicActionResult(req, ActionResultCode.FAILED, message)
	
}