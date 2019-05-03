package org.json4s
package zio

import org.specs2.mutable.Specification
import JsonScalazZio._
import _root_.zio.{DefaultRuntime, IO, ZIO}

object ValidationExample extends Specification with DefaultRuntime {

  case class Person(name: String, age: Int)

  "Validation" should {
    def min(x: Int): Int => Result[Int] =
      (y: Int) =>
        if (y < x) IO.fail(UncategorizedError("min", s"$y < $x"))
        else IO.succeed(y)

    def max(x: Int): Int => Result[Int] =
      (y: Int) =>
        if (y > x) IO.fail(UncategorizedError("max", s"$y > $x"))
        else IO.succeed(y)

    val json = native.JsonParser.parse(""" {"name":"joe","age":17} """)

    // Note 'apply _' is not needed on Scala 2.8.1 >=
    "fail when age is less than min age" in {
      // Age must be between 18 an 60
      val person = Person.applyJSON(
        field[String]("name"),
        validate[Int]("age").flatMap(min(18)).flatMap(max(60)).provide
      )
      unsafeRun(person(json).either) must beLeft(
        UncategorizedError("min", "17 < 18"): Error
      )
    }

    "pass when age within limits" in {
      val person = Person.applyJSON(
        field[String]("name"),
        (validate[Int]("age") flatMap min(16) flatMap max(60)).provide
      )
      unsafeRun(person(json)) must_== Person("joe", 17)
    }
  }

  case class Range(start: Int, end: Int)

  // This example shows:
  // * a validation where result depends on more than one value
  // * parse a List with invalid values
  "Range filtering" should {
    val json = native.JsonParser.parse(
      """ [{"s":10,"e":17},{"s":12,"e":13},{"s":11,"e":8}] """
    )

    val ascending = (x1: Int, x2: Int) => {
      if (x1 > x2) IO.fail(UncategorizedError("asc", x1 + " > " + x2))
      else IO.succeed((x1, x2))
    }

    // Valid range is a range having start <= end
    implicit def rangeJSON: JSONR[Range] =
      (json: JValue) =>
        (field[Int]("s")(json), field[Int]("e")(json))
          .map2(ascending)
          .flatten map Range.tupled

    "fail if lists contains invalid ranges" in {
      val r = fromJSON[List[Range]](json)
      unsafeRun(r.either) must beLeft(
        UncategorizedError("asc", "11 > 8"): Error
      )
    }

    "optionally return only valid ranges" in {
      val ranges = ZIO
        .collectAll(
          json.children.map(fromJSON[Range]).map(_.option.map(_.toList))
        )
        .map(_.flatten)
      unsafeRun(ranges) must_== List(Range(10, 17), Range(12, 13))
    }
  }
}
