package hnct.lib.access.playframework

import com.google.inject.AbstractModule
import hnct.lib.access.core.basic.BasicAccessProcessorModule
import hnct.lib.access.core.hashers.BasicHashersModule
import hnct.lib.access.core.session.SessionAccessProcessorModule

class PlayAuthModule extends AbstractModule {

	def configure = {
		install(new BasicAccessProcessorModule())
		install(new SessionAccessProcessorModule())
		install(new BasicHashersModule())


	}
	
}