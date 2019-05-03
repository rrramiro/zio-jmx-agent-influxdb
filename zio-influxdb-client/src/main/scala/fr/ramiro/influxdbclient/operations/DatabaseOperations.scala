package fr.ramiro.influxdbclient.operations

import fr.ramiro.influxdbclient.InfluxDBSelected
import fr.ramiro.influxdbclient.InfluxDBSelected

protected[influxdbclient] trait DatabaseOperations { self: InfluxDBSelected =>

  def create() =
    exec(s"""CREATE DATABASE "$databaseName"""")

  def drop() =
    exec(s"""DROP DATABASE "$databaseName"""")

  def exists() =
    showDatabases().map(_.contains(databaseName))

}
