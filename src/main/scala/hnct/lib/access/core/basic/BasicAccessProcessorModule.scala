package hnct.lib.access.core.basic

import com.google.inject.AbstractModule
import hnct.lib.access.api.AccessProcessorFactory
import com.google.inject.multibindings.MapBinder
import com.google.inject.TypeLiteral
import com.google.inject.assistedinject.FactoryModuleBuilder
import hnct.lib.access.api.AccessProcessor
import hnct.lib.access.api.User

class BasicAccessProcessorModule extends AbstractModule {
	
	def configure() = {
		
		val mb : MapBinder[String, AccessProcessorFactory] =
			MapBinder.newMapBinder(binder(), classOf[String], classOf[AccessProcessorFactory])
		
		// install a module that provides an implementation of BasicAccessProcessorFactory
		install(
			new FactoryModuleBuilder().
					implement(classOf[AccessProcessor], classOf[BasicAccessProcessor]).
					build(classOf[BasicAccessProcessorFactory])
		)
			
		// add binding for the basic access processor
		// this will make the basic access processor factory available in the AccessProcessorContainer
		// and hence the basic access processor can be created.
		mb.addBinding(classOf[BasicAccessProcessor].getName()).to(classOf[BasicAccessProcessorFactory]);
		
	}
	
}