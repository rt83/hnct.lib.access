package hnct.lib.access.core.basic

import hnct.lib.access.api.AccessProcessorFactory
import hnct.lib.utility.Logable
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
			val request = new BasicAccessRequest("abc", "abc").asInstanceOf[p.AccessRequestType]
			p.login(request).token.map {
				println(_)
			}
			val sessionAccessor = p.loginSessionAccessor(request)
			sessionAccessor map { a =>
				println("Reading session")
				println(a.read("_token"))				
			}
		}
		
	}
	
}