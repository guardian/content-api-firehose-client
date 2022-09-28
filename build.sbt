import sbtrelease.ReleaseStateTransformations._

name:= "content-api-firehose-client"
organization := "com.gu"
scalaVersion := "2.12.17"
crossScalaVersions := Seq(scalaVersion.value, "2.13.9")
scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-release:8", "-Xfatal-warnings")
Compile / doc / scalacOptions  := Nil

releaseCrossBuild := true

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
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

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

libraryDependencies ++= Seq(
  "com.gu" %% "content-api-models-scala" % "17.3.0",
  "com.gu" %% "thrift-serializer" % "5.0.0-SNAPSHOT",
  "software.amazon.kinesis" % "amazon-kinesis-client" % "2.4.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.twitter" %% "scrooge-core" % "21.1.0")
