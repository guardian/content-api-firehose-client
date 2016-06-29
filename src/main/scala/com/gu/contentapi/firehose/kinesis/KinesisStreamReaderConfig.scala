package com.gu.contentapi.firehose.kinesis

import com.amazonaws.auth.AWSCredentialsProvider

case class KinesisStreamReaderConfig(
    streamName: String,
    app: String,
    stage: String,
    mode: String,
    suffix: Option[String],
    kinesisCredentialsProvider: AWSCredentialsProvider,
    dynamoCredentialsProvider: AWSCredentialsProvider,
    awsRegion: String
) {

  lazy val applicationName: String = s"${streamName}_${app}-${mode}-${stage.toUpperCase}${suffix.getOrElse("")}"

}
