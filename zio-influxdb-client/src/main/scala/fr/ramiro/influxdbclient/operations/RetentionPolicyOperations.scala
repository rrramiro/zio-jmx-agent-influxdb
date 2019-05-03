package fr.ramiro.influxdbclient.operations

import fr.ramiro.influxdbclient.InfluxDBSelected
import zio.IO

protected[influxdbclient] trait RetentionPolicyOperations {
  self: InfluxDBSelected =>
  def createRetentionPolicy(
      name: String,
      duration: String,
      replication: Int,
      default: Boolean
  ) = query(
    s"""CREATE RETENTION POLICY "$name" ON "$databaseName" DURATION $duration ${replicationStr(
         replication
       )} ${defaultStr(default)}""".stripMargin
  )

  def showRetentionPolicies() =
    query(s"""SHOW RETENTION POLICIES ON "$databaseName"""")

  def dropRetentionPolicy(name: String) =
    exec(s"""DROP RETENTION POLICY "$name" ON "$databaseName"""")

  def alterRetentionPolicy(
      name: String,
      duration: Option[String] = None,
      replication: Int = -1,
      default: Boolean = false
  ) = {
    if (duration.isEmpty && replication == -1 && !default)
      IO.fail(
        new InvalidRetentionPolicyParametersException(
          "At least one parameter has to be set"
        )
      )
    else {
      query(s"""ALTER RETENTION POLICY "$name" ON "$databaseName" ${durationStr(
        duration
      )} ${replicationStr(replication)} ${defaultStr(default)}""")
    }
  }

  private def defaultStr(b: Boolean): String = if (b) "DEFAULT" else ""
  private def replicationStr(i: Int): String =
    if (i > -1) s"REPLICATION $i" else ""
  private def durationStr(o: Option[String]) =
    o.map("DURATION " + _).getOrElse("")
}

class InvalidRetentionPolicyParametersException(str: String)
    extends Exception(str)
