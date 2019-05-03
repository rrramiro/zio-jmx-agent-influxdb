package org.json4s
package zio

import _root_.zio.{IO, ZIO}

trait Base { this: Types =>
  implicit def boolJSON: JSON[Boolean] = new JSON[Boolean] {
    def read(json: JValue): Result[Boolean] = json match {
      case JBool(b) => IO.succeed(b)
      case x        => IO.fail(UnexpectedJSONError(x, classOf[JBool]))
    }

    def write(value: Boolean) = JBool(value)
  }

  implicit def intJSON: JSON[Int] = new JSON[Int] {
    def read(json: JValue): Result[Int] = readJInt(json)(_.intValue)

    def write(value: Int) = JInt(BigInt(value))
  }

  implicit def longJSON: JSON[Long] = new JSON[Long] {
    def read(json: JValue): Result[Long] = readJInt(json)(_.longValue)

    def write(value: Long) = JInt(BigInt(value))
  }

  implicit def doubleJSON: JSON[Double] = new JSON[Double] {
    def read(json: JValue): Result[Double] = json match {
      case JDouble(x) => IO.succeed(x)
      case x          => IO.fail(UnexpectedJSONError(x, classOf[JDouble]))
    }

    def write(value: Double) = JDouble(value)
  }

  implicit def stringJSON: JSON[String] = new JSON[String] {
    def read(json: JValue): Result[String] = json match {
      case JString(x) => IO.succeed(x)
      case x          => IO.fail(UnexpectedJSONError(x, classOf[JString]))
    }

    def write(value: String) = JString(value)
  }

  def readJInt[T](json: JValue)(f: BigInt => T): Result[T] = json match {
    case JInt(x) => IO.succeed(f(x))
    case x       => IO.fail(UnexpectedJSONError(x, classOf[JInt]))
  }

  implicit def bigintJSON: JSON[BigInt] = new JSON[BigInt] {
    def read(json: JValue): Result[BigInt] = readJInt(json)(identity)

    def write(value: BigInt) = JInt(value)
  }

  implicit def jvalueJSON: JSON[JValue] = new JSON[JValue] {
    def read(json: JValue): Result[JValue] = IO.succeed(json)
    def write(value: JValue): JValue = value
  }

  implicit def listJSONR[A: JSONR]: JSONR[List[A]] = {
    case JArray(xs) => ZIO.collectAll(xs.map(fromJSON[A]))
    case x          => IO.fail(UnexpectedJSONError(x, classOf[JArray]))
  }

  implicit def listJSONW[A: JSONW]: JSONW[List[A]] =
    (values: List[A]) => JArray(values.map(x => toJSON(x)))

  implicit def optionJSONR[A: JSONR]: JSONR[Option[A]] = {
    case JNothing | JNull => IO.succeed(None)
    case x                => fromJSON[A](x).map(Option.apply)
  }

  implicit def optionJSONW[A: JSONW]: JSONW[Option[A]] =
    (value: Option[A]) => value.map(x => toJSON(x)).getOrElse(JNull)

  implicit def mapJSONR[A: JSONR]: JSONR[Map[String, A]] = {
    case JObject(fs) =>
      val m = fs.map(f => fromJSON[A](f._2) map (f._1 -> _))
      val mm = ZIO.collectAll(m)
      mm.map(_.toMap)
    case x => IO.fail(UnexpectedJSONError(x, classOf[JObject]))
  }

  implicit def mapJSONW[A: JSONW]: JSONW[Map[String, A]] =
    (values: Map[String, A]) =>
      JObject(values.map { case (k, v) => JField(k, toJSON(v)) }.toList)
}
