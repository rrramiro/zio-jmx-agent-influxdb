package fr.ramiro.influxdbclient

import java.net.{DatagramPacket, DatagramSocket, InetSocketAddress}

import fr.ramiro.influxdbclient.model.InfluxSerializable
import zio.IO

class UdpClient protected[influxdbclient] (host: String, port: Int)
    extends AutoCloseable {

  val socket = new DatagramSocket()
  val address = new InetSocketAddress(host, port)

  def write(point: InfluxSerializable) = send(point.serialize.getBytes)

  def close(): Unit = socket.close()

  private def send(payload: Array[Byte]) = {
    IO.effect(socket.send(new DatagramPacket(payload, payload.length, address)))
  }
}
