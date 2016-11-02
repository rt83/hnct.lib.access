package hnct.lib.access.core.session

import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import hnct.lib.access.api.{AccessProcessorConfig, AccessRequest, DataAdapter, PasswordHasher}
import hnct.lib.access.core.basic.BasicAccessProcessor
import hnct.lib.session.api.{AccessorDescriptor, SessionAccessor, SessionContainer}

/**
  * Created by ryan on 11/2/2016.
  */
class SessionAccessProcessor @Inject()
(
	@Assisted() override protected[this] val _config: AccessProcessorConfig,
	private val sessionContainer: SessionContainer,
	override val adapters: java.util.Set[DataAdapter],
	override val hashers: java.util.Set[PasswordHasher]) extends BasicAccessProcessor(_config, sessionContainer, adapters, hashers) {

	override val ART : Class[_ <: AccessRequest] = classOf[SessionAccessRequest]

	override def loginSessionAccessor(req: AccessRequest): Option[SessionAccessor] = {
		if (config.useSession) {
			val r = req.asInstanceOf[SessionAccessRequest]

			if (r.sessionId.isEmpty) None
			else config.sessionUnit.fold(sessionContainer.getSession())(sessionContainer.getSession(_)).map {
				_.accessor(AccessorDescriptor(config.sessionNamespace, r.sessionId.get))
			}
		} else None
	}

}
