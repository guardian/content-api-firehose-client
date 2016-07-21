package com.gu.contentapi.firehose

import com.amazonaws.services.kinesis.clientlibrary.interfaces.{ IRecordProcessor, IRecordProcessorFactory }
import com.gu.contentapi.client.model.v1.Content
import com.gu.contentapi.firehose.client.StreamListener
import com.gu.contentapi.firehose.kinesis.{ KinesisStreamReader, KinesisStreamReaderConfig, SingleEventProcessor }
import com.gu.crier.model.event.v1.EventPayload.UnknownUnionField
import com.gu.crier.model.event.v1.EventType.EnumUnknownEventType
import com.gu.crier.model.event.v1.{ Event, EventPayload, EventType }

import scala.concurrent.duration._

class ContentApiFirehoseConsumer(
    val kinesisStreamReaderConfig: KinesisStreamReaderConfig,
    val streamListener: StreamListener,
    val filterProductionMonitoring: Boolean = false

) extends KinesisStreamReader {

  val eventProcessorFactory = new IRecordProcessorFactory {
    override def createProcessor(): IRecordProcessor =
      new ContentApiEventProcessor(filterProductionMonitoring, kinesisStreamReaderConfig.checkpointInterval, kinesisStreamReaderConfig.maxCheckpointBatchSize, streamListener)
  }
}

class ContentApiEventProcessor(filterProductionMonitoring: Boolean, override val checkpointInterval: Duration, override val maxCheckpointBatchSize: Int, streamListener: StreamListener) extends SingleEventProcessor[Event] {

  val codec = Event

  override protected def processEvent(event: Event): Unit = {
    event.eventType match {

      case EventType.Update | EventType.RetrievableUpdate =>
        event.payload.foreach { payload =>
          payload match {
            case EventPayload.Content(content) => streamListener.contentUpdate(content)
            case EventPayload.RetrievableContent(content) => streamListener.contentRetrievableUpdate(content)
            case UnknownUnionField(e) => logger.warn(s"Received an unknown event payload $e. You should possibly consider updating")
          }
        }

      case EventType.Delete =>
        if (filterProductionMonitoring && event.payloadId.startsWith("production-monitoring")) {
          // do nothing.
        } else {
          streamListener.contentTakedown(event.payloadId)
        }

      case EnumUnknownEventType(e) => logger.warn(s"Received an unknown event type $e")
    }
  }

}
