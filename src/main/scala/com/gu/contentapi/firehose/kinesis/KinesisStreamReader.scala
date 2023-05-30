package com.gu.contentapi.firehose.kinesis

import java.util.concurrent.Executors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import java.util.UUID
import software.amazon.kinesis.common.{ ConfigsBuilder, InitialPositionInStream, KinesisClientUtil }
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.processor.ShardRecordProcessorFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

trait KinesisStreamReader {
  val credentialsProvider: AwsCredentialsProvider
  val kinesisStreamReaderConfig: KinesisStreamReaderConfig
  protected val eventProcessorFactory: ShardRecordProcessorFactory

  /* only applies when there are no checkpoints */
  val initialPosition = InitialPositionInStream.TRIM_HORIZON

  /* Unique ID for the worker thread */
  private val workerId = UUID.randomUUID().toString

  private val kinesisClient = KinesisClientUtil.createKinesisAsyncClient(
    kinesisStreamReaderConfig.KinesisClientBuilder()
      .credentialsProvider(credentialsProvider)
      .region(Region.of(kinesisStreamReaderConfig.awsRegion)))

  private val dynamoClient = kinesisStreamReaderConfig.DynamoClientBuilder()
    .credentialsProvider(credentialsProvider)
    .region(Region.of(kinesisStreamReaderConfig.awsRegion))
    .build()

  private val cwClient = kinesisStreamReaderConfig.CloudWatchClientBuilder()
    .credentialsProvider(credentialsProvider)
    .region(Region.of(kinesisStreamReaderConfig.awsRegion))
    .build()

  private val configsBuilder = new ConfigsBuilder(
    kinesisStreamReaderConfig.streamName,
    kinesisStreamReaderConfig.applicationName,
    kinesisClient,
    dynamoClient,
    cwClient,
    workerId,
    eventProcessorFactory)

  lazy val scheduler: Scheduler = new Scheduler(
    configsBuilder.checkpointConfig(),
    configsBuilder.coordinatorConfig(),
    configsBuilder.leaseManagementConfig(),
    configsBuilder.lifecycleConfig(),
    configsBuilder.metricsConfig(),
    configsBuilder.processorConfig(),
    configsBuilder.retrievalConfig())

  private lazy val threadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(s"${getClass.getSimpleName}-$workerId-thread-%d").build())

  /* Start the worker in a new thread. It will run forever */
  private lazy val workerThread =
    new Thread(scheduler, s"${getClass.getSimpleName}-$workerId")

  def start(): Thread = {
    workerThread.start()
    workerThread
  }

  def shutdown(): Unit = {
    scheduler.shutdown()
    threadPool.shutdown()
  }

}
