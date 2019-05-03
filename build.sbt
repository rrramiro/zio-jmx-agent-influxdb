import sbt.Package.ManifestAttributes

lazy val zioVersion = "1.0.0-RC16"
lazy val json4sVersion = "3.6.7"
lazy val scalatestVersion = "3.0.8"

ThisBuild / organization := "fr.ramiro"

ThisBuild / scalaVersion := "2.13.1"

ThisBuild / scalafmtOnCompile := true

lazy val root = (project in file("."))
.aggregate(zioJson4s, zioInfluxdbClient, zioJmxAgentInfluxdb, zioPureconfig)

lazy val zioJmxAgentInfluxdb = (project in file("zio-jmx-agent-influxdb"))
.settings(
  packageOptions := Seq(ManifestAttributes(
    "Premain-Class" -> "fr.ramiro.zio.jmxagent.influxdb.ZioJmxAgent",
    "Agent-Class" -> "fr.ramiro.zio.jmxagent.influxdb.ZioJmxAgent"
  )),
  libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.scalatest" %% "scalatest" % scalatestVersion % "test"
  ),
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF","io.netty.versions.properties", xs @ _ *)  => MergeStrategy.first
    case PathList("scalaz", "zio","BuildInfo$.class", xs @ _ *) => MergeStrategy.discard
    case x => (assemblyMergeStrategy in assembly).value(x)
  }
).dependsOn(zioInfluxdbClient, zioPureconfig)

lazy val zioInfluxdbClient = (project in file("zio-influxdb-client"))
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp" %% "async-http-client-backend-zio" % "1.7.2",
      "org.typelevel" %% "cats-core" % "2.0.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
      "org.scalatest" %% "scalatest" % scalatestVersion % "test",
      "com.github.tomakehurst" % "wiremock" % "2.16.0" % "test"
    )
  )
  .dependsOn(zioJson4s)

lazy val zioJson4s = (project in file("zio-json4s"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "org.json4s" %% "json4s-core" % json4sVersion,
      "org.json4s" %% "json4s-native" % json4sVersion,
      "org.specs2" %% "specs2-scalacheck" % "4.5.1" % "test"
    )
  )

lazy val zioPureconfig = (project in file("zio-pureconfig")).settings(
  libraryDependencies ++= Seq(
    "dev.zio" %% "zio" % zioVersion,
    "com.github.pureconfig" %% "pureconfig" % "0.12.1"
  )
)
