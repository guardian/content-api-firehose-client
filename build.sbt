import sbtrelease.ReleaseStateTransformations._
import sbtversionpolicy.withsbtrelease.ReleaseVersion

name:= "content-api-firehose-client"
organization := "com.gu"
scalaVersion := "2.12.19"
crossScalaVersions := Seq(scalaVersion.value, "2.13.13")
scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-Xfatal-warnings", "-release:11")
Compile / doc / scalacOptions  := Nil

releaseCrossBuild := true

enablePlugins(plugins.JUnitXmlReportPlugin)
Test / testOptions ++= Seq( Tests.Argument("-u", sys.env.getOrElse("SBT_JUNIT_OUTPUT","/tmp")) )

organization := "com.gu"
licenses := Seq(License.Apache2)

releaseCrossBuild := true

releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)

resolvers += "Guardian GitHub Repository" at "https://guardian.github.io/maven/repo-releases"
resolvers ++= Resolver.sonatypeOssRepos("releases")

libraryDependencies ++= Seq(
  "com.gu" %% "content-api-models-scala" % "25.0.0",
  "com.gu" %% "thrift-serializer" % "5.0.5",
  "software.amazon.kinesis" % "amazon-kinesis-client" % "2.6.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.twitter" %% "scrooge-core" % "21.12.0")

val jacksonVersion = "2.17.2"
dependencyOverrides ++= Seq(
  "com.charleskorn.kaml" % "kaml" % "0.53.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "software.amazon.awssdk" % "netty-nio-client" % "2.26.25",
  "org.json" % "json" % "20231013",
  "org.xerial.snappy" % "snappy-java" % "1.1.10.4",
  "org.apache.commons" % "commons-compress" % "1.26.0",
  "com.amazon.ion" % "ion-java" % "1.10.5",//overriding until a version of amazon-kinesis-client is available that removes the ion-java vulnerability
  "software.amazon.glue" % "schema-registry-serde" % "1.1.19", //overriding until a version of amazon-kinesis-client is available that removes the ion-java vulnerability
  "org.apache.avro" % "avro" % "1.11.4", //overriding until a version of amazon-kinesis-client / glue-schema-registry-common is available that removes the avro vuln
  "com.google.protobuf" % "protobuf-java" % "3.25.5"  //overriding until a version of amazon-kinesis-client is available that removes the protobuf vuln
)
