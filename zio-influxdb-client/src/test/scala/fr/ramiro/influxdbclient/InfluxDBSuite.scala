package fr.ramiro.influxdbclient

import fr.ramiro.influxdbclient.operations.QueryException

class InfluxDBSuite extends CustomTestSuite {

  test("Shows existing databases") {
    val result = await(influxdb.showDatabases())
    assert(result.contains("_internal"))
  }

  test("Shows existing measurements") {
    val result = await(influxdb.selectDatabase("_internal").showMeasurements())
    assert(result.contains("database"))
    assert(result.contains("subscriber"))
  }

  test("Server can be pinged") {
    await(influxdb.ping())
  }

  test("If an error happens during a ping a PingException is thrown") {
    withFailingInfluxDb() { client =>
      for {
        result <- client.ping().either
      } yield {
        result match {
          case Left(PingException(_, _)) =>
          case x                         => fail(s"Unexpected: $x")
        }
      }
    }
  }

  test("If an error happens during a query a QueryException is thrown") {
    withFailingInfluxDb() { client =>
      for {
        result <- client.query("").either
      } yield {
        result match {
          case Left(QueryException(_, _)) =>
          case x                          => fail(s"Unexpected: $x")
        }
      }
    }
  }

  test("Multiple queries can be executed at the same time") {
    val queries = List(
      """select * from "write" limit 5""",
      "select * from subscriber limit 5"
    )
    val results =
      await(influxdb.selectDatabase("_internal").multiQuery(queries))
    assert(results.length == 2)
    assert(results(0).series.head.name == "write")
    assert(results(1).series.head.name == "subscriber")
  }

  test("Connections can be closed") {
    val influxdb = InfluxDB.connect()
    influxdb.close()
    assert(influxdb.isClosed)
  }
}
