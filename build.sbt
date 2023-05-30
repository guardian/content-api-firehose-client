import sbtrelease.ReleaseStateTransformations._

name:= "content-api-firehose-client"
organization := "com.gu"
scalaVersion := "2.12.17"
crossScalaVersions := Seq(scalaVersion.value, "2.13.10")
scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-Xfatal-warnings")
Compile / doc / scalacOptions  := Nil

releaseCrossBuild := true

enablePlugins(plugins.JUnitXmlReportPlugin)
Test / testOptions ++= Seq( Tests.Argument("-u", sys.env.getOrElse("SBT_JUNIT_OUTPUT","/tmp")) )

pomExtra := (
<url>https://github.com/guardian/content-api-firehose-client</url>
  <developers>
    <developer>
      <id>LATaylor-guardian</id>
      <name>Luke Taylor</name>
      <url>https://github.com/LATaylor-guardian</url>
    </developer>
  </developers>
)
Test / publishArtifact  := false
releasePublishArtifactsAction := PgpKeys.publishSigned.value
organization := "com.gu"
licenses := Seq("Apache v2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scmInfo := Some(ScmInfo(
  url("https://github.com/guardian/content-api-firehose-client"),
  "scm:git:git@github.com:guardian/content-api-firehose-client.git"
))

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeOssSnapshots.head
  else
    Opts.resolver.sonatypeStaging
)

ThisBuild / publishMavenStyle := true
ThisBuild / pomIncludeRepository := { _ => false }

releaseCrossBuild := true
releasePublishArtifactsAction := PgpKeys.publishSigned.value
releaseProcess := Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

resolvers += "Guardian GitHub Repository" at "https://guardian.github.io/maven/repo-releases"
resolvers ++= Resolver.sonatypeOssRepos("snapshots")

libraryDependencies ++= Seq(
  "com.gu" %% "content-api-models-scala" % "17.5.1",
  "com.gu" %% "thrift-serializer" % "5.0.2",
  "software.amazon.kinesis" % "amazon-kinesis-client" % "2.4.8",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.twitter" %% "scrooge-core" % "21.1.0")

val jacksonVersion = "2.12.7"
dependencyOverrides ++= Seq(
  "com.charleskorn.kaml" % "kaml" % "0.53.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.7.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "software.amazon.awssdk" % "netty-nio-client" % "2.20.26",
)