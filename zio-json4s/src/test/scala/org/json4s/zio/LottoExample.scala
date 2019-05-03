package org.json4s
package zio

import JsonScalazZio._
import native.JsonMethods._
import org.specs2.mutable.Specification
import _root_.zio.{DefaultRuntime, IO}

object LottoExample extends Specification with DefaultRuntime {
  case class Winner(winnerId: Long, numbers: List[Int])
  case class Lotto(
      id: Long,
      winningNumbers: List[Int],
      winners: List[Winner],
      drawDate: Option[String]
  )

  val json: JValue = parse(
    """{"id":5,"winning-numbers":[2,45,34,23,7,5],"winners":[{"winner-id":23,"numbers":[2,45,34,23,3,5]},{"winner-id":54,"numbers":[52,3,12,11,18,22]}]}"""
  )

  def len(x: Int): List[Int] => Result[List[Int]] = (xs: List[Int]) => {
    if (xs.length != x)
      IO.fail(UncategorizedError("len", s"${xs.length} != $x"))
    else IO.succeed(xs)
  }

  implicit def winnerJSON: JSONR[Winner] = {
    Winner.applyJSON(
      field[Long]("winner-id"),
      validate[List[Int]]("numbers").flatMap(len(6)).provide
    )
  }

  implicit def lottoJSON: JSONR[Lotto] = Lotto.applyJSON(
    field[Long]("id"),
    (validate[List[Int]]("winning-numbers") flatMap len(6)).provide,
    field[List[Winner]]("winners"),
    field[Option[String]]("draw-date")
  )

  val winners = List(
    Winner(23, List(2, 45, 34, 23, 3, 5)),
    Winner(54, List(52, 3, 12, 11, 18, 22))
  )
  val lotto = Lotto(5, List(2, 45, 34, 23, 7, 5), winners, None)

  "LottoExample" in {
    unsafeRun(fromJSON[Lotto](json)) must_== lotto
  }
}
