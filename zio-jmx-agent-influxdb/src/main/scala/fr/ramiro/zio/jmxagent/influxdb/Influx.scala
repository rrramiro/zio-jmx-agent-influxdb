package fr.ramiro.zio.jmxagent.influxdb

import fr.ramiro.influxdbclient.{InfluxDB, InfluxDBSelected}
import fr.ramiro.influxdbclient.{
  InfluxDB,
  InfluxDBSelected,
  SttpConfigWithDatabase
}
import zio.{IO, ZIO}

object Influx {
  def withInflux[Z, E, T](
      influxdbConfig: SttpConfigWithDatabase
  )(use: InfluxDBSelected => ZIO[Z, E, T]) =
    IO(InfluxDB.connectWithDatabase(influxdbConfig))
      .bracket(c => IO.succeed(c.close()))(use)
}
