package fr.ramiro.influxdbclient

import org.json4s._
import org.json4s.zio.JsonScalazZio._
import native.JsonMethods._
import _root_.zio.{IO, Task}

trait InfluxValue
case class IString(value: String) extends InfluxValue
case class IInt(value: BigInt) extends InfluxValue
case class IBoolean(value: Boolean) extends InfluxValue
case object INull extends InfluxValue

case class IRecord(
    name: String,
    columns: List[String],
    values: List[List[InfluxValue]]
)
case class ISeries(records: List[IRecord], statementId: Option[Int] = None)
case class IQueryResult(results: List[ISeries])

case class NamedRecord(
    namesIndex: Map[String, Int],
    values: List[InfluxValue]
) {
  def apply(position: Int): InfluxValue = values(position)
  def apply(name: String): InfluxValue = values(namesIndex(name))
}

case class NamedTagSet(tagsIndex: Map[String, Int], values: List[InfluxValue]) {
  def apply(position: Int): InfluxValue = values(position)
  def apply(name: String): InfluxValue = values(tagsIndex(name))
}

case class NamedSeries(
    name: String,
    columns: List[String],
    records: List[NamedRecord],
    tags: NamedTagSet
) {
  def points(column: String): List[InfluxValue] = records.map(_(column))
  def points(column: Int): List[InfluxValue] = records.map(_(column))
  def values: List[List[InfluxValue]] = records.map(_.values)
}

case class NamedQueryResult(series: List[NamedSeries] = List.empty)

object QueryResult {

  def fromJson(data: String): Task[NamedQueryResult] = {
    for {
      resultsArray <- parseJson(data)
      r <- resultsArray.arr.headOption.fold(
        IO.fail(UncategorizedError("fromJson", " missing results")): Result[
          NamedQueryResult
        ]
      )(makeSingleResult)
    } yield r
  }.mapError {
    case UncategorizedError("makeSingleResult", error) =>
      ErrorResponseException(error)
    case error => MalformedResponseException(error.toString)
  }

  def fromJsonMulti(data: String): Task[List[NamedQueryResult]] = {
    for {
      resultsArray <- parseJson(data)
      r <- IO.collectAll(resultsArray.arr.map(makeSingleResult))
    } yield r
  }.mapError {
    case UncategorizedError("makeMultiResult", error) =>
      ErrorResponseException(error)
    case error => MalformedResponseException(error.toString)
  }

  private def makeSingleResult(
      resultObject: JValue
  ): Result[NamedQueryResult] = {
    for {
      seriesArray <- field[Option[JArray]]("series")(resultObject)
        .map(_.getOrElse(JArray.apply(List.empty)))
      series <- IO.collectAll(seriesArray.arr.map(constructSeries))
      _ <- field[Option[String]]("error")(resultObject).flatMap {
        case Some(error) =>
          IO.fail(UncategorizedError("makeSingleResult", error))
        case None => IO.succeed(())
      }
    } yield NamedQueryResult(series)
  }

  implicit val jArrayJSON: JSONR[JArray] = {
    case a: JArray => IO.succeed(a)
    case x         => IO.fail(UnexpectedJSONError(x, classOf[JArray]))
  }

  private def parseJson(data: String): Result[JArray] = {
    IO.effect(parse(data))
      .mapError(e => UncategorizedError("parseJson", e.getMessage))
      .flatMap(field[JArray]("results"))
  }

  implicit def influxValueJSON: JSONR[InfluxValue] = {
    case JInt(num)      => IO.succeed(IInt(num))
    case JString(str)   => IO.succeed(IString(str))
    case JBool(boolean) => IO.succeed(IBoolean(boolean))
    case JNull          => IO.succeed(INull)
    case x =>
      IO.fail(UncategorizedError("influxValueJSON", s"Found invalid type: $x"))
  }

  protected[influxdbclient] def constructSeries(
      value: JValue
  ): Result[NamedSeries] = {
    for {
      seriesName <- field[Option[String]]("name")(value).map(_.getOrElse(""))
      columns <- field[List[String]]("columns")(value)
      namesIndex = columns.zipWithIndex.toMap
      recordsIO <- field[Option[JArray]]("values")(value).map(
        _.map(_.arr.map(constructRecord(namesIndex, _))).getOrElse(List.empty)
      )
      records <- IO.collectAll(recordsIO)
      tagsMap <- field[Option[Map[String, InfluxValue]]]("tags")(value)
        .map(_.getOrElse(Map.empty))
      tags = NamedTagSet(
        tagsMap.keySet.zipWithIndex.toMap,
        tagsMap.values.toList
      )
    } yield NamedSeries(seriesName, columns, records, tags)
  }

  protected[influxdbclient] def constructRecord(
      namesIndex: Map[String, Int],
      value: JValue
  ): Result[NamedRecord] = {
    implicitly[JSONR[List[InfluxValue]]]
      .read(value)
      .map(NamedRecord(namesIndex, _))
  }
}

abstract class QueryResultException(
    message: String,
    throwable: Throwable = null
) extends Exception(message, throwable)

case class MalformedResponseException(
    message: String,
    throwable: Throwable = null
) extends QueryResultException(message, throwable)

case class ErrorResponseException(
    message: String,
    throwable: Throwable = null
) extends QueryResultException(message, throwable)
