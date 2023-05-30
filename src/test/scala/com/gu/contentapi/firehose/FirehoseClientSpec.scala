package com.gu.contentapi.firehose

import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.firehose.client.StreamListener
import com.gu.contentapi.firehose.kinesis.KinesisStreamReaderConfig
import com.gu.contentatom.thrift.Atom
import com.gu.crier.model.event.v1.{ DeletedContent, EventPayload, EventType, ItemType, RetrievableContent }
import org.slf4j.LoggerFactory
import org.specs2.mutable.Specification

class FirehoseClientSpec extends Specification {
  private val logger = LoggerFactory.getLogger(getClass)

  "FirehoseClient" should {
    "connect to a kinesis stream" in {
      logger.info("In test code")

      var contentUpdates = 0
      var retrievableUpdates = 0
      var deletedUpdates = 0
      var takedownUpdates = 0
      var atomUpdates = 0

      val listener = new StreamListener {
        /**
         * When content is updated or created on the Guardian an `update` event will be sent to the events stream. This
         * update event contains the entire payload.
         *
         * @param content
         */
        override def contentUpdate(content: Content): Unit = {
          logger.info("contentUpdate")
          contentUpdates += 1
        }

        /**
         * It is possible for an `update` event to exceed the upper limit of an event for our stream implementation.
         * In the event that this occurs, a `RetrievableUpdate` is sent containing a payload which allows the client
         * to retrieve the update themselves.
         *
         * @param content
         */
        override def contentRetrievableUpdate(content: RetrievableContent): Unit = {
          logger.info("contentRetrievableUpdate")
          retrievableUpdates += 1
        }

        /**
         * When content is deleted on the Guardian an update event will be sent to the events stream.
         *
         * @param content
         */
        override def contentDelete(content: DeletedContent): Unit = {
          logger.info("contentDelete")
          deletedUpdates += 1
        }

        /**
         * When content is removed from the Guardian a `takedown` event will be sent to the events stream. We expect all
         * consumers of the Content API and this events stream to respect these takedowns and remove the content from their
         * own downstream systems.
         *
         * @param contentId
         */
        override def contentTakedown(contentId: String): Unit = {
          logger.info("contentTakedown")
          takedownUpdates += 1
        }

        /**
         * The CAPI now publishes `Atoms` as well as content, which have their own flow.  This event is called whenever an atom is updated
         *
         * @param atom
         */
        override def atomUpdate(atom: Atom): Unit = {
          logger.info("atomUpdate")
          atomUpdates += 1
        }
      }

      val client = TestClient.createTestClient("my-test-stream-name", listener)

      logger.info("Starting up test client....")
      client.start()
      logger.info("Waiting")
      Thread.sleep(45000)
      logger.info("Dispatching test message...")
      client.sendMessage(EventType.Update, ItemType.Content, None, Some(EventPayload.Content(Content(
        "test-content-id",
        webTitle = "test content",
        webUrl = "https://path.to/some/content",
        apiUrl = "https://api.path.to/some/content"))))

      //      logger.info("Requesting shutdown")
      //      client.shutdown
      logger.info("Waiting")
      Thread.sleep(3000)
      logger.info("Wait completed")
      contentUpdates mustEqual 1
    }
  }
}
