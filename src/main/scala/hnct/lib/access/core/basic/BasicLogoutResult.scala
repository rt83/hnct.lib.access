package hnct.lib.access.core.basic

import hnct.lib.access.api.LogoutResult
import hnct.lib.access.api.ActionResultCode

class BasicLogoutResult(
			_r : BasicAccessRequest, 
			_s : ActionResultCode)
		extends LogoutResult[BasicAccessRequest] {
	
	status = _s
	request = _r
	
	def this(req : BasicAccessRequest) = this(req, ActionResultCode.FAILED)
	
}