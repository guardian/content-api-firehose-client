package com.gu.contentapi.firehose

import com.amazonaws.services.kinesis.clientlibrary.interfaces.{ IRecordProcessor, IRecordProcessorFactory }
import com.gu.contentapi.firehose.client.PublicationLogic
import com.gu.contentapi.firehose.kinesis.{ KinesisStreamReader, KinesisStreamReaderConfig, SingleEventProcessor }
import com.gu.crier.model.event.v1.EventPayload.UnknownUnionField
import com.gu.crier.model.event.v1.EventType.EnumUnknownEventType
import com.gu.crier.model.event.v1.{ Event, EventPayload, EventType }
import scala.concurrent.duration._

class ContentApiFirehoseConsumer(
    val kinesisStreamReaderConfig: KinesisStreamReaderConfig,
    val logic: PublicationLogic

) extends KinesisStreamReader {

  val eventProcessorFactory = new IRecordProcessorFactory {
    override def createProcessor(): IRecordProcessor =
      new ContentApiEventProcessor(kinesisStreamReaderConfig.checkpointInterval, kinesisStreamReaderConfig.maxCheckpointBatchSize, logic)
  }
}

class ContentApiEventProcessor(override val checkpointInterval: Duration, override val maxCheckpointBatchSize: Int, publicationLogic: PublicationLogic) extends SingleEventProcessor[Event] {

  val codec = Event

  override protected def processEvent(event: Event): Unit = {
    event.eventType match {

      case EventType.Update | EventType.RetrievableUpdate =>
        event.payload.foreach { payload =>
          payload match {
            case EventPayload.Content(content) => publicationLogic.contentUpdate(content)
            case EventPayload.RetrievableContent(content) => publicationLogic.contentRetrievableUpdate(content)
            case UnknownUnionField(e) => logger.warn(s"Received an unknown event payload $e. You should possibly consider updating")
          }
        }

      case EventType.Delete => publicationLogic.contentTakedown(event.payloadId)

      case EnumUnknownEventType(e) => logger.warn(s"Received an unknown event type $e")
    }
  }

}
