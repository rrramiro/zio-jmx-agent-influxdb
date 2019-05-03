package fr.ramiro.influxdbclient

import fr.ramiro.influxdbclient.operations.InvalidRetentionPolicyParametersException
import org.scalatest.BeforeAndAfter

class RetentionPolicyManagementSuite
    extends CustomTestSuite
    with BeforeAndAfter {

  val databaseName = "_test_database_rp"

  var database: InfluxDBSelected = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    database = influxdb.selectDatabase(databaseName)
  }

  val retentionPolicyName = "test_retention_policy"

  before {
    await(database.create())
  }

  after {
    await(database.drop())
  }

  test("A retention policy can be created") {
    await(
      database
        .createRetentionPolicy(retentionPolicyName, "1w", 1, default = true)
    )
    val policies = await(database.showRetentionPolicies())
    assert(policies.series.head.records.length == 2)
    val policy = policies.series.head.records(1)
    assert(policy("name") == IString(retentionPolicyName))
    assert(policy("duration") == IString("168h0m0s"))
    assert(policy("replicaN") == IInt(1))
    assert(policy("default") == IBoolean(true))
  }

  test("A retention policy can be created and deleted") {
    await(
      database
        .createRetentionPolicy(retentionPolicyName, "1w", 1, default = false)
    )
    await(database.dropRetentionPolicy(retentionPolicyName))

    val policiesAfterDeleting = await(database.showRetentionPolicies())
    assert(policiesAfterDeleting.series.head.records.length == 1)
  }

  test("A retention policy's duration can be altered") {
    await(
      database
        .createRetentionPolicy(retentionPolicyName, "1w", 1, default = false)
    )
    await(database.alterRetentionPolicy(retentionPolicyName, Some("2w")))
    val policies = await(database.showRetentionPolicies())
    val policy = policies.series.head.records(1)
    assert(policy("name") == IString(retentionPolicyName))
    assert(policy("duration") == IString("336h0m0s"))
  }

  test("A retention policy's replication can be altered") {
    await(
      database
        .createRetentionPolicy(retentionPolicyName, "1w", 1, default = false)
    )
    await(database.alterRetentionPolicy(retentionPolicyName, replication = 2))
    val policies = await(database.showRetentionPolicies())
    val policy = policies.series.head.records(1)
    assert(policy("name") == IString(retentionPolicyName))
    assert(policy("replicaN") == IInt(2))
  }

  test("A retention policy's defaultness can be altered") {
    await(
      database
        .createRetentionPolicy(retentionPolicyName, "1w", 1, default = false)
    )
    await(database.alterRetentionPolicy(retentionPolicyName, default = true))
    val policies = await(database.showRetentionPolicies())
    val policy = policies.series.head.records(1)
    assert(policy("name") == IString(retentionPolicyName))
    assert(policy("default") == IBoolean(true))
  }

  test("At least one parameter has to be altered") {
    await(
      database
        .createRetentionPolicy(retentionPolicyName, "1w", 1, default = false)
    )
    val result =
      await(database.alterRetentionPolicy(retentionPolicyName).either)
    result match {
      case Left(e)
          if e.isInstanceOf[InvalidRetentionPolicyParametersException] =>
      case x                                                           => fail(s"Unexpected: $x")
    }
  }

}
