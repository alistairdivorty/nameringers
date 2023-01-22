import Dependencies._

ThisBuild / scalaVersion := "2.12.17"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.nameringers"
ThisBuild / organizationName := "nameringers"

lazy val root = (project in file("."))
    .settings(
      name := "spark",
      Compile / run / fork := true,
      libraryDependencies += scalaTest % Test
    )

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.12" % "3.3.1" % "provided",
  "org.apache.spark" % "spark-sql_2.12" % "3.3.1" % "provided",
  "org.apache.spark" % "spark-mllib_2.12" % "3.3.1" % "provided",
  "org.apache.hadoop" % "hadoop-aws" % "3.3.2" % "provided",
  "ml.combust.mleap" % "mleap-spark_2.12" % "0.21.1",
  "ml.combust.bundle" % "bundle-ml_2.12" % "0.21.1",
  scalaTest % Test
)

ThisBuild / assemblyMergeStrategy := {
    case PathList("META-INF", _*)            => MergeStrategy.discard
    case n if n.startsWith("reference.conf") => MergeStrategy.concat
    case _                                   => MergeStrategy.first
}

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
