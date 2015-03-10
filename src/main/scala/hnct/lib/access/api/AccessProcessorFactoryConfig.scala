package hnct.lib.access.api

case class AccessUnit(name : String, processor : Class[_ <: AccessProcessor], config : AccessProcessorConfig)

case class AccessProcessorFactoryConfig(defaultUnit : String, units : List[AccessUnit])