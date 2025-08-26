import sbtrelease.ReleaseStateTransformations._
import sbtversionpolicy.withsbtrelease.ReleaseVersion

name:= "content-api-firehose-client"
organization := "com.gu"
scalaVersion := "2.12.20"
crossScalaVersions := Seq(scalaVersion.value, "2.13.15")
scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-Xfatal-warnings", "-release:11")
Compile / doc / scalacOptions  := Nil

enablePlugins(plugins.JUnitXmlReportPlugin)
Test / testOptions +=
  Tests.Argument(TestFrameworks.ScalaTest, "-u", s"test-results/scala-${scalaVersion.value}", "-o")

organization := "com.gu"
licenses := Seq(License.Apache2)

releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value
releaseCrossBuild := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)

resolvers ++= Resolver.sonatypeOssRepos("releases")

libraryDependencies ++= Seq(
  "com.gu" %% "content-api-models-scala" % "29.0.0",
  "com.gu" %% "thrift-serializer" % "5.0.7",
  "software.amazon.kinesis" % "amazon-kinesis-client" % "3.0.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.twitter" %% "scrooge-core" % "24.2.0",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
) ++ Seq("aws-json-protocol", "kinesis").map(artifact => "software.amazon.awssdk" % artifact % "2.29.47")

val jacksonVersion = "2.17.3"
dependencyOverrides ++= Seq(
  "com.charleskorn.kaml" % "kaml" % "0.67.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "org.json" % "json" % "20250107",
  "org.xerial.snappy" % "snappy-java" % "1.1.10.7",
  "org.apache.commons" % "commons-compress" % "1.26.2",
  "software.amazon.glue" % "schema-registry-serde" % "1.1.22", //overriding until a version of amazon-kinesis-client is available that removes the ion-java vulnerability
  "org.apache.avro" % "avro" % "1.12.0", //overriding until a version of amazon-kinesis-client / glue-schema-registry-common is available that removes the avro vuln
  "com.google.protobuf" % "protobuf-java" % "4.29.2"  //overriding until a version of amazon-kinesis-client is available that removes the protobuf vuln
)
