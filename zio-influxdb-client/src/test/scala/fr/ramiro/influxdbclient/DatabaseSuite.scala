package fr.ramiro.influxdbclient

import fr.ramiro.influxdbclient.Mocks.{ErrorReturningHttpClient, ExceptionThrowingHttpClient}
import fr.ramiro.influxdbclient.Parameter._
import fr.ramiro.influxdbclient.Parameter.Consistency.ALL
import fr.ramiro.influxdbclient.Parameter.TimeUnit.MILLISECONDS
import fr.ramiro.influxdbclient.model.Point
import fr.ramiro.influxdbclient.operations._
import org.scalatest.BeforeAndAfter
import zio.IO

class DatabaseSuite extends CustomTestSuite with BeforeAndAfter {

  var database: InfluxDBSelected = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    database = influxdb.selectDatabase("_test_database_db")
  }

  before {
    await {
      for {
        _ <- database.drop().either
        _ <- database.create()
      } yield ()
    }
  }

  override def afterAll: Unit = {
    await(database.drop().either)
    super.afterAll
  }

  test("Writing to a non-existent database throws a DatabaseNotFoundException") {
    val db = influxdb.selectDatabase("_test_database_db_2")

    val result = await {
      for {
        r <- db.write(Point("test_measurement").addField("value", 123)).either
        _ <- db.drop().either
      } yield r
    }
    result match {
      case Left(e) if e.isInstanceOf[DatabaseNotFoundException] =>
      case x                                                    => fail(s"Unexpected: $x")
    }
  }

  test("A point can be written and read") {
    await(database.write(Point("test_measurement").addField("value", 123)))
    val result = await(database.query("SELECT * FROM test_measurement"))
    assert(result.series.length == 1)
  }

  test("Multiple points can be written and read") {
    val time = 1444760421000L
    val points = List(
      Point("test_measurement", time).addField("value", 123),
      Point("test_measurement", time + 1).addField("value", 123),
      Point("test_measurement", time + 2).addField("value", 123)
    )
    await(
      database.write(points, precision = Some(Precision(MILLISECONDS)))
    )
    val result = await(database.query("SELECT * FROM test_measurement"))
    assert(result.series.head.records.length == 3)
  }

  test("A point can be written with tags") {
    await(
      database.write(
        Point("test_measurement")
          .addField("value", 123)
          .addTag("tag_key", "tag_value")
      )
    )
    val result = await(
      database.query("SELECT * FROM test_measurement WHERE tag_key='tag_value'")
    )
    assert(result.series.length == 1)
  }

  test("A point can be written and read with a precision parameter") {
    val time = 1444760421270L
    await(
      database.write(
        Point("test_measurement", time).addField("value", 123),
        precision = Some(Precision(TimeUnit.MILLISECONDS))
      )
    )
    val result = await(
      database.query(
        "SELECT * FROM test_measurement",
        Some(Epoch(TimeUnit.MILLISECONDS))
      )
    )

    assert(result.series.head.records.head("time") == IInt(time))
  }

  test("A point can be written with a consistency parameter") {
    await(
      database.write(
        Point("test_measurement").addField("value", 123),
        consistency = Some(ALL)
      )
    )
    val result = await(database.query("SELECT * FROM test_measurement"))
    assert(result.series.length == 1)
  }

  test("A point can be written with a retention policy parameter") {
    val retentionPolicyName = "custom_retention_policy"
    val measurementName = "test_measurement"
    await(
      database
        .createRetentionPolicy(retentionPolicyName, "1w", 1, default = false)
    )
    await(
      database.write(
        Point(measurementName).addField("value", 123),
        retentionPolicy = Some(RetentionPolicy(retentionPolicyName))
      )
    )
    val result = await(
      database.query(
        "SELECT * FROM %s.%s".format(retentionPolicyName, measurementName)
      )
    )
    assert(result.series.length == 1)
  }

  test("Writing to a non-existent retention policy throws an error") {
    val result = await(
      database
        .write(
          Point("test_measurement").addField("value", 123),
          retentionPolicy = Some(RetentionPolicy("fake_retention_policy"))
        )
        .either
    )
    result match {
      case Left(e) if e.isInstanceOf[WriteException] =>
      case x =>
        fail("Write using non-existent retention policy did not fail: $x")
    }
  }

  test("If an exception occurrs during a write, a WriteException is thrown") {
    await(
      IO.succeed(new ExceptionThrowingHttpClient(influxdbConfig))
        .bracket(c => IO.succeed(c.close())) { client =>
          val db = new InfluxDBSelected("fake_name", client)
          for {
            result <- db.write(Point("point")).either
          } yield {
            result match {
              case Left(error) if error.isInstanceOf[WriteException] =>
              case x                                                 => fail(s"Unexpected: $x")
            }
          }
        }
    )
  }

  test(
    "If a 200 code is return during a write, a MalformedRequestException is thrown"
  ) {
    await(
      IO.succeed(new ErrorReturningHttpClient(influxdbConfig, 200))
        .bracket(c => IO.succeed(c.close())) { client =>
          val db = new InfluxDBSelected("fake_name", client)
          for {
            result <- db.write(Point("point")).either
          } yield {
            result match {
              case Left(error)
                  if error.isInstanceOf[RequestNotCompletedException] =>
              case x                                                  => fail(s"Unexpected: $x")
            }
          }
        }
    )
  }

  test(
    "If a 400 error occurrs during a write, a MalformedRequestException is thrown"
  ) {
    await(
      IO.succeed(new ErrorReturningHttpClient(influxdbConfig, 400))
        .bracket(c => IO.succeed(c.close())) { client =>
          val db = new InfluxDBSelected("fake_name", client)
          for {
            result <- db.write(Point("point")).either
          } yield {
            result match {
              case Left(error)
                  if error.isInstanceOf[MalformedRequestException] =>
              case x                                               => fail(s"Unexpected: $x")
            }
          }
        }
    )
  }

  test(
    "If a 500 error occurrs during a write, a ServerUnavailableException is thrown"
  ) {
    await(
      IO.succeed(new ErrorReturningHttpClient(influxdbConfig, 500))
        .bracket(c => IO.succeed(c.close())) { client =>
          val db = new InfluxDBSelected("fake_name", client)
          for {
            result <- db.write(Point("point")).either
          } yield {
            result match {
              case Left(error)
                  if error.isInstanceOf[ServerUnavailableException] =>
              case x                                                => fail(s"Unexpected: $x")
            }
          }
        }
    )
  }

}
