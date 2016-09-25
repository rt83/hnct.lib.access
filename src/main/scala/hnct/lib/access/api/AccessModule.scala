package hnct.lib.access.api

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import hnct.lib.config.Configuration
import hnct.lib.config.ConfigurationFormat

/**
 * The access module should be installed by the using application.
 * 
 * The Access Module will read the configuration file and extract all access processor config. 
 * It also provides a factory from which application can obtain acess processor instances. One application
 * might have multiple access processor
 */
class AccessModule(private val config : AccessProcessorContainerConfig) extends AbstractModule {
	
	def this() = this(
			config = Configuration.read(
					Some(AccessModule.configFileName), 
					Some(AccessModule.systemPropName), 
					classOf[AccessProcessorContainerConfig], 
					ConfigurationFormat.JSON
			).
			// throw exception when cannot read the configuration file successfully
			getOrElse(throw new RuntimeException("Could not load the access configuration file"))
	)
	
	def configure = {
		this.bind(classOf[AccessProcessorContainer]).in(classOf[Singleton]);
		
		// bind a singleton for the factory configuration
		bind(classOf[AccessProcessorContainerConfig]).toInstance(config);
		
	}
	
}

object AccessModule {
	private val configFileName = "access.json"
	private val systemPropName = "accessConfigFile"
}