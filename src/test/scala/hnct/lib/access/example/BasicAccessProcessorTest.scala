package hnct.lib.access.example

import hnct.lib.access.api.AccessProcessorFactory
import hnct.lib.utility.Logable
import hnct.lib.access.core.basic.BasicAccessRequest
import hnct.lib.access.core.basic._
import hnct.lib.access.api._
import com.google.inject.Injector
import com.google.inject.Guice
import hnct.lib.access.core.hashers.BasicHashersModule
import hnct.lib.access.core.hashers.MD5Hasher
import scala.concurrent.ExecutionContext.Implicits.global
import hnct.lib.session.api.SessionModule

/**
 * 
 */
object BasicAccessProcessorTest extends Logable {
	
	def main(args : Array[String]) : Unit = {
		
		val injector = Guice.createInjector(
				new BasicAccessProcessorModule(),	// provide the basic implementation of the access processor
				new AccessProcessorExampleModule(),	// provide the data adapter for this test
				new BasicHashersModule(),
				new SessionModule());			// provide the MD5 hasher
		
		val factory = injector.getInstance(classOf[BasicAccessProcessorFactory])	// retrieve the factory to create the BasicAccessProcessor
		
		val conf = new BasicAccessProcessorConfig()
		conf.dataAdapterClass = classOf[TestDataAdapter]
		conf.hasher = Some(classOf[MD5Hasher])
		
		val p = factory.create(conf);
		
		log.info(p.toString())
				
		val basicRequest = new BasicAccessRequest("abc", "abc", 2000)	// create a login request, which requires a time out of 2 seconds
		val request = basicRequest
		
		val loginResult = p.login(request)	// do the login
		
		loginResult map { _.token.map {	token => 
				println(token)
				
				val sessionAccessor = p.loginSessionAccessor(request)
				sessionAccessor map { a =>		// check the session to see if the user token is loaded correctly
					println("Reading session")
					a.read[String]("_token") map { v =>	println("Read "+v.get.value) }
				}
				
				val authenticateRequest = new BasicAccessRequest("abc", token)
				p.authenticate(authenticateRequest) map { result => println("Authentication result: " + result.status) }
				
				Thread.sleep(1000)
				
				p.authenticate(authenticateRequest) map { result => println("Authentication result after 1s: "+result.status) }
				
				Thread.sleep(2000)
				
				p.authenticate(authenticateRequest) map { result => println("Authentication result after 3s: "+result.status) }
				
			}
		}
		
		Thread.sleep(10000)	// sleep so that the main thread doesn't exit before things get executed

	}
	
}