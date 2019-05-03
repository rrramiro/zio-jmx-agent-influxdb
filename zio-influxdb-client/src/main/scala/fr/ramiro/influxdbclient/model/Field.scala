package fr.ramiro.influxdbclient.model

sealed trait Field extends InfluxSerializable

case class StringField(key: String, value: String) extends Field {
  def serialize =
    Util.escapeString(key) + "=\"" + value.replaceAll("\"", "\\\\\"") + "\""
}

case class DoubleField(key: String, value: Double) extends Field {
  def serialize = Util.escapeString(key) + "=" + value
}

case class LongField(key: String, value: Long) extends Field {
  def serialize = Util.escapeString(key) + "=" + value + "i"
}

case class BooleanField(key: String, value: Boolean) extends Field {
  def serialize = Util.escapeString(key) + "=" + value
}

case class BigDecimalField(key: String, value: BigDecimal) extends Field {
  def serialize = Util.escapeString(key) + "=" + value
}
