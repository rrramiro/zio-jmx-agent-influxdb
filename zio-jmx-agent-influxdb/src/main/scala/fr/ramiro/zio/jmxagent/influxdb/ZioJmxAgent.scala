package fr.ramiro.zio.jmxagent.influxdb

import java.lang.instrument.Instrumentation
import java.util.concurrent.TimeUnit

import zio.{IO, Queue, Task, ZIO, ZSchedule}
import zio.duration._
import zio.syntax._

import scala.language.postfixOps

object ZioJmxAgent extends ZioRuntime {

  private val logger = Logger.getLogger(this.getClass)

  private def premainInit: ZIO[Environment, Throwable, Unit] =
    readPremainConfiguration >>= (
        conf =>
          waitForCustomMBeanServer(
            conf.waitForCustomMBeanServer,
            conf.customMBeanServerTimeout
          ).delay(conf.delay)
      )

  private def readPremainConfiguration =
    ZIO.environment[Environment] >>= { env =>
      (
        env.system.propertyDuration(TimeUnit.MILLISECONDS)(
          "jmxtrans.agent.premain.delay",
          Duration.Zero
        ),
        env.system.propertyBoolean(
          "jmxtrans.agent.premain.waitForCustomMBeanServer"
        ),
        env.system.propertyDuration(TimeUnit.SECONDS)(
          "jmxtrans.agent.premain.waitForCustomMBeanServer.timeoutInSeconds",
          120 seconds
        )
      ).map3(PremainConfiguration)
    }

  private def waitForCustomMBeanServer(
      shouldWaitForCustomMBeanServer: Boolean,
      timeoutDuration: Duration
  ): ZIO[Environment, Throwable, Unit] =
    if (shouldWaitForCustomMBeanServer) {
      (ZIO.environment[Environment] >>= (_.system
        .propertyBoolean("javax.management.builder.initial")))
        .repeat {
          ZSchedule.identity[Boolean].whileOutput(!_) &&
          ZSchedule.duration(timeoutDuration) &&
          ZSchedule.spaced(1 second)
        }
        .map(_ => ())
    } else {
      Task.unit
    }

  private def initializeAgent = {
    //TODO influx
    for {
      env <- ZIO.environment[Environment]
      config <- env.configuration.getConfig
      _ <- Influx.withInflux(config.influxdbConfig) { db =>
        for {
          queue <- Queue
            .bounded[MBeanResultRaw](100)
            .map(_.map(PlatformMBean.jmxResultProcessor))
          c <- ZIO.forkAll(config.queries.map { q =>
            (PlatformMBean
              .collect(q.objectName, q.attributes) >>= queue.offerAll)
              .repeat(ZSchedule.spaced(q.interval))
          })
          _ <- (queue.take.map(_.toString) >>= env.console.putStrLn)
            .repeat(ZSchedule.spaced(1 second))
          _ <- c.interrupt
        } yield ()
      }
    } yield ()
  }

  final def agentmain(agentArgs: String, inst: Instrumentation): Unit =
    zioRun(agentArgs) {
      initializeAgent
    }

  final def premain(agentArgs: String, inst: Instrumentation): Unit =
    zioRun(agentArgs) {
      premainInit *> initializeAgent
    }

  final def main(args: Array[String]): Unit =
    premain("./testconfig.conf", null) //TODO remove

}
