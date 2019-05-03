package fr.ramiro.zio.jmxagent.influxdb

import java.util.concurrent.TimeUnit

import zio.{IO, Task, UIO, ZIO}
import zio.duration.Duration

trait System extends Serializable {
  val system: System.Service[Any]
}
object System extends Serializable {
  trait Service[R] extends Serializable {
    def env(variable: String): ZIO[R, SecurityException, Option[String]]

    def setProperty(
        prop: String,
        value: String
    ): ZIO[Any, Throwable, Option[String]]
    def property(prop: String): ZIO[R, Throwable, Option[String]]
    def propertyBoolean(key: String): Task[Boolean]
    def propertyDuration(
        timeUnit: TimeUnit
    ): (String, Duration) => Task[Duration]

    val lineSeparator: ZIO[R, Nothing, String]
  }
  trait Live extends System {
    val system: Service[Any] = new Service[Any] {
      import java.lang.{System => JSystem}

      def env(variable: String): ZIO[Any, SecurityException, Option[String]] =
        ZIO.effect(Option(JSystem.getenv(variable))).refineOrDie {
          case e: SecurityException => e
        }

      def setProperty(
          prop: String,
          value: String
      ): ZIO[Any, Throwable, Option[String]] =
        ZIO.effect(Option(JSystem.setProperty(prop, value)))

      def property(prop: String): ZIO[Any, Throwable, Option[String]] =
        ZIO.effect(Option(JSystem.getProperty(prop)))

      def propertyBoolean(key: String): Task[Boolean] =
        property(key).map(_.contains("true"))

      def propertyDuration(
          timeUnit: TimeUnit
      ): (String, Duration) => Task[Duration] =
        propertyWithDefault(i => Duration(i.toInt, timeUnit))

      private def propertyWithDefault[T](
          f: String => T
      )(key: String, default: T): Task[T] =
        property(key) >>= (o => IO.effect(o.map(f).getOrElse(default)))

      val lineSeparator: UIO[String] = ZIO.effectTotal(JSystem.lineSeparator)
    }
  }
  object Live extends Live
}
