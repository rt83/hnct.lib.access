package hnct.lib.access.playframework

import com.google.inject.multibindings.MapBinder
import com.google.inject.{AbstractModule, TypeLiteral}
import hnct.lib.access.api.AccessRequest
import hnct.lib.access.core.basic.BasicAccessRequest
import hnct.lib.access.core.session.SessionAccessRequest

/**
  * Created by Ryan on 10/26/2016.
  *
  * Provide the binding so that we can build default BasicAccessRequest and SessionAccessRequest
  * These binding will be availablve to the action builder through Guice Injection
  *
  */
class DefaultRequestBuilders extends AbstractModule {
	override def configure(): Unit = {
		val s = MapBinder.newMapBinder(binder(), new TypeLiteral[Class[_ <: AccessRequest]]() {}, new TypeLiteral[ConcreteRequestBuilder]() {})
		
		s.addBinding(classOf[BasicAccessRequest]).to(classOf[BasicAccessRequestBuilder])
		s.addBinding(classOf[SessionAccessRequest]).to(classOf[SessionAccessRequestBuilder])
	}
}
