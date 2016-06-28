name:= "content-api-firehose-client"
organization := "com.gu"
description := "The content api firehose client - A scala client for consuming from the content api events stream."
scalaVersion := "2.11.8"
scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-target:jvm-1.8", "-Xfatal-warnings")
scalacOptions in doc in Compile := Nil

scroogeThriftDependencies in Compile := Seq("content-api-models", "story-packages-model-thrift", "content-atom-model-thrift")
resolvers += "Guardian GitHub Repository" at "http://guardian.github.io/maven/repo-releases"

libraryDependencies ++= Seq(
  "com.gu" %% "content-api-client" % "9.0",
  "com.amazonaws" % "amazon-kinesis-client" % "1.6.3",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.11",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.apache.thrift" % "libthrift" % "0.9.1",
  "com.twitter" %% "scrooge-core" % "4.6.0")


// See: https://github.com/twitter/scrooge/issues/199
scroogeThriftSources in Compile ++= {
  (scroogeUnpackDeps in Compile).value.flatMap { dir => (dir ** "*.thrift").get }
}

initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}