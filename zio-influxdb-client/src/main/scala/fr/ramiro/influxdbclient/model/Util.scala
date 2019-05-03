package fr.ramiro.influxdbclient.model

protected[influxdbclient] object Util {
  def escapeString(str: String): String = str.replaceAll("([ ,=])", "\\\\$1")
}
