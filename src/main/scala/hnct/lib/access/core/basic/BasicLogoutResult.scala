package hnct.lib.access.core.basic

import hnct.lib.access.api.results.LogoutResult
import hnct.lib.access.api.results.ActionResultCode

class BasicLogoutResult(
			override val request : BasicAccessRequest, 
			override val status : ActionResultCode)
		extends LogoutResult {
	
	def this(req : BasicAccessRequest) = this(req, ActionResultCode.FAILED)
	
}