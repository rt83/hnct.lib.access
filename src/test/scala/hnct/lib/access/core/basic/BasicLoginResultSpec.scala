package hnct.lib.access.core.basic

//import hnct.lib.access.api.results.LoginResultCode
//
//class BasicLoginResultSpec extends UnitSpec {
//
//	"A BasicLoginResult" should "be able to initialize with request and LoginResultCode only" in {
//		val result = BasicLoginResult(new BasicAccessRequest(Some("ryan"), Some("password")), LoginResultCode.FAILED_INVALID_CREDENTIALS)
//
//		result.request.username.get should be ("ryan")
//		result.user should be (None)
//		result.status should be (LoginResultCode.FAILED_INVALID_CREDENTIALS)
//		result.token should be (None)
//
//	}
//
//}