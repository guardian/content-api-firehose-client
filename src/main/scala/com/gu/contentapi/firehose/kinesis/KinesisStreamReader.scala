package com.gu.contentapi.firehose.kinesis

import java.util.concurrent.Executors

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.UUID
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{ Worker, KinesisClientLibConfiguration, InitialPositionInStream }
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory

trait KinesisStreamReader {

  val streamName: String
  val app: String
  val stage: String
  val mode: String
  val suffix: Option[String]

  val kinesisCredentialsProvider: AWSCredentialsProvider
  val dynamoCredentialsProvider: AWSCredentialsProvider
  val awsRegion: String

  lazy val applicationName: String = s"${streamName}_${app}-${mode}-${stage.toUpperCase}${suffix.getOrElse("")}"

  /* only applies when there are no checkpoints */
  val initialPosition = InitialPositionInStream.TRIM_HORIZON

  /* Unique ID for the worker thread */
  private val workerId = UUID.randomUUID().toString

  private lazy val config =
    new KinesisClientLibConfiguration(applicationName, streamName, kinesisCredentialsProvider, dynamoCredentialsProvider, null, workerId)
      .withInitialPositionInStream(initialPosition)
      .withRegionName(awsRegion)

  protected val eventProcessorFactory: IRecordProcessorFactory

  /* Create a worker, which will in turn create one or more EventProcessors */
  lazy val worker = new Worker(
    eventProcessorFactory,
    config,
    new NullMetricsFactory(), // don't send metrics to CloudWatch because it's expensive and not very helpful
    Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(s"${getClass.getSimpleName}-$workerId-thread-%d").build())
  )

  /* Start the worker in a new thread. It will run forever */
  private lazy val workerThread =
    new Thread(worker, s"${getClass.getSimpleName}-$workerId")

  def start() = workerThread.start()

}
