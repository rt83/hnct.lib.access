package hnct.lib.access.core.session

import hnct.lib.access.core.basic.BasicAccessRequest

/**
  * Created by Ryan on 10/26/2016.
  */
class SessionAccessRequest(
  override val username : Option[String],
  override val token : Option[String],
  override val timeout : Long,
  val sessionId : Option[String]) extends BasicAccessRequest(username, token, timeout) {

  def this(un : Option[String], t : Option[String], sid : Option[String]) = this(un, t, -1, sid)

}
