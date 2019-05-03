package fr.ramiro.influxdbclient.model

case class Tag(key: String, value: String) extends InfluxSerializable {
  require(value != null, "Tag values may not be null")
  require(!value.isEmpty, "Tag values may not be empty")

  def serialize: String =
    Util.escapeString(key) + "=" + Util.escapeString(value)
}
