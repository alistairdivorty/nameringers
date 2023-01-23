import Dependencies._

ThisBuild / scalaVersion := "2.12.17"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.nameringers"
ThisBuild / organizationName := "nameringers"

lazy val root = (project in file("."))
    .settings(
      name := "weaviate-client",
      libraryDependencies += scalaTest % Test
    )

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.0",
  "com.softwaremill.sttp.client3" %% "core" % "3.8.8",
  "ml.combust.mleap" % "mleap-runtime_2.12" % "0.21.1",
  "ml.combust.mleap" % "mleap-core_2.12" % "0.21.1"
)

ThisBuild / assemblyMergeStrategy := {
    case PathList("META-INF", _*)            => MergeStrategy.discard
    case n if n.startsWith("reference.conf") => MergeStrategy.concat
    case _                                   => MergeStrategy.first
}

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
