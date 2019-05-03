package fr.ramiro.influxdbclient

import Mocks.ExceptionThrowingHttpClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import zio.{DefaultRuntime, IO, Task}

class CustomTestSuite
    extends FunSuite
    with BeforeAndAfterAll
    with DefaultRuntime
    with ScalaFutures {
  override implicit val patienceConfig =
    PatienceConfig(
      timeout = scaled(Span(15, Seconds)),
      interval = scaled(Span(2, Seconds))
    )

  val databaseUsername = "influx_user"
  val databasePassword = "influx_password"

  val influxdbConfig = SttpConfig(
    "localhost",
    8086,
    false,
    Some(databaseUsername),
    Some(databasePassword)
  )

  var influxdb: InfluxDB = _

  def await[A](f: Task[A]): A = unsafeRunToFuture(f).futureValue

  def withClient[T](config: SttpConfig)(use: SttpClient => Task[T]): T = await {
    IO.succeed(new SttpClient(config)).bracket(c => IO.succeed(c.close()))(use)
  }

  def withFailingInfluxDb[T](
      config: SttpConfig = SttpConfig("", 0)
  )(use: InfluxDB => Task[T]) = await {
    IO.succeed(new InfluxDB(new ExceptionThrowingHttpClient(config)))
      .bracket(c => IO.succeed(c.close()))(use)
  }

  override def beforeAll(): Unit = {
    influxdb = InfluxDB.connect(influxdbConfig)
  }

  override def afterAll: Unit = influxdb.close()

}
