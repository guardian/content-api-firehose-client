import sbtrelease.ReleaseStateTransformations._
import sbtversionpolicy.withsbtrelease.ReleaseVersion

name:= "content-api-firehose-client"
organization := "com.gu"
scalaVersion := "2.12.18"
crossScalaVersions := Seq(scalaVersion.value, "2.13.12")
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
  "com.gu" %% "content-api-models-scala" % "17.5.1",
  "com.gu" %% "thrift-serializer" % "5.0.2",
  "software.amazon.kinesis" % "amazon-kinesis-client" % "2.5.2",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.twitter" %% "scrooge-core" % "21.1.0")

val jacksonVersion = "2.12.7"
dependencyOverrides ++= Seq(
  "com.charleskorn.kaml" % "kaml" % "0.53.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.7.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "software.amazon.awssdk" % "netty-nio-client" % "2.20.26",
  "org.json" % "json" % "20231013",
  "io.netty" % "netty-handler" % "4.1.94.Final",      // SNYK-JAVA-IONETTY-5725787
  "io.netty" % "netty-codec-http2" % "4.1.100.Final", // SNYK-JAVA-IONETTY-5953332
  "org.xerial.snappy" % "snappy-java" % "1.1.10.4",
  "org.apache.commons" % "commons-compress" % "1.26.0",
  "com.amazon.ion" % "ion-java" % "1.10.5"
)