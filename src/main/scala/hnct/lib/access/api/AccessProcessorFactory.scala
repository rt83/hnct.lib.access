package hnct.lib.access.api

import scala.collection._
import hnct.lib.config.Configuration
import hnct.lib.config.ConfigurationFormat
import hnct.lib.utility.Logable

class AccessProcessorFactory(val config : AccessProcessorFactoryConfig) extends Logable {
	
	private val auMap = mutable.HashMap[String, AccessProcessor[_, _, _]]()	// access unit map
	
	// build the session from the configuration
	config.units.foreach { unit =>
		val accessUnit = unit.processor.newInstance()
		
		accessUnit.configure(unit.config.asInstanceOf[accessUnit.ConfigType])
		
		log.info("Access unit {} was initialized and added to the map.", unit.name)
		
		auMap.put(unit.name, accessUnit)
		
	}
	
	if (config.defaultUnit != null && !config.defaultUnit.isEmpty()) {
		// validation to see whether the default session unit exist
		if (auMap.get(config.defaultUnit) == None) {
			// print out some warning
		}
	} else {
		// print some warning that the default access unit is not defined
	}
	
	def get = {
		auMap.get(config.defaultUnit)
	}
	
	def get(unitName : String) : Option[AccessProcessor[_,_,_]] = {
		
		if (unitName.isEmpty()) get
		else auMap.get(unitName)
	}
	
}

/**
 * Companion object to create the factory from file name
 */
object AccessProcessorFactory {
	
	private val configFileName = "access.json"
	private val systemPropName = "accessConfigFile"
	
	def apply() : AccessProcessorFactory = apply(configFileName, systemPropName)
	
	def apply(fileName : String, systemPropName : String) = {
		val config = Configuration.read(
				Some(configFileName), 
				Some(systemPropName), 
				classOf[AccessProcessorFactoryConfig], 
				ConfigurationFormat.JSON
		).
		// throw exception when cannot read the configuration file successfully
		getOrElse(throw new RuntimeException("Could not load the access configuration file"))
		
		new AccessProcessorFactory(config)
	}
	
}