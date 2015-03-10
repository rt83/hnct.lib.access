package hnct.lib.access.core.basic

import hnct.lib.access.api.LoginResultCode

class BasicLoginResultSpec extends UnitSpec {
	
	"A BasicLoginResult" should "be able to initialize with request and LoginResultCode only" in {
		val result = new BasicLoginResult(new BasicAccessRequest("ryan", "password"), LoginResultCode.FAILED_INVALID_CREDENTIALS)
		
		result.request.username should be ("ryan")
		result.user should be (None)
		result.status should be (LoginResultCode.FAILED_INVALID_CREDENTIALS)
		result.token should be (None)
		
	}
	
}