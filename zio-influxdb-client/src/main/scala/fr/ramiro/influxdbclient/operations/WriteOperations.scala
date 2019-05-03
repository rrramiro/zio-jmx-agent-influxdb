package fr.ramiro.influxdbclient.operations

import fr.ramiro.influxdbclient.Parameter._
import fr.ramiro.influxdbclient.model.InfluxSerializable
import fr.ramiro.influxdbclient.{HttpException, InfluxDBSelected}
import fr.ramiro.influxdbclient.{HttpException, InfluxDBSelected}
import fr.ramiro.influxdbclient.Parameter.{
  Consistency,
  Precision,
  RetentionPolicy
}
import fr.ramiro.influxdbclient.model.InfluxSerializable
import zio.{IO, Task}

protected[influxdbclient] trait WriteOperations { self: InfluxDBSelected =>

  def write(
      point: InfluxSerializable,
      precision: Option[Precision] = None,
      consistency: Option[Consistency] = None,
      retentionPolicy: Option[RetentionPolicy] = None
  ): Task[Boolean] = {
    sttpClient
      .post(
        "write",
        point.serialize,
        DatabaseName(databaseName),
        precision,
        consistency,
        retentionPolicy
      )
      .mapError {
        case error: HttpException =>
          exceptionFromStatusCode(
            error.code,
            "Error during write: " + error.getMessage,
            Some(error)
          )
        case error => error
      }
      .flatMap { result =>
        if (result.code != 204)
          IO.fail(
            exceptionFromStatusCode(
              result.code,
              "Error during write: " + result.content
            )
          )
        else
          IO.succeed(true)
      }
  }

  private def exceptionFromStatusCode(
      statusCode: Int,
      str: String,
      throwable: Option[Throwable] = None
  ): WriteException =
    statusCode match {
      case 200 => new RequestNotCompletedException(str, throwable.orNull)
      case 404 => new DatabaseNotFoundException(str, throwable.orNull)
      case e if 400 <= e && e <= 499 =>
        new MalformedRequestException(str, throwable.orNull)
      case e if 500 <= e && e <= 599 =>
        new ServerUnavailableException(str, throwable.orNull)
      case _ => new UnknownErrorException(str, throwable.orNull)
    }

}

abstract class WriteException(str: String, throwable: Throwable)
    extends InfluxDBException(str, throwable)
class DatabaseNotFoundException(str: String, throwable: Throwable)
    extends WriteException(str, throwable)
class MalformedRequestException(str: String, throwable: Throwable)
    extends WriteException(str, throwable)
class RequestNotCompletedException(str: String, throwable: Throwable)
    extends WriteException(str, throwable)
class ServerUnavailableException(str: String, throwable: Throwable)
    extends WriteException(str, throwable)
class UnknownErrorException(str: String, throwable: Throwable)
    extends WriteException(str, throwable)
