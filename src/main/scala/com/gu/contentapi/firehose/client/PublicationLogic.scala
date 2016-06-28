package com.gu.contentapi.firehose.client

import com.gu.contentapi.client.model.v1.Content
import com.gu.crier.model.event.v1.EventPayload.RetrievableContent

/**
 * Client interface to implement for providing logic to handle various types of events.
 */
trait PublicationLogic {

  def contentUpdate(content: Content): Unit
  def contentRetrievableUpdate(content: RetrievableContent): Unit
  def contentTakedown(contentId: String): Unit

}
