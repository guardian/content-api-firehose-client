import sbtrelease.ReleaseStateTransformations._
import sbtversionpolicy.withsbtrelease.ReleaseVersion

name:= "content-api-firehose-client"
organization := "com.gu"
scalaVersion := "2.12.20"
crossScalaVersions := Seq(scalaVersion.value, "2.13.18")
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
  "com.gu" %% "content-api-models-scala" % "32.0.0",
  "com.gu" %% "thrift-serializer" % "5.0.7",
  "software.amazon.kinesis" % "amazon-kinesis-client" % "3.2.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.twitter" %% "scrooge-core" % "21.12.0",
  "at.yawk.lz4" % "lz4-java" % "1.10.2", // https://github.com/advisories/GHSA-cmp6-m4wj-q63q
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
) ++ Seq("aws-json-protocol", "kinesis").map(artifact => "software.amazon.awssdk" % artifact % "2.29.47")

excludeDependencies ++= Seq(
  ExclusionRule(
    organization = "org.lz4", // https://github.com/advisories/GHSA-cmp6-m4wj-q63q
    name = "lz4-java"
  )
)

val jacksonVersion = "2.17.2"
dependencyOverrides ++= Seq(
  "com.charleskorn.kaml" % "kaml" % "0.53.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "org.json" % "json" % "20231013",
  "org.xerial.snappy" % "snappy-java" % "1.1.10.4",
  "org.apache.commons" % "commons-compress" % "1.26.0"
)
