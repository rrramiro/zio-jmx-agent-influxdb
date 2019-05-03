package fr.ramiro.influxdbclient

import Parameter.{DatabaseName, Epoch, Query}
import zio._
import fr.ramiro.influxdbclient.operations._

object InfluxDB {

  def connect(config: SttpConfig = SttpConfig()): InfluxDB =
    new InfluxDB(new SttpClient(config))

  def connectWithDatabase(config: SttpConfigWithDatabase): InfluxDBSelected =
    connect(config.base).selectDatabase(config.database)

  def udpConnect(host: String, port: Int) = new UdpClient(host, port)

}

case class InfluxDB protected[influxdbclient] (sttpClient: SttpClient)
    extends UserOperations
    with BasicOperations
    with QueryOperations
    with AutoCloseable {

  def selectDatabase(databaseName: String) =
    new InfluxDBSelected(databaseName, sttpClient)

  def exec(query: String): Task[NamedQueryResult] =
    sttpClient
      .post("query", "", defaultParameters :+ Query(query): _*)
      .flatMap(response => QueryResult.fromJson(response.content))
      .mapError { error =>
        QueryException("Error during query", error)
      }

  protected def executeQuery(query: String, epoch: Option[Epoch]) =
    sttpClient
      .get(
        "query",
        defaultParameters ++ Seq(Query(query), epoch: Parameter): _*
      )
      .mapError { error =>
        QueryException("Error during query", error)
      }

  protected def defaultParameters: Seq[Parameter] = Seq.empty

  def ping() =
    sttpClient
      .get("ping")
      .map(_ => true)
      .mapError { error =>
        PingException("Error during ping", error)
      }

  def isClosed: Boolean = sttpClient.isClosed

  def close(): Unit = sttpClient.close()
}

class InfluxDBSelected protected[influxdbclient] (
    val databaseName: String,
    sttpClient: SttpClient
) extends InfluxDB(sttpClient)
    with RetentionPolicyOperations
    with DatabaseOperations
    with WriteOperations {

  override protected def defaultParameters: Seq[Parameter] =
    super.defaultParameters :+ DatabaseName(databaseName)

}

case class PingException(str: String, throwable: Throwable)
    extends InfluxDBException(str, throwable)
