package com.gu.contentapi.firehose

import com.amazonaws.http.apache.client.impl.SdkHttpClient
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.firehose.client.StreamListener
import com.gu.contentapi.firehose.kinesis.KinesisStreamReaderConfig
import com.gu.contentatom.thrift.Atom
import com.gu.crier.model.event.v1.{ DeletedContent, Event, EventPayload, EventType, ItemType, RetrievableContent }
import com.twitter.scrooge.ThriftStruct
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.transport.TMemoryBuffer
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, AwsCredentialsProvider, StaticCredentialsProvider }
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.apache.internal.impl.ApacheSdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.{ CreateStreamRequest, DescribeStreamRequest, ListStreamsRequest, PutRecordRequest, PutRecordsRequestEntry, ResourceNotFoundException, StreamMode, StreamModeDetails, StreamStatus }

import java.net.URI
import java.nio.ByteBuffer
import java.time.{ Instant, ZonedDateTime }
import java.util.UUID
import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global

class TestClient(kinesisStreamReaderConfig: KinesisStreamReaderConfig, credentialsProvider: AwsCredentialsProvider, streamListener: StreamListener, internalKinesisClient: KinesisClient)
  extends ContentApiFirehoseConsumer(kinesisStreamReaderConfig, credentialsProvider, streamListener, false) {
  private final val logger = LoggerFactory.getLogger(getClass)

  def sendMessage(eventType: EventType, itemType: ItemType, sentAt: Option[ZonedDateTime], payload: Option[EventPayload]) = {
    val id = UUID.randomUUID()

    logger.info(s"Sending message with id $id of type $eventType with item type $itemType to ${kinesisStreamReaderConfig.streamName}")
    val timestamp = sentAt match {
      case Some(timestamp) =>
        timestamp.toInstant.getEpochSecond
      case None =>
        Instant.now().getEpochSecond
    }

    val evt: Event = Event(
      payloadId = id.toString,
      eventType,
      itemType,
      timestamp,
      payload)

    internalKinesisClient.putRecord(PutRecordRequest.builder()
      .streamName(kinesisStreamReaderConfig.streamName)
      .data(SdkBytes.fromByteBuffer(TestClient.serializeToByteBuffer(evt)))
      .partitionKey(evt.payloadId)
      .build())
  }
}

object TestClient {
  private val logger = LoggerFactory.getLogger(getClass)
  private val localStackEndpoint = new URI("http://localhost:4566")
  private val fakeCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create("localstack-user", "localstack-key"))
  private val internalKinesisClient = KinesisClient.builder()
    .httpClientBuilder(ApacheHttpClient.builder())
    .region(Region.US_EAST_1)
    .credentialsProvider(fakeCredentialsProvider)
    .endpointOverride(localStackEndpoint)
    .build()

  private val ThriftBufferInitialSize = 128
  def serializeToByteBuffer(struct: ThriftStruct): ByteBuffer = {
    val buffer = new TMemoryBuffer(ThriftBufferInitialSize)
    val protocol = new TCompactProtocol(buffer)
    struct.write(protocol)
    ByteBuffer.wrap(buffer.getArray)
  }

  def checkStreamExists(streamName: String) = {
    try {
      Some(internalKinesisClient.describeStream(DescribeStreamRequest.builder().streamName(streamName).build()))
    } catch {
      case _: ResourceNotFoundException =>
        None
      case err: Throwable =>
        throw err
    }
  }

  def waitForStream(streamName: String): Unit = {
    checkStreamExists(streamName) match {
      case Some(info) =>
        if (info.streamDescription().streamStatus() != StreamStatus.ACTIVE) {
          Thread.sleep(1000)
          waitForStream(streamName)
        }
      case None =>

    }
  }

  def createStreamName(basepart: String): String = {
    var i = 0
    for (i <- 0 to 100000) {
      val potentialStreamName = s"$basepart-$i"
      if (checkStreamExists(potentialStreamName).isEmpty) {
        logger.info(s"Test stream is called $potentialStreamName")
        internalKinesisClient.createStream(
          CreateStreamRequest.builder()
            .streamName(potentialStreamName)
            .shardCount(1)
            .streamModeDetails(
              StreamModeDetails.builder()
                .streamMode(StreamMode.PROVISIONED)
                .build())
            .build())
        waitForStream(potentialStreamName)
        return potentialStreamName
      }
    }
    throw new RuntimeException("Tried 100000 potential unique stream names and all were taken. Something is wrong here.")
  }

  def createTestClient(streamName: String, streamListener: StreamListener): TestClient = {
    val config = KinesisStreamReaderConfig(
      createStreamName(streamName),
      "test-specification",
      "TEST",
      "LIVE",
      None,
      fakeCredentialsProvider,
      fakeCredentialsProvider,
      "us-east-1", //default value for localstack
      endpointOverride = Some(localStackEndpoint))

    val initialisationPromise = Promise[Unit]()

    new TestClient(config, fakeCredentialsProvider, new StreamListener {
      /**
       * When content is updated or created on the Guardian an `update` event will be sent to the events stream. This
       * update event contains the entire payload.
       *
       * @param content
       */
      override def contentUpdate(content: Content): Unit = streamListener.contentUpdate(content)

      /**
       * It is possible for an `update` event to exceed the upper limit of an event for our stream implementation.
       * In the event that this occurs, a `RetrievableUpdate` is sent containing a payload which allows the client
       * to retrieve the update themselves.
       *
       * @param content
       */
      override def contentRetrievableUpdate(content: RetrievableContent): Unit = streamListener.contentRetrievableUpdate(content)

      /**
       * When content is deleted on the Guardian an update event will be sent to the events stream.
       *
       * @param content
       */
      override def contentDelete(content: DeletedContent): Unit = streamListener.contentDelete(content)

      /**
       * When content is removed from the Guardian a `takedown` event will be sent to the events stream. We expect all
       * consumers of the Content API and this events stream to respect these takedowns and remove the content from their
       * own downstream systems.
       *
       * @param contentId
       */
      override def contentTakedown(contentId: String): Unit = streamListener.contentTakedown(contentId)

      /**
       * The CAPI now publishes `Atoms` as well as content, which have their own flow.  This event is called whenever an atom is updated
       *
       * @param atom
       */
      override def atomUpdate(atom: Atom): Unit = streamListener.atomUpdate(atom)

      override def listenerStarted(): Unit = {
        initialisationPromise.success(())
      }
    }, internalKinesisClient)
  }
}
