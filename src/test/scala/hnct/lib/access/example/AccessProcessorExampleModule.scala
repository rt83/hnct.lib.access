package hnct.lib.access.example

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import hnct.lib.access.api.DataAdapter

class AccessProcessorExampleModule extends AbstractModule {
	
	def configure = {
		val sb = Multibinder.newSetBinder(binder(), classOf[DataAdapter])
		sb.addBinding().to(classOf[TestDataAdapter])
	}
	
}