package fr.ramiro.influxdbclient

import cats.Monoid
import com.softwaremill.sttp.Uri.QueryFragment.KeyValue
import com.softwaremill.sttp.Uri.{QueryFragment, QueryFragmentEncoding}
import scala.collection.immutable

trait Parameter {
  def toQueryFragments: immutable.Seq[QueryFragment]
}

object Parameter {

  implicit val semigroupParameter: Monoid[Parameter] =
    new Monoid[Parameter] {
      override def combine(x: Parameter, y: Parameter): Parameter =
        new Parameter {
          override def toQueryFragments: immutable.Seq[QueryFragment] =
            x.toQueryFragments ++ y.toQueryFragments
        }

      override val empty: Parameter = new Parameter {
        override def toQueryFragments: immutable.Seq[QueryFragment] =
          immutable.Seq.empty
      }
    }

  case class Precision(value: TimeUnit) extends Parameter {
    override def toQueryFragments: immutable.Seq[QueryFragment] =
      immutable.Seq(KeyValue("precision", value.value))
  }

  case class Epoch(value: TimeUnit) extends Parameter {
    override def toQueryFragments: immutable.Seq[QueryFragment] =
      immutable.Seq(KeyValue("epoch", value.value))
  }

  sealed abstract class TimeUnit(val value: String)
      extends Product
      with Serializable

  object TimeUnit {
    case object NANOSECONDS extends TimeUnit("ns")
    case object MICROSECONDS extends TimeUnit("u")
    case object MILLISECONDS extends TimeUnit("ms")
    case object SECONDS extends TimeUnit("s")
    case object MINUTES extends TimeUnit("m")
    case object HOURS extends TimeUnit("h")
  }

  sealed abstract class Consistency(val value: String)
      extends Parameter
      with Serializable {
    override def toQueryFragments: immutable.Seq[KeyValue] =
      immutable.Seq(KeyValue("consistency", value))
  }

  object Consistency {
    case object ONE extends Consistency("one")
    case object QUORUM extends Consistency("quorum")
    case object ALL extends Consistency("all")
    case object ANY extends Consistency("any")
  }

  case class DatabaseName(value: String) extends Parameter {
    override def toQueryFragments: immutable.Seq[QueryFragment] =
      immutable.Seq(KeyValue("db", value))
  }
  case class Query(value: String) extends Parameter {
    override def toQueryFragments: immutable.Seq[QueryFragment] =
      immutable.Seq(
        KeyValue("q", value, valueEncoding = QueryFragmentEncoding.All)
      )
  }
  case class RetentionPolicy(value: String) extends Parameter {
    override def toQueryFragments: immutable.Seq[QueryFragment] =
      immutable.Seq(KeyValue("rp", value))
  }

  implicit def optionParameter[T <: Parameter](o: Option[T]): Parameter =
    new Parameter {
      override def toQueryFragments: immutable.Seq[QueryFragment] =
        o.fold(immutable.Seq.empty[QueryFragment])(_.toQueryFragments)
    }
}
