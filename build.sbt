import Dependencies._

ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val akkaVersion = "2.7.0"
lazy val alpakkaKafkaVersion = "4.0.2"

lazy val root = (project in file("."))
  .settings(
    name := "demo",
    Compile / mainClass := Some("example.Demo"),

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka" % alpakkaKafkaVersion,
      "org.apache.kafka" % "kafka-clients" % "3.2.3",
      "ch.qos.logback" % "logback-classic" % "1.2.10",
      munit % Test
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
