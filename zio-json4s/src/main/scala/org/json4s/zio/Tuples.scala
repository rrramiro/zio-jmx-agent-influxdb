package org.json4s
package zio

import _root_.zio.IO

trait Tuples { this: Types =>
  implicit def Tuple2JSON[A: JSON, B: JSON]: JSON[(A, B)] = new JSON[(A, B)] {
    def read(json: JValue): Result[(A, B)] = json match {
      case JArray(a :: b :: _) =>
        (fromJSON[A](a), fromJSON[B](b)).map2(Tuple2.apply)
      case x =>
        IO.fail(UnexpectedJSONError(x, classOf[JArray]))
    }

    def write(value: (A, B)) =
      JArray(toJSON(value._1) :: toJSON(value._2) :: Nil)
  }

  implicit def Tuple3JSON[A: JSON, B: JSON, C: JSON]: JSON[(A, B, C)] =
    new JSON[(A, B, C)] {
      def read(json: JValue): Result[(A, B, C)] = json match {
        case JArray(a :: b :: c :: _) =>
          (fromJSON[A](a), fromJSON[B](b), fromJSON[C](c)).map3(Tuple3.apply)
        case x =>
          IO.fail(UnexpectedJSONError(x, classOf[JArray]))
      }

      def write(value: (A, B, C)) =
        JArray(toJSON(value._1) :: toJSON(value._2) :: toJSON(value._3) :: Nil)
    }

  implicit def Tuple4JSON[A: JSON, B: JSON, C: JSON, D: JSON]
      : JSON[(A, B, C, D)] = new JSON[(A, B, C, D)] {
    def read(json: JValue): Result[(A, B, C, D)] = json match {
      case JArray(a :: b :: c :: d :: _) =>
        (fromJSON[A](a), fromJSON[B](b), fromJSON[C](c), fromJSON[D](d))
          .map4(Tuple4.apply)
      case x =>
        IO.fail(UnexpectedJSONError(x, classOf[JArray]))
    }

    def write(value: (A, B, C, D)) =
      JArray(
        toJSON(value._1) :: toJSON(value._2) :: toJSON(value._3) :: toJSON(
          value._4
        ) :: Nil
      )
  }

  implicit def Tuple5JSON[A: JSON, B: JSON, C: JSON, D: JSON, E: JSON]
      : JSON[(A, B, C, D, E)] = new JSON[(A, B, C, D, E)] {
    def read(json: JValue): Result[(A, B, C, D, E)] = json match {
      case JArray(a :: b :: c :: d :: e :: _) =>
        (
          fromJSON[A](a),
          fromJSON[B](b),
          fromJSON[C](c),
          fromJSON[D](d),
          fromJSON[E](e)
        ).map5(Tuple5.apply)
      case x =>
        IO.fail(UnexpectedJSONError(x, classOf[JArray]))
    }

    def write(value: (A, B, C, D, E)) =
      JArray(
        toJSON(value._1) :: toJSON(value._2) :: toJSON(value._3) :: toJSON(
          value._4
        ) :: toJSON(value._5) :: Nil
      )
  }

  implicit def Tuple6JSON[A: JSON, B: JSON, C: JSON, D: JSON, E: JSON, F: JSON]
      : JSON[(A, B, C, D, E, F)] = new JSON[(A, B, C, D, E, F)] {
    def read(json: JValue): Result[(A, B, C, D, E, F)] = json match {
      case JArray(a :: b :: c :: d :: e :: f :: _) =>
        (
          fromJSON[A](a),
          fromJSON[B](b),
          fromJSON[C](c),
          fromJSON[D](d),
          fromJSON[E](e),
          fromJSON[F](f)
        ).map6(Tuple6.apply)
      case x =>
        IO.fail(UnexpectedJSONError(x, classOf[JArray]))
    }

    def write(value: (A, B, C, D, E, F)) =
      JArray(
        toJSON(value._1) :: toJSON(value._2) :: toJSON(value._3) :: toJSON(
          value._4
        ) :: toJSON(value._5) :: toJSON(value._6) :: Nil
      )
  }
}
