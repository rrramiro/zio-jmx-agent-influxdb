package fr.ramiro.zio.jmxagent.influxdb

import ch.qos.logback.classic.{Level, LoggerContext, Logger => LogbackLogger}
import org.slf4j.{LoggerFactory => SLF4JLoggerFactory, Logger => SLF4JLogger}
import zio.{IO, UIO}
import java.lang.{System => JSystem}

class Logger(l: LogbackLogger) {
  def trace(msg: String): UIO[Unit] = IO.effectTotal(l.trace(msg))
  def debug(msg: String): UIO[Unit] = IO.effectTotal(l.debug(msg))
  def info(msg: String): UIO[Unit] = IO.effectTotal(l.info(msg))
  def warn(msg: String): UIO[Unit] = IO.effectTotal(l.warn(msg))
  def error(msg: String): UIO[Unit] = IO.effectTotal(l.error(msg))
}

object Logger {
  private val context =
    SLF4JLoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
  private val rootLogger = context.getLogger(SLF4JLogger.ROOT_LOGGER_NAME)

  def getLogger(clazz: Class[_]): Logger = {
    val logger = context.getLogger(clazz)
    logger.setLevel(
      Level.valueOf(
        JSystem
          .getProperty(clazz.getName + ".level", rootLogger.getLevel.levelStr)
      )
    )
    new Logger(logger)
  }

}
