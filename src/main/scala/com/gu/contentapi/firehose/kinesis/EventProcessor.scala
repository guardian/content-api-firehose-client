package com.gu.contentapi.firehose.kinesis

import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{ IRecordProcessor, IRecordProcessorCheckpointer }
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.typesafe.scalalogging.LazyLogging
import com.twitter.scrooge.ThriftStruct
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }
import java.util.{ List => JList }

import com.gu.contentapi.firehose.kinesis.EventProcessor.EventWithSize

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

abstract class EventProcessor[EventT <: ThriftStruct](
  checkpointInterval: Duration = 30.seconds,
  maxCheckpointBatchSize: Int = 20
)
    extends IRecordProcessor
    with ThriftDeserializer[EventT]
    with LazyLogging {

  private[this] var shardId: String = _

  /* Use atomic to prevent any concurrent access issues */
  private[this] val lastCheckpointedAt = new AtomicLong(System.nanoTime())
  private[this] val recordsProcessedSinceCheckpoint = new AtomicInteger()

  override def initialize(shardId: String): Unit = {
    this.shardId = shardId
    logger.info(s"Initialized an event processor for shard $shardId")
  }

  override def processRecords(records: JList[Record], checkpointer: IRecordProcessorCheckpointer): Unit = {
    processRecordsIfActivated(records, checkpointer)
  }

  private def processRecordsIfActivated(records: JList[Record], checkpointer: IRecordProcessorCheckpointer): Unit = {
    val events = records.asScala.flatMap { record =>
      val buffer = record.getData
      val op = deserializeEvent(buffer)
      op match {
        case Success(event) => Some(EventWithSize(event, buffer.array.length))
        case Failure(e) => {
          logger.error(s"deserialization of event buffer failed: ${e.getMessage}", e)
          None
        }
      }
    }

    processEvents(events)

    /*
     Several errors can be encountered:
     - Compression unsupported
     The thrift bytes have been compressed with a compression algo which is not yet supported by porter.
     - Thrift protocol decoding exception
     The thrift format we are using for decoding the object is not compatible with the one that has been used to encode the message.
     - Thrift transport exception
     The byte buffer we are consuming has thrown an IO exception.
     Recovering options are:
     - Stop the processing of records for the porter instance
     We stop to process the records using a scala.util.control.Breaks.breakable and an atomic boolean.
     The processing could not be (re)enabled without restarting the instance.
     - Ignore the record
     We skip the record and continue to process the next one.
     Given the ability we have to recreate new events in the stream by launching a reindex, or relaunching/resaving an article,
     the most appropriate and simplest recovering option is to ignore the record.
     Note that one of the drawback of the binary serialisation is that we do not know the content id associated to the event.
     As a result any error reported on production should be considered as critical and fixed immediately.
     */

    /* increment the record counter */
    recordsProcessedSinceCheckpoint.addAndGet(events.size)

    if (shouldCheckpointNow) {
      checkpoint(checkpointer)
    }
  }

  protected def processEvents(events: Seq[EventWithSize[EventT]]): Unit

  /* Checkpoint after every X seconds or every Y records */
  private def shouldCheckpointNow =
    recordsProcessedSinceCheckpoint.get() >= maxCheckpointBatchSize ||
      lastCheckpointedAt.get() < System.nanoTime() - checkpointInterval.toNanos

  private def checkpoint(checkpointer: IRecordProcessorCheckpointer) = {
    /* Store our latest position in the stream */
    checkpointer.checkpoint()

    /* Reset the counters */
    lastCheckpointedAt.set(System.nanoTime())
    recordsProcessedSinceCheckpoint.set(0)
  }

  /* This method may be called by KCL, e.g. in case of shard splits/merges */
  override def shutdown(checkpointer: IRecordProcessorCheckpointer, reason: ShutdownReason): Unit = {
    if (reason == ShutdownReason.TERMINATE) {
      checkpointer.checkpoint()
    }
    logger.info(s"Shutdown event processor for shard $shardId because $reason")
  }

}

trait SingleEventProcessor[EventT <: ThriftStruct] extends EventProcessor[EventT] {

  override protected def processEvents(events: Seq[EventWithSize[EventT]]) = events foreach processEvent
  protected def processEvent(eventWithSize: EventWithSize[EventT]): Unit

}

object EventProcessor {

  case class EventWithSize[EventT <: ThriftStruct](event: EventT, eventSize: Int)
}
