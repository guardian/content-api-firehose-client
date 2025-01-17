package com.gu.contentapi.firehose.kinesis

import com.gu.thrift.serializer.ThriftDeserializer
import com.typesafe.scalalogging.LazyLogging
import com.twitter.scrooge.{ThriftStruct, ThriftStructCodec}
import software.amazon.kinesis.exceptions.{InvalidStateException, ShutdownException}
import software.amazon.kinesis.lifecycle.ShutdownReason
import software.amazon.kinesis.lifecycle.events.{InitializationInput, LeaseLostInput, ProcessRecordsInput, ShardEndedInput, ShutdownRequestedInput}
import software.amazon.kinesis.processor.{RecordProcessorCheckpointer, ShardRecordProcessor}

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.{Phaser, TimeoutException}
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import scala.jdk.CollectionConverters._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

abstract class EventProcessor[EventT <: ThriftStruct: ThriftStructCodec]
  extends ShardRecordProcessor
  with LazyLogging {

  private val ongoingProcessRecordCallsPhaser = new Phaser(1) // allow 1 'await' in shutdownRequested()
  val checkpointInterval: Duration
  val maxCheckpointBatchSize: Int

  private[this] var shardId: String = _

  /* Use atomic to prevent any concurrent access issues */
  private[this] val lastCheckpointedAt = new AtomicLong(System.nanoTime())
  private[this] val recordsProcessedSinceCheckpoint = new AtomicInteger()

  override def initialize(input: InitializationInput): Unit = {
    this.shardId = input.shardId()
    logger.info(s"Initialized an event processor for shard $shardId")
  }

  override def processRecords(input: ProcessRecordsInput): Unit = {
    ongoingProcessRecordCallsPhaser.register() // graceful shutdown should wait for us to complete this work
    val events = input.records().asScala.flatMap { record =>
      val buffer = record.data()
      val op = ThriftDeserializer.deserialize(buffer)
      op match {
        case Success(event) => Some(event)
        case Failure(e) => {
          logger.error(s"deserialization of event buffer failed: ${e.getMessage}", e)
          buffer.rewind()
          val encoded = Base64.getEncoder.encode(buffer)
          val b64string = new String(encoded.array(), StandardCharsets.ISO_8859_1)
          logger.error(s"Offending binary content: $b64string")
          None
        }
      }
    }.toSeq //.toSeq is required on Scala 2.13 as the comprehension above gives us a mutable.Buffer which is not directly compatible with Seq.

    processEvents(events)
    ongoingProcessRecordCallsPhaser.arriveAndDeregister() // it's no longer necessary for any shutdown to wait for us

    /* increment the record counter */
    recordsProcessedSinceCheckpoint.addAndGet(events.size)

    if (shouldCheckpointNow) {
      checkpoint(input.checkpointer())
    }
  }

  protected def processEvents(events: Seq[EventT]): Unit

  /* Checkpoint after every X seconds or every Y records */
  private def shouldCheckpointNow =
    recordsProcessedSinceCheckpoint.get() >= maxCheckpointBatchSize ||
      lastCheckpointedAt.get() < System.nanoTime() - checkpointInterval.toNanos

  private def checkpoint(checkpointer: RecordProcessorCheckpointer) = {
    /* Store our latest position in the stream */
    checkpointer.checkpoint()

    /* Reset the counters */
    lastCheckpointedAt.set(System.nanoTime())
    recordsProcessedSinceCheckpoint.set(0)
  }

  def leaseLost(leaseLostInput: LeaseLostInput): Unit = {
    logger.info(s"Shutdown event processor for shard $shardId because lease was lost")
    shutdown(ShutdownReason.LEASE_LOST)
  }

  def shardEnded(shardEndedInput: ShardEndedInput): Unit = {
    logger.info(s"Shutdown event processor for shard $shardId because the shard ended")
    shutdown(ShutdownReason.SHARD_END)
  }

  def shutdownRequested(shutdownRequestedInput: ShutdownRequestedInput): Unit = {
    try {
      val ongoingCalls = ongoingProcessRecordCallsPhaser.getUnarrivedParties - 1
      if (ongoingCalls > 0)
        logger.info(s"Shutdown requested but waiting for processRecords() to complete - ongoingCalls=$ongoingCalls")

      // Ensure that all records we've received have been processed before we call checkpoint and exit
      Try(ongoingProcessRecordCallsPhaser.awaitAdvanceInterruptibly(ongoingProcessRecordCallsPhaser.arrive(), 10, SECONDS)).recover {
        case e: TimeoutException => logger.error("Timeout while waiting for processRecords() to complete", e)
      }
      logger.info("Scheduler is shutting down, checkpointing.")
      shutdownRequestedInput.checkpointer().checkpoint()
    } catch {
      case e: ShutdownException | InvalidStateException =>
        logger.error("Exception while checkpointing at requested shutdown. Giving up.", e)
    }
    logger.info(s"Shutdown event processor for shard $shardId because shutdown was requested")
    shutdown(ShutdownReason.REQUESTED)
  }

  /**
   * Subclass this method if you want to be informed of shutdown events.  The default implementation does nothing.
   * @param reason ShutdownReason indicating why the shutdown occurred
   */
  def shutdown(reason: ShutdownReason): Unit = {}
}

trait SingleEventProcessor[EventT <: ThriftStruct] extends EventProcessor[EventT] {

  override protected def processEvents(events: Seq[EventT]) = events foreach processEvent
  protected def processEvent(eventWithSize: EventT): Unit

}
