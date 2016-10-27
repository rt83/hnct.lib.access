package hnct.lib.access.core.session

import hnct.lib.access.core.basic.BasicAccessRequest

/**
  * Created by Ryan on 10/26/2016.
  */
class SessionAccessRequest(
  override val username : String,
  override val token : String,
  override val timeout : Long,
  val sessionId : String) extends BasicAccessRequest(username, token, timeout) {

  def this(un : String, t : String, sid : String) = this(un, t, -1, sid)

}
