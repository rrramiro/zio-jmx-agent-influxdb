package fr.ramiro.influxdbclient.operations

import fr.ramiro.influxdbclient.{IString, InfluxDB}
import fr.ramiro.influxdbclient.{IString, InfluxDB}

protected[influxdbclient] trait BasicOperations { self: InfluxDB =>
  def showDatabases() =
    query("SHOW DATABASES")
      .map(
        response =>
          response.series.head.points("name").collect {
            case IString(name) => name
          }
      )

  def showMeasurements() =
    query("SHOW MEASUREMENTS")
      .map(
        response =>
          response.series.head.points("name").collect {
            case IString(name) => name
          }
      )

}
