package com.gu.contentapi.firehose

import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.firehose.client.StreamListener
import com.gu.contentapi.firehose.kinesis.KinesisStreamReaderConfig
import com.gu.contentatom.thrift.Atom
import com.gu.crier.model.event.v1.{ DeletedContent, RetrievableContent }
import org.specs2.mutable.Specification
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, AwsCredentialsProvider, StaticCredentialsProvider }

import java.net.URI

class FirehoseClientSpec extends Specification {
  "FirehoseClient" should {
    "connect to a kinesis stream" in {
      val fakeCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("localstack-user", "localstack-key"))

      val config = KinesisStreamReaderConfig(
        "testfirehose",
        "test-specification",
        "TEST",
        "LIVE",
        None,
        fakeCredentialsProvider,
        fakeCredentialsProvider,
        "eu-west-1",
        endpointOverride = Some(new URI("http://localhost:4572")))

      val client = new ContentApiFirehoseConsumer(config, fakeCredentialsProvider, new StreamListener {
        /**
         * When content is updated or created on the Guardian an `update` event will be sent to the events stream. This
         * update event contains the entire payload.
         *
         * @param content
         */
        override def contentUpdate(content: Content): Unit = ???

        /**
         * It is possible for an `update` event to exceed the upper limit of an event for our stream implementation.
         * In the event that this occurs, a `RetrievableUpdate` is sent containing a payload which allows the client
         * to retrieve the update themselves.
         *
         * @param content
         */
        override def contentRetrievableUpdate(content: RetrievableContent): Unit = ???

        /**
         * When content is deleted on the Guardian an update event will be sent to the events stream.
         *
         * @param content
         */
        override def contentDelete(content: DeletedContent): Unit = ???

        /**
         * When content is removed from the Guardian a `takedown` event will be sent to the events stream. We expect all
         * consumers of the Content API and this events stream to respect these takedowns and remove the content from their
         * own downstream systems.
         *
         * @param contentId
         */
        override def contentTakedown(contentId: String): Unit = ???

        /**
         * The CAPI now publishes `Atoms` as well as content, which have their own flow.  This event is called whenever an atom is updated
         *
         * @param atom
         */
        override def atomUpdate(atom: Atom): Unit = ???
      })
      client must not beNull

      val newThread = client.start()

      newThread.getState mustEqual Thread.State.RUNNABLE
      newThread.join()
      newThread.getState mustEqual Thread.State.TERMINATED
    }
  }
}
