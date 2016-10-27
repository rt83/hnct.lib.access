package hnct.lib.access.api

import scala.collection._
import hnct.lib.config.Configuration
import hnct.lib.config.ConfigurationFormat
import hnct.lib.utility.Logable
import com.google.inject.Inject
import java.util.Map

/**
 * The container is the class that holds all the access processor instances configured for the 
 * application
 */
class AccessProcessorContainer @Inject() (
			val config : AccessProcessorContainerConfig,
			
			// the map is provided through map binding. Each access processor implementation should provide its factory binding in the map.
			val factories : Map[String, AccessProcessorFactory]
			
		) extends Logable {
	
	private val auMap = mutable.HashMap[String, AccessProcessor]()	// access unit map
	
	// build the session from the configuration
	config.units.foreach { unit =>
		
		val factory = factories.get(unit.processor.getClass().getName());
		
		if (factory == null)
			throw new RuntimeException("Unable to find the factory to create access unit "+unit.name);
		
		val accessUnit = factory.create(unit.config);
		
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
	
	def get = {	// at runtime user should know what they are using
		auMap.get(config.defaultUnit)
	}
	
	def get(unitName : String) : Option[AccessProcessor] = {
		
		if (unitName.isEmpty()) get
		else auMap.get(unitName)
		
	}
	
}