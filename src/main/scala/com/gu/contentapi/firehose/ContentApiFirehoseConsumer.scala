package com.gu.contentapi.firehose

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{ IRecordProcessor, IRecordProcessorFactory }
import com.gu.contentapi.firehose.client.PublicationLogic
import com.gu.contentapi.firehose.kinesis.{ EventProcessor, KinesisStreamReader, SingleEventProcessor }
import com.gu.crier.model.event.v1.EventPayload.UnknownUnionField
import com.gu.crier.model.event.v1.EventType.EnumUnknownEventType
import com.gu.crier.model.event.v1.{ Event, EventPayload, EventType }

class ContentApiFirehoseConsumer(
    val app: String,
    val mode: String,
    val suffix: Option[String],
    val streamName: String,
    val stage: String,
    val kinesisCredentialsProvider: AWSCredentialsProvider,
    val dynamoCredentialsProvider: AWSCredentialsProvider,
    val awsRegion: String,
    val logic: PublicationLogic
) extends KinesisStreamReader {

  val eventProcessorFactory = new IRecordProcessorFactory {
    override def createProcessor(): IRecordProcessor = new CapiEventProcessor(logic)
  }
}

class CapiEventProcessor(publicationLogic: PublicationLogic) extends EventProcessor[Event] with SingleEventProcessor[Event] {

  val codec = Event

  override protected def processEvent(eventWithSize: EventProcessor.EventWithSize[Event]): Unit = {
    val event = eventWithSize.event
    event.eventType match {

      case EventType.Update | EventType.RetrievableUpdate =>
        event.payload.foreach { payload =>
          payload match {
            case content: EventPayload.Content => publicationLogic.contentUpdate(content.content)
            case content: EventPayload.RetrievableContent => publicationLogic.contentRetrievableUpdate(content)
            case UnknownUnionField(e) => logger.warn(s"ContentApiFirehoseConsumer: received an unknown event payload $e. You should possibly consider updating")
          }
        }

      case EventType.Delete => publicationLogic.contentTakedown(event.payloadId)

      case EnumUnknownEventType(e) => logger.warn(s"ContentApiFirehoseConsumer: received an unknown event type $e")
    }
  }

}
