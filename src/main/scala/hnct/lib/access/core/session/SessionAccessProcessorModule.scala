package hnct.lib.access.core.session

import com.google.inject.AbstractModule
import com.google.inject.assistedinject.FactoryModuleBuilder
import com.google.inject.multibindings.MapBinder
import hnct.lib.access.api.{AccessProcessor, AccessProcessorFactory}
import hnct.lib.access.core.basic.{BasicAccessProcessor, BasicAccessProcessorFactory}

/**
  * Created by ryan on 11/11/2016.
  */
class SessionAccessProcessorModule extends AbstractModule {

	override def configure(): Unit = {

		val mb : MapBinder[String, AccessProcessorFactory] =
			MapBinder.newMapBinder(binder(), classOf[String], classOf[AccessProcessorFactory])

		// install a module that provides an implementation of SessionAccessProcessorFactory
		install(
			new FactoryModuleBuilder().
				implement(classOf[AccessProcessor], classOf[SessionAccessProcessor]).
				build(classOf[SessionAccessProcessorFactory])
		)

		// add binding for the basic access processor
		// this will make the basic access processor factory available in the AccessProcessorContainer
		// and hence the basic access processor can be created.
		mb.addBinding(classOf[SessionAccessProcessor].getName()).to(classOf[SessionAccessProcessorFactory]);

	}

}
