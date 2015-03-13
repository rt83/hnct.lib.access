package hnct.lib.access.api

import scala.collection._
import hnct.lib.config.Configuration
import hnct.lib.config.ConfigurationFormat

object AccessProcessorFactory {
	
	private val configFileName = "access.json"
	private val systemPropName = "accessConfigFile"
	
	private val auMap = mutable.HashMap[String, AccessProcessor]()	// access unit map
	
	// read the configuration
	private val config = Configuration.read(
				Some(configFileName), 
				Some(systemPropName), 
				classOf[AccessProcessorFactoryConfig], 
				ConfigurationFormat.JSON
	).
	// throw exception when cannot read the configuration file successfully
	getOrElse(throw new RuntimeException("Could not load the access configuration file"))
	
	// build the session from the configuration
	config.units.foreach { unit =>
		val accessUnit = unit.processor.newInstance()
		
		accessUnit.configure(unit.config.asInstanceOf[accessUnit.ConfigType])
		
		auMap + (unit.name -> accessUnit)
	}
	
	if (config.defaultUnit != null && !config.defaultUnit.isEmpty()) {
		// validation to see whether the default session unit exist
		if (auMap.get(config.defaultUnit) == None) {
			// print out some warning
		}
	} else {
		// print some warning that the default access unit is not defined
	}
	
	def get() = {
		auMap.get(config.defaultUnit)
	}
	
	def get(unitName : String) = {
		auMap.get(unitName)
	}
	
}