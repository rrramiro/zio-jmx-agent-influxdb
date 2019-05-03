package fr.ramiro.influxdbclient

import org.json4s._
import org.json4s.zio.JsonScalazZio._
import org.json4s.native.JsonMethods._
import _root_.zio.DefaultRuntime

class QueryResultSuite extends CustomTestSuite with DefaultRuntime {

  test("Construct result") {
    val data =
      """{"results":[{"series":[{"name":"databases","columns":["name"],"values":[["_internal"]],"tags":{"tag": "value"}}]}]}"""
    val queryResult = await(QueryResult.fromJson(data))

    assert(queryResult.series.length == 1)
  }

  test("Construct record") {
    val data = parse("""[1, "second value"]""")
    val record = await(
      QueryResult
        .constructRecord(Map("first_metric" -> 0, "second_metric" -> 1), data)
        .mapError(e => new Exception(e.toString))
    )
    assert(record(0) == IInt(1))
    assert(record("first_metric") == IInt(1))
    assert(record(1) == IString("second value"))
    assert(record("second_metric") == IString("second value"))
  }

  test("Null values are supported") {
    val data = parse("""[1, null]""")
    val record = await(
      QueryResult
        .constructRecord(Map("first_metric" -> 0, "second_metric" -> 1), data)
        .mapError(e => new Exception(e.toString))
    )
    assert(record(1) == INull)
    assert(record("second_metric") == INull)
  }

  test(
    "Constructing a record with unsupported types throws a MalformedResponseException"
  ) {
    val data = parse("""[{}, "second value"]""")
    val record = await(
      QueryResult
        .constructRecord(Map("first_metric" -> 0, "second_metric" -> 1), data)
        .either
    )
    record match {
      case Left(e) if e.isInstanceOf[UncategorizedError] =>
      case x                                             => fail(s"Unexpected: $x")
    }
  }

  test("Construct series") {
    val data = parse(
      """{"name":"test_series","columns":["column1", "column2", "column3"],"values":[["value1", 2, true]],"tags":{"tag": "value"}}"""
    )
    val series = await(
      QueryResult
        .constructSeries(data)
        .mapError(e => new Exception(e.toString))
    )

    assert(series.name == "test_series")
    assert(series.columns == List("column1", "column2", "column3"))
    assert(series.records.length == 1)
    assert(series.tags.values.size == 1)

    val record = series.records.head
    assert(record("column1") == IString("value1"))
    assert(record("column2") == IInt(2))
    assert(record("column3") == IBoolean(true))
    assert(record.values.length == 3)
  }

  test("Construct series without a name") {
    val data = parse(
      """{"columns":["column1", "column2", "column3"],"values":[["value1", 2, true]],"tags":{"tag": "value"}}"""
    )
    val series = await(
      QueryResult
        .constructSeries(data)
        .mapError(e => new Exception(e.toString))
    )

    assert(series.name == "")
    assert(series.columns == List("column1", "column2", "column3"))
    assert(series.records.length == 1)
    assert(series.tags.values.size == 1)
  }

  test("Construct series without values") {
    val data = parse("""{"columns":["column1", "column2", "column3"]}""")
    val series = await(
      QueryResult
        .constructSeries(data)
        .mapError(e => new Exception(e.toString))
    )

    assert(series.name == "")
    assert(series.columns == List("column1", "column2", "column3"))
    assert(series.records.isEmpty)
  }

  test(
    "Constructing a series with unsupported types throws a MalformedResponseException"
  ) {
    val data = parse("""{"name":"test_series","columns":[1],"values":[]}""")
    val series = await(QueryResult.constructSeries(data).either)
    series match {
      case Left(e) if e.isInstanceOf[UnexpectedJSONError] =>
      case x                                              => fail(s"Unexpected: $x")
    }
  }

  test(
    "Constructing a series with unsupported tag types throws a MalformedResponseException"
  ) {
    val data = parse(
      """{"name":"test_series","columns":["columns1"],"values":[["value1"]],"tags": {"tag": []}}"""
    )
    val series = await(QueryResult.constructSeries(data).either)
    series match {
      case Left(e) if e.isInstanceOf[UncategorizedError] =>
      case x                                             => fail(s"Unexpected: $x")
    }
  }

  test("Value series can be accessed by name, position and as a list") {
    val data = parse(
      """{"name":"n","columns":["column1", "column2"],"values":[[1, 2],[2, 3],[3, 4],[4, 5]]}"""
    )
    val series = await(
      QueryResult
        .constructSeries(data)
        .mapError(e => new Exception(e.toString))
    )

    assert(series.points("column1") == List(1, 2, 3, 4).map(IInt(_)))
    assert(series.points(0) == List(1, 2, 3, 4).map(IInt(_)))
    assert(series.points("column2") == List(2, 3, 4, 5).map(IInt(_)))
    assert(series.points(1) == List(2, 3, 4, 5).map(IInt(_)))
    assert(
      series.values == List(List(1, 2), List(2, 3), List(3, 4), List(4, 5))
        .map(_.map(IInt(_)))
    )
  }

  test("Tags can be accessed by name and position") {
    val data = parse(
      """{"columns":["column1", "column2", "column3"],"values":[["value1", 2, true]],"tags":{"tag": "value"}}"""
    )
    val series = await(
      QueryResult
        .constructSeries(data)
        .mapError(e => new Exception(e.toString))
    )

    assert(series.tags("tag") == IString("value"))
    assert(series.tags(0) == IString("value"))
  }

  test("Tags can be defined as strings, numbers or booleans") {
    val data = parse(
      """{"columns":["column1", "column2", "column3"],"values":[["value1", 2, true]],"tags":{"tag1": "value", "tag2": 10, "tag3": true}}"""
    )
    val series = await(
      QueryResult
        .constructSeries(data)
        .mapError(e => new Exception(e.toString))
    )

    assert(series.tags("tag1") == IString("value"))
    assert(series.tags(0) == IString("value"))
    assert(series.tags("tag2") == IInt(10))
    assert(series.tags(1) == IInt(10))
    assert(series.tags("tag3") == IBoolean(true))
    assert(series.tags(2) == IBoolean(true))
  }

  test("Valid error responses throws an ErrorResponseException") {
    val data = """{"results":[{"error":"database not found: _test"}]}"""
    val result = await(QueryResult.fromJson(data).either)
    result match {
      case Left(e) if e.isInstanceOf[ErrorResponseException] =>
      case x                                                 => fail(s"Unexpected: $x")
    }
  }

  test("Empty responses return a QueryResponse with no series") {
    val data = """{"results":[{}]}"""
    val response = await(QueryResult.fromJson(data))
    assert(response.series.isEmpty)
  }

  test("Multiple results are parsed correctly") {
    val data =
      """{"results":[{"series":[{"name":"databases","columns":["name"],"values":[["_internal"]],"tags":{"tag": "value"}}]},{"series":[{"name":"databases_2","columns":["name"],"values":[["_internal"]],"tags":{"tag": "value"}}]}]}"""
    val queryResults = await(QueryResult.fromJsonMulti(data))
    assert(queryResults.length == 2)
    assert(queryResults(0).series.head.name == "databases")
    assert(queryResults(1).series.head.name == "databases_2")
  }
}
