package hnct.lib.access.playframework

import com.google.inject.AbstractModule
import hnct.lib.access.api.AccessModule
import hnct.lib.access.core.basic.BasicAccessProcessorModule
import hnct.lib.access.core.hashers.BasicHashersModule

class PlayAuthModule extends AbstractModule {

	def configure = {
		install(new BasicAccessProcessorModule())
		install(new BasicHashersModule())


	}
	
}