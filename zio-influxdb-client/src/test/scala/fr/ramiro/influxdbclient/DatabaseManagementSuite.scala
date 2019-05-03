package fr.ramiro.influxdbclient

import org.scalatest.BeforeAndAfter

class DatabaseManagementSuite extends CustomTestSuite with BeforeAndAfter {

  val databaseName = "_test_database_mgmnt"

  var database: InfluxDBSelected = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    database = influxdb.selectDatabase(databaseName)
  }

  before {
    val exists = await(database.exists())
    if (exists) {
      await(database.drop())
    }
  }

  test("A database can be created and dropped") {
    await(database.create())
    assert(await(database.exists()))

    await(database.drop())
    assert(!await(database.exists()))
  }
}
