package com.gu.contentapi.firehose.kinesis

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient

import java.net.URI
import scala.concurrent.duration._

case class KinesisStreamReaderConfig(
  streamName: String,
  app: String,
  stage: String,
  mode: String,
  suffix: Option[String],
  kinesisCredentialsProvider: AwsCredentialsProvider,
  dynamoCredentialsProvider: AwsCredentialsProvider,
  awsRegion: String,
  checkpointInterval: Duration = 30.second,
  maxCheckpointBatchSize: Int = 20,
  maxRecords: Int = 10000,
  idleTimeBetweenReadsInMillis: Long = 2000L,
  endpointOverride: Option[URI] = None) {

  lazy val applicationName: String = s"${streamName}_${app}-${mode}-${stage.toUpperCase}${suffix.getOrElse("")}"

  def DynamoClientBuilder() = {
    val b = DynamoDbAsyncClient.builder()
    endpointOverride match {
      case Some(endpoint) => b.endpointOverride(endpoint)
      case None => b
    }
  }

  def KinesisClientBuilder() = {
    val b = KinesisAsyncClient.builder()
    endpointOverride match {
      case Some(endpoint) => b.endpointOverride(endpoint)
      case None => b
    }
  }

  def CloudWatchClientBuilder() = {
    val b = CloudWatchAsyncClient.builder()
    endpointOverride match {
      case Some(endpoint) => b.endpointOverride(endpoint)
      case None => b
    }
  }
}
