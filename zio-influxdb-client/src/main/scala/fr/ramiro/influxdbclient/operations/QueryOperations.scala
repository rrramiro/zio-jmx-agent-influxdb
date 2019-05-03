package fr.ramiro.influxdbclient.operations

import fr.ramiro.influxdbclient.Parameter.Epoch
import fr.ramiro.influxdbclient.{InfluxDB, NamedQueryResult, QueryResult}
import fr.ramiro.influxdbclient.{InfluxDB, NamedQueryResult, QueryResult}
import fr.ramiro.influxdbclient.Parameter.Epoch
import zio.Task

protected[influxdbclient] trait QueryOperations { self: InfluxDB =>

  def query(
      query: String,
      epoch: Option[Epoch] = None
  ): Task[NamedQueryResult] =
    executeQuery(query, epoch)
      .flatMap(response => QueryResult.fromJson(response.content))

  def multiQuery(
      query: Seq[String],
      epoch: Option[Epoch] = None
  ): Task[List[NamedQueryResult]] =
    executeQuery(query.mkString(";"), epoch)
      .flatMap(response => QueryResult.fromJsonMulti(response.content))

}

abstract class InfluxDBException(str: String, throwable: Throwable)
    extends Exception(str, throwable)
case class QueryException(str: String, throwable: Throwable)
    extends InfluxDBException(str, throwable)
