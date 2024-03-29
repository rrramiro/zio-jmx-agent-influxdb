package org.json4s
package zio

import org.specs2.mutable.Specification
import _root_.zio.DefaultRuntime
import org.json4s.native.JsonMethods._
import org.json4s.native._
import JsonScalazZio._

object Example extends Specification with DefaultRuntime {

  case class Address(street: String, zipCode: String)
  case class Person(name: String, age: Int, address: Address)

  "Parse address in an Applicative style" in {
    val json = parse(""" {"street": "Manhattan 2", "zip": "00223" } """)
    val a1 =
      (field[String]("street")(json), field[String]("zip")(json)).map2(Address)
    val a2 = (field[String]("street")(json), field[String]("zip")(json)).map2 {
      Address
    }
    val a3 =
      Address.applyJSON(field[String]("street"), field[String]("zip"))(json)
    unsafeRun(a2) mustEqual Address("Manhattan 2", "00223")
    unsafeRun(a3) mustEqual unsafeRun(a2)
    unsafeRun(a1) mustEqual unsafeRun(a2)
  }

  "Failed address parsing" in {
    val json = parse(""" {"street": "Manhattan 2", "zip": "00223" } """)
    val a = (field[String]("streets")(json), field[String]("zip")(json)).map2 {
      Address
    }
    unsafeRun(a.either) must beLeft(NoSuchFieldError("streets", json): Error)
  }

  "Parse Person with Address" in {
    implicit def addrJSON: JSONR[Address] =
      (json: JValue) =>
        Address.applyJSON(field[String]("street"), field[String]("zip"))(json)

    val p = parse(
      """ {"name":"joe","age":34,"address":{"street": "Manhattan 2", "zip": "00223" }} """
    )
    val person = Person.applyJSON(
      field[String]("name"),
      field[Int]("age"),
      field[Address]("address")
    )(p)
    unsafeRun(person) mustEqual Person(
      "joe",
      34,
      Address("Manhattan 2", "00223")
    )
  }

  "Format Person with Address" in {
    implicit def addrJSON: JSONW[Address] = new JSONW[Address] {
      def write(a: Address): JValue =
        makeObj(
          ("street" -> toJSON(a.street)) :: ("zip" -> toJSON(a.zipCode)) :: Nil
        )
    }

    val p = Person("joe", 34, Address("Manhattan 2", "00223"))
    val json = makeObj(
      ("name" -> toJSON(p.name)) ::
        ("age" -> toJSON(p.age)) ::
        ("address" -> toJSON(p.address)) :: Nil
    )

    val show = renderJValue _ andThen compactJson

    show(json) mustEqual
      """{"name":"joe","age":34,"address":{"street":"Manhattan 2","zip":"00223"}}"""
  }

  "Parse Map" in {
    val json = parse(""" {"street": "Manhattan 2", "zip": "00223" } """)
    unsafeRun(fromJSON[Map[String, String]](json)) mustEqual Map(
      "street" -> "Manhattan 2",
      "zip" -> "00223"
    )
  }

  "Format Map" in {
    toJSON(Map("street" -> "Manhattan 2", "zip" -> "00223")) mustEqual
      parse("""{"street":"Manhattan 2","zip":"00223"}""")
  }
}
