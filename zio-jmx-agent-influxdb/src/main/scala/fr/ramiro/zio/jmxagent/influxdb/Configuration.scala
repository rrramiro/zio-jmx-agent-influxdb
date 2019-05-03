package fr.ramiro.zio.jmxagent.influxdb

import java.lang

import fr.ramiro.influxdbclient.SttpConfigWithDatabase
import pureconfig._
import pureconfig.module.zio._
import pureconfig.generic.auto._
import zio.{IO, Task, ZIO}
import zio.duration.Duration

import scala.concurrent.duration.{Duration => ScalaDuration}

trait Configuration extends Serializable {
  val configuration: Configuration.Service[Any]
}

object Configuration {
  trait Service[R] extends Serializable {
    def setConfigPath(path: String): ZIO[R, Throwable, Option[String]]
    def getConfigPath: ZIO[R, Throwable, Option[String]]
    def getConfig: ZIO[R, Throwable, JmxInfluxConfig]
  }
  trait Live extends Configuration { self: System =>

    private val configurationPathKey = "jmxtrans.config.path"

    private implicit val zioDurationConfigReader: ConfigReader[Duration] =
      ConfigReader[ScalaDuration]
        .map(d => Duration.fromNanos(d.toNanos))

    private val configWithDb = (tmp: JmxInfluxConfigTmp) =>
      IO.fromOption(
          SttpConfigWithDatabase
            .fromUrl(tmp.url)
            .map(JmxInfluxConfig(_, tmp.queries))
        )
        .mapError(_ => new Exception(""))

    private def loadConfig(
        filenameOpt: Option[String]
    ): Task[JmxInfluxConfig] =
      ConfigSource
        .file(filenameOpt.getOrElse("jmxtrans.conf"))
        .loadIO[JmxInfluxConfigTmp] >>= configWithDb

    val configuration: Service[Any] = new Service[Any] {
      override def setConfigPath(path: String): Task[Option[String]] =
        if (path.nonEmpty) system.setProperty(configurationPathKey, path)
        else IO.succeed(None)
      override def getConfigPath: Task[Option[String]] =
        system.property(configurationPathKey)
      override def getConfig: Task[JmxInfluxConfig] =
        getConfigPath >>= loadConfig
    }
  }
  object Live extends Live with System.Live
}
