package fr.ramiro.influxdbclient

import fr.ramiro.influxdbclient.operations._

class UserManagementSuite extends CustomTestSuite {

  val username = "_test_username"
  val password = "test_password"

  test("A user can be created and dropped") {
    await(influxdb.createUser(username, password))

    var users = await(influxdb.showUsers())
    var usernames = users.series.head.points("user")
    assert(usernames.contains(IString(username)))

    await(influxdb.dropUser(username).either)

    users = await(influxdb.showUsers())
    usernames = users.series.head.points("user")
    assert(!usernames.contains(IString(username)))
  }

  test("Passwords are correctly escaped") {
    assert(influxdb.escapePassword("pass'wor\nd") == "pass\\'wor\\\nd")
  }

  test("A user's password can be changed") {
    await(influxdb.createUser(username, password))
    assert(
      await(influxdb.setUserPassword(username, "new_password").either).isRight
    )
    await(influxdb.dropUser(username).either)
  }

  test("Privileges can be granted to and revoked from a user") {
    await(influxdb.createUser(username, password))
    val database = influxdb.selectDatabase("_test_database")
    await(database.create())
    assert(
      await(influxdb.grantPrivileges(username, "_test_database", ALL).either).isRight
    )
    assert(
      await(
        influxdb
          .revokePrivileges(username, "_test_database", WRITE)
          .either
      ).isRight
    )
    await(influxdb.dropUser(username).either)
    await(database.drop().either)
  }

  test("A user can be created as cluster admin") {
    await(influxdb.createUser(username, password, true))
    assert(await(influxdb.showUsers().either).isRight)
    assert(await(influxdb.userIsClusterAdmin(username).either) == Right(true))
    await(influxdb.dropUser(username).either)
  }

  test("A user can be made cluster admin") {
    await(influxdb.createUser(username, password))
    assert(await(influxdb.makeClusterAdmin(username).either).isRight)
    assert(await(influxdb.userIsClusterAdmin(username).either) == Right(true))
    await(influxdb.dropUser(username).either)
  }
}
