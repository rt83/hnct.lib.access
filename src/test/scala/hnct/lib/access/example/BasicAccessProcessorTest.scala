package hnct.lib.access.example

import hnct.lib.access.api.AccessProcessorFactory
import hnct.lib.utility.Logable
import hnct.lib.access.core.basic.BasicAccessRequest;
import hnct.lib.access.core.basic._
import hnct.lib.access.api._

/**
 * 
 */
object BasicAccessProcessorTest extends Logable {
	
	def main(args : Array[String]) : Unit = {
		val processor = AccessProcessorFactory.get()
		
		log.info(processor.toString())
		
		processor.map { p =>
			val basicRequest = new BasicAccessRequest("abc", "abc", 2000)	// create a login request, which requires a time out of 2 seconds
			val request = basicRequest.asInstanceOf[p.AccessRequestType]
			
			val loginResult = p.login(request)	// do the login
			
			loginResult.token.map {	token => 
				println(token)
				
				val sessionAccessor = p.loginSessionAccessor(request)
				sessionAccessor map { a =>		// check the session to see if the user token is loaded correctly
					println("Reading session")
					println("Read "+a.read("_token").get.value)				
				}
				
				val authenticateRequest = new BasicAccessRequest("abc", token).asInstanceOf[p.AccessRequestType]
				println("Authentication result: "+(p.authenticate(authenticateRequest).status))
				
				Thread.sleep(1000)
				
				println("Authentication result after 1s: "+(p.authenticate(authenticateRequest).status))
				
				Thread.sleep(2000)
				
				println("Authentication result after 3s: "+(p.authenticate(authenticateRequest).status))
				
			}

		}
		
	}
	
}