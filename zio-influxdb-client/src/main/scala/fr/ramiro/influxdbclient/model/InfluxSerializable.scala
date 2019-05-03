package fr.ramiro.influxdbclient.model

trait InfluxSerializable extends Product with Serializable {
  def serialize: String
}
object InfluxSerializable {
  implicit def toSeqPoint(points: Seq[Point]): InfluxSerializable =
    SeqPoint(points)
}
