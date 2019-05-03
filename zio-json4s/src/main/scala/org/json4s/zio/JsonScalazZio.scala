package org.json4s
package zio

import _root_.zio.{IO, ZIO}
import _root_.zio.syntax.ZIOSyntax._

trait Types {
  type Result[+A] = IO[Error, A]

  sealed abstract class Error extends Product with Serializable
  case class UnexpectedJSONError(was: JValue, expected: Class[_ <: JValue])
      extends Error
  case class NoSuchFieldError(name: String, json: JValue) extends Error
  case class UncategorizedError(key: String, desc: String) extends Error

  trait JSONR[A] {
    def read(json: JValue): Result[A]
  }

  trait JSONW[A] {
    def write(value: A): JValue
  }

  trait JSON[A] extends JSONR[A] with JSONW[A]

  implicit def Result2JSONR[A](f: JValue => Result[A]): JSONR[A] =
    (json: JValue) => f(json)

  def fromJSON[A](json: JValue)(implicit jsonr: JSONR[A]): Result[A] =
    jsonr.read(json)
  def toJSON[A](value: A)(implicit jsonw: JSONW[A]): JValue = jsonw.write(value)

  def field[A](
      name: String
  )(json: JValue)(implicit jsonr: JSONR[A]): Result[A] = json match {
    case JObject(fs) =>
      fs.find(_._1 == name) match {
        case Some(f) => jsonr.read(f._2)
        case None =>
          jsonr.read(JNothing).orElse(IO.fail(NoSuchFieldError(name, json)))
      }

    case x =>
      IO.fail(UnexpectedJSONError(x, classOf[JObject]))
  }

  def validate[A: JSONR](name: String): ZIO[JValue, Error, A] = {
    for {
      json <- ZIO.environment[JValue]
      element <- field[A](name)(json)
    } yield element
  }

  def makeObj(fields: Iterable[(String, JValue)]): JObject =
    JObject(fields.toList.map { case (n, v) => JField(n, v) })

  implicit final def ioEagerSyntax[A](a: A): EagerCreationSyntax[A] =
    new EagerCreationSyntax[A](a)
  implicit final def ioLazySyntax[A](a: => A): LazyCreationSyntax[A] =
    new LazyCreationSyntax[A](() => a)
  implicit final def ioIterableSyntax[E, A, B](
      ios: Iterable[ZIO[E, A, B]]
  ): IterableSyntax[E, A, B] =
    new IterableSyntax(ios)
  implicit final def ioTuple2Syntax[R, E, A, B](
      ios: (ZIO[R, E, A], ZIO[R, E, B])
  ): Tuple2Syntax[R, E, A, B] = new Tuple2Syntax(ios)
  implicit final def ioTuple3Syntax[R, E, A, B, C](
      ios: (ZIO[R, E, A], ZIO[R, E, B], ZIO[R, E, C])
  ): Tuple3Syntax[R, E, A, B, C] =
    new Tuple3Syntax(ios)
  implicit final def ioTuple4Syntax[R, E, A, B, C, D](
      ios: (ZIO[R, E, A], ZIO[R, E, B], ZIO[R, E, C], ZIO[R, E, D])
  ): Tuple4Syntax[R, E, A, B, C, D] =
    new Tuple4Syntax(ios)

  implicit final class IOTuple5[E, A, B, C, D, G](
      val ios5: (IO[E, A], IO[E, B], IO[E, C], IO[E, D], IO[E, G])
  ) {
    def map5[F](f: (A, B, C, D, G) => F): IO[E, F] =
      for {
        a <- ios5._1
        b <- ios5._2
        c <- ios5._3
        d <- ios5._4
        g <- ios5._5
      } yield f(a, b, c, d, g)
  }

  implicit final class IOTuple6[E, A, B, C, D, G, H](
      val ios6: (IO[E, A], IO[E, B], IO[E, C], IO[E, D], IO[E, G], IO[E, H])
  ) {
    def map6[F](f: (A, B, C, D, G, H) => F): IO[E, F] =
      for {
        a <- ios6._1
        b <- ios6._2
        c <- ios6._3
        d <- ios6._4
        g <- ios6._5
        h <- ios6._6
      } yield f(a, b, c, d, g, h)
  }

  implicit final class IOTuple7[E, A, B, C, D, G, H, I](
      val ios7: (
          IO[E, A],
          IO[E, B],
          IO[E, C],
          IO[E, D],
          IO[E, G],
          IO[E, H],
          IO[E, I]
      )
  ) {
    def map7[F](f: (A, B, C, D, G, H, I) => F): IO[E, F] =
      for {
        a <- ios7._1
        b <- ios7._2
        c <- ios7._3
        d <- ios7._4
        g <- ios7._5
        h <- ios7._6
        i <- ios7._7
      } yield f(a, b, c, d, g, h, i)
  }

  implicit final class IOTuple8[E, A, B, C, D, G, H, I, J](
      val ios8: (
          IO[E, A],
          IO[E, B],
          IO[E, C],
          IO[E, D],
          IO[E, G],
          IO[E, H],
          IO[E, I],
          IO[E, J]
      )
  ) {
    def map8[F](f: (A, B, C, D, G, H, I, J) => F): IO[E, F] =
      for {
        a <- ios8._1
        b <- ios8._2
        c <- ios8._3
        d <- ios8._4
        g <- ios8._5
        h <- ios8._6
        i <- ios8._7
        j <- ios8._8
      } yield f(a, b, c, d, g, h, i, j)
  }

}

object JsonScalazZio
    extends Types
    with Lifting
    with Base
    with org.json4s.zio.Tuples
