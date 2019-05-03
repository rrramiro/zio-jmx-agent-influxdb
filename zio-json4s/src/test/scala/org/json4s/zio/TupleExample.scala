package org.json4s
package zio

import org.specs2.mutable.Specification
import JsonScalazZio._
import _root_.zio.DefaultRuntime

object TupleExample extends Specification with DefaultRuntime {
  "Parse tuple from List" in {
    val json = native.JsonParser.parse(""" [1,2,3] """)
    unsafeRun(fromJSON[Tuple3[Int, Int, Int]](json)) must_== ((1, 2, 3))
  }
}
