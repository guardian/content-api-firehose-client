package com.gu.contentapi.firehose.kinesis

import com.amazonaws.auth.AWSCredentialsProvider
import scala.concurrent.duration._

case class KinesisStreamReaderConfig(
    streamName: String,
    app: String,
    stage: String,
    mode: String,
    suffix: Option[String],
    kinesisCredentialsProvider: AWSCredentialsProvider,
    dynamoCredentialsProvider: AWSCredentialsProvider,
    awsRegion: String,
    checkpointInterval: Duration = 30.second,
    maxCheckpointBatchSize: Int = 20,
    maxRecords: Int = 10000,
    idleTimeBetweenReadsInMillis: Long = 2000L

) {

  lazy val applicationName: String = s"${streamName}_${app}-${mode}-${stage.toUpperCase}${suffix.getOrElse("")}"

}
