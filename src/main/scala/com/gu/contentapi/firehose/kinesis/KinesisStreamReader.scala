package com.gu.contentapi.firehose.kinesis

import java.util.concurrent.Executors
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.UUID
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{ InitialPositionInStream, KinesisClientLibConfiguration, Worker }
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory

trait KinesisStreamReader {

  val kinesisStreamReaderConfig: KinesisStreamReaderConfig
  protected val eventProcessorFactory: IRecordProcessorFactory

  /* only applies when there are no checkpoints */
  val initialPosition = InitialPositionInStream.TRIM_HORIZON

  /* Unique ID for the worker thread */
  private val workerId = UUID.randomUUID().toString

  private lazy val config =
    new KinesisClientLibConfiguration(
      kinesisStreamReaderConfig.applicationName,
      kinesisStreamReaderConfig.streamName,
      kinesisStreamReaderConfig.kinesisCredentialsProvider,
      kinesisStreamReaderConfig.dynamoCredentialsProvider,
      null,
      workerId
    )
      .withInitialPositionInStream(initialPosition)
      .withRegionName(kinesisStreamReaderConfig.awsRegion)
      .withMaxRecords(kinesisStreamReaderConfig.maxRecords)
      .withIdleTimeBetweenReadsInMillis(kinesisStreamReaderConfig.idleTimeBetweenReadsInMillis)

  /* Create a worker, which will in turn create one or more EventProcessors */
  lazy val worker = new Worker(
    eventProcessorFactory,
    config,
    new NullMetricsFactory(), // don't send metrics to CloudWatch because it's expensive and not very helpful
    threadPool
  )

  private lazy val threadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(s"${getClass.getSimpleName}-$workerId-thread-%d").build())

  /* Start the worker in a new thread. It will run forever */
  private lazy val workerThread =
    new Thread(worker, s"${getClass.getSimpleName}-$workerId")

  def start(): Thread = {
    workerThread.start()
    workerThread
  }

  def shutdown(): Unit = {
    worker.shutdown()
    threadPool.shutdown()
  }

}
