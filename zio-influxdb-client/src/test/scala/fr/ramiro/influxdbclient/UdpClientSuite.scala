package fr.ramiro.influxdbclient

import fr.ramiro.influxdbclient.model.Point
import org.scalatest.BeforeAndAfter
import zio.IO
import zio.clock.Clock
import zio.duration._

import scala.language.postfixOps

class UdpClientSuite extends CustomTestSuite with BeforeAndAfter {

  val databaseName = "_test_database_udp"
  val udpPort = 8086
  val udpHost = "localhost"
  var database: InfluxDBSelected = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    database = influxdb.selectDatabase(databaseName)
  }

  before {
    await {
      for {
        _ <- database.drop().either
        _ <- database.create().either
      } yield ()
    }
  }

  test("Points can be written") {
    await(
      IO.succeed(InfluxDB.udpConnect(udpHost, udpPort))
        .bracket(c => IO.effectTotal(c.close())) { udpClient =>
          for {
            _ <- udpClient.write(
              Point("test_measurement")
                .addField("value", 123)
                .addTag("tag_key", "tag_value")
            )
            _ <- Clock.Live.clock
              .sleep(1000 millis) // to allow flushing to happen inside influx
            result <- influxdb
              .selectDatabase(databaseName)
              .query("SELECT * FROM test_measurement")
          } yield {
            assert(result.series.nonEmpty)
            assert(result.series.head.records.length == 1)
            assert(result.series.head.records.head("value") == IInt(123))
          }
        }
    )
  }

  test("Points can be written in bulk") {
    await(
      IO.succeed(InfluxDB.udpConnect(udpHost, udpPort))
        .bracket(c => IO.effectTotal(c.close())) { udpClient =>
          val timestamp = System.currentTimeMillis()
          for {
            _ <- udpClient.write(
              List(
                Point("test_measurement", timestamp)
                  .addField("value", 1)
                  .addTag("tag_key", "tag_value"),
                Point("test_measurement", timestamp + 1)
                  .addField("value", 2)
                  .addTag("tag_key", "tag_value"),
                Point("test_measurement", timestamp + 2)
                  .addField("value", 3)
                  .addTag("tag_key", "tag_value")
              )
            )
            _ <- Clock.Live.clock
              .sleep(1000 millis) // to allow flushing to happen inside influx
            result <- influxdb
              .selectDatabase(databaseName)
              .query("SELECT * FROM test_measurement")
          } yield {
            assert(result.series.nonEmpty)
            assert(result.series.head.records.length == 3)
          }
        }
    )
  }

}
