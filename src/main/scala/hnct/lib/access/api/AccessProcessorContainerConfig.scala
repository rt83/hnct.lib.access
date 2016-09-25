package hnct.lib.access.api

case class AccessUnit(name : String, processor : Class[_ <: AccessProcessor[_, _]], config : AccessProcessorConfig)

case class AccessProcessorContainerConfig(defaultUnit : String, units : List[AccessUnit])