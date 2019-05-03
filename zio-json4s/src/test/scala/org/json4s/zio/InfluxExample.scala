package org.json4s
package zio

import JsonScalazZio.{Result => JsonResult, _}
import native.JsonMethods._
import org.specs2.mutable.Specification
import _root_.zio.{DefaultRuntime, IO, ZIO}

object InfluxExample extends Specification with DefaultRuntime {
  trait InfluxValue
  case class IString(value: String) extends InfluxValue
  case class IInt(value: Int) extends InfluxValue
  case class IBoolean(value: Boolean) extends InfluxValue
  case object INull extends InfluxValue

  case class Series(
      name: String,
      columns: List[String],
      values: List[List[InfluxValue]]
  )
  case class Result(series: List[Series], statementId: Option[Int] = None)
  case class ResultResponse(results: List[Result])

  val jsonError1 = """{"error":"retention policy not found: myrp"}"""
  val jsonError2 =
    """{"results":[{"series":[{}],"error":"...."}],"error":"...."}"""
  val json1 =
    """{"results":[{"series":[{"name":"databases","columns":["name"],"values":[["mydb"]]}]}]}"""
  val json2 =
    """{"results":[{"series":[{"name":"cpu","columns":["time","value"],"values":[["2015-06-06T14:55:27.195Z",90],["2015-06-06T14:56:24.556Z",90]]}]}]}"""
  val jsonMulti =
    """{"results":[{"statement_id":0,"series":[{"name":"mymeas","columns":["time","myfield","mytag1","mytag2"],"values":[[1488327378,33.1,null,null],[1488327438,12.4,"12","14"]]}]},{"statement_id":1,"series":[{"name":"mymeas","columns":["time","mean"],"values":[[0,22.75]]}]}]}"""

  val objError1 =
    UncategorizedError("responseJSON", "retention policy not found: myrp")
  val obj1 = ResultResponse(
    results = List(
      Result(
        series = List(
          Series(
            "databases",
            List("name"),
            values = List(List(IString("mydb")))
          )
        )
      )
    )
  )
  val obj2 = ResultResponse(
    results = List(
      Result(
        series = List(
          Series(
            "cpu",
            List("time", "value"),
            values = List(
              List(IString("2015-06-06T14:55:27.195Z"), IInt(90)),
              List(IString("2015-06-06T14:56:24.556Z"), IInt(90))
            )
          )
        )
      )
    )
  )

  implicit def influxValueJSON: JSONR[InfluxValue] = {
    case JInt(num)      => IO.succeed(IInt(num.toInt))
    case JString(str)   => IO.succeed(IString(str))
    case JBool(boolean) => IO.succeed(IBoolean(boolean))
    case JNull          => IO.succeed(INull)
    case x =>
      IO.fail(UncategorizedError("influxValueJSON", s"Found invalid type: $x"))
  }

  implicit def listOfListJSONR[A](
      implicit listJsonR: JSONR[List[A]]
  ): JSONR[List[List[A]]] = {
    case JArray(xs) => ZIO.collectAll(xs.map(fromJSON[List[A]]))
    case x          => IO.fail(UnexpectedJSONError(x, classOf[JArray]))
  }

  implicit def serieJSON: JSONR[Series] = Series.applyJSON(
    field[String]("name"),
    field[List[String]]("columns"),
    field[List[List[InfluxValue]]]("values")
  )

  implicit def resultJSON: JSONR[Result] =
    Result.applyJSON(
      field[List[Series]]("series"),
      field[Option[Int]]("statement_id")
    )

  implicit def responseJSON: JSONR[ResultResponse] = new JSONR[ResultResponse] {
    override def read(json: JValue): JsonResult[ResultResponse] = {
      ResultResponse
        .applyJSON(field[List[Result]]("results"))
        .read(json)
        .foldM(
          _ =>
            field[String]("error")(json)
              .flatMap(e => IO.fail(UncategorizedError("responseJSON", e))),
          IO.succeed
        )
    }
  }

  "Influx" in {
    unsafeRun(fromJSON[ResultResponse](parse(jsonError1)).either) must beLeft(
      objError1: Error
    )
    unsafeRun(fromJSON[ResultResponse](parse(json1)).either) must beRight(obj1)
    unsafeRun(fromJSON[ResultResponse](parse(json2)).either) must beRight(obj2)

  }
}
