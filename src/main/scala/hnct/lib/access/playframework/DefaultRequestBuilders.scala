package hnct.lib.access.playframework

import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.multibindings.{MapBinder, Multibinder}
import hnct.lib.access.api.AccessRequest
import hnct.lib.access.core.basic.BasicAccessRequest

/**
  * Created by Ryan on 10/26/2016.
  */
class DefaultRequestBuilders extends AbstractModule {
  override def configure(): Unit = {
    val s = MapBinder.newMapBinder(binder(), new TypeLiteral[Class[_ <: AccessRequest]]() {}, new TypeLiteral[ConcreteRequestBuilder]() {})

    s.addBinding(classOf[BasicAccessRequest]).to(classOf[BasicAccessRequestBuilder])
  }
}
