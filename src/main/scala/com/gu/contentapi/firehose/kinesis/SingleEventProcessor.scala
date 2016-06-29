package com.gu.contentapi.firehose.kinesis

import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{ IRecordProcessor, IRecordProcessorCheckpointer }
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.typesafe.scalalogging.LazyLogging
import com.twitter.scrooge.ThriftStruct
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }
import java.util.{ List => JList }
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
    val events = records.asScala.flatMap { record =>
      val buffer = record.getData
      val op = deserializeEvent(buffer)
      op match {
        case Success(event) => Some(event)
        case Failure(e) => {
          logger.error(s"deserialization of event buffer failed: ${e.getMessage}", e)
          None
        }
      }
    }

    processEvents(events)

    /* increment the record counter */
    recordsProcessedSinceCheckpoint.addAndGet(events.size)

    if (shouldCheckpointNow) {
      checkpoint(checkpointer)
    }
  }

  protected def processEvents(events: Seq[EventT]): Unit

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

  override protected def processEvents(events: Seq[EventT]) = events foreach processEvent
  protected def processEvent(eventWithSize: EventT): Unit

}
