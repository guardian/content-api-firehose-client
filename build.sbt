import sbtrelease.ReleaseStateTransformations._

name:= "content-api-firehose-client"
organization := "com.gu"
scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.11.12", scalaVersion.value)
scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked", "-target:jvm-1.8", "-Xfatal-warnings")
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

resolvers += "Guardian GitHub Repository" at "http://guardian.github.io/maven/repo-releases"

libraryDependencies ++= Seq(
  "com.gu" %% "content-api-models-scala" % "14.1",
  "com.gu" %% "thrift-serializer" % "4.0.0",
  "com.amazonaws" % "amazon-kinesis-client" % "1.9.1",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.378",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "com.twitter" %% "scrooge-core" % "19.3.0")

initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}