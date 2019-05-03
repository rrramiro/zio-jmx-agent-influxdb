package fr.ramiro.zio.jmxagent.influxdb

import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.internal.{Platform, PlatformLive}
import zio.random.Random
import zio._

import scala.language.postfixOps

trait ZioRuntime
    extends Runtime[
      Clock with Console with System with Random with Blocking with Configuration
    ] {
  type Environment = Clock
    with Console
    with System
    with Random
    with Blocking
    with Configuration

  val Platform: Platform = PlatformLive.Default
  val Environment: Environment = new Clock.Live with Console.Live
  with System.Live with Random.Live with Blocking.Live with Configuration.Live

  def zioRun(configPath: String)(run: ZIO[Environment, Any, Unit]): Unit =
    sys.exit(
      unsafeRun(
        for {
          _ <- ZIO.environment[Environment] >>= (_.configuration
            .setConfigPath(configPath))
          fiber <- run.either.map {
            case Right(_) => 0
            case Left(_)  => 1
          }.fork
          _ <- IO.effectTotal(
            java.lang.Runtime.getRuntime.addShutdownHook(new Thread {
              override def run(): Unit = {
                val _ = unsafeRunSync(fiber.interrupt)
              }
            })
          )
          result <- fiber.join
        } yield result
      )
    )
}
