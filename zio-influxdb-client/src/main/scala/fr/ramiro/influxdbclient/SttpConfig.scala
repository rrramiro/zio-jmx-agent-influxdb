package fr.ramiro.influxdbclient

import org.asynchttpclient.config.AsyncHttpClientConfigDefaults

case class SttpConfig(
    host: String = "localhost",
    port: Int = 8086,
    https: Boolean = false,
    username: Option[String] = None,
    password: Option[String] = None,
    connectTimeout: Int = AsyncHttpClientConfigDefaults.defaultConnectTimeout(),
    requestTimeout: Int = AsyncHttpClientConfigDefaults.defaultReadTimeout(),
    acceptAnyCertificate: Boolean =
      AsyncHttpClientConfigDefaults.defaultUseInsecureTrustManager()
)
case class SttpConfigWithDatabase(database: String, base: SttpConfig)

object SttpConfigWithDatabase {
  private val influxUrlPattern =
    "^http(s)?://((\\w+):(\\w+)@)?(\\w+)(:(\\d+))?/(\\w+)$".r

  def fromUrl(influxUrl: String): Option[SttpConfigWithDatabase] = {
    influxUrl match {
      case influxUrlPattern(
          protocol,
          _,
          username,
          password,
          host,
          _,
          port,
          database
          ) =>
        Some(
          SttpConfigWithDatabase(
            database,
            SttpConfig(
              host = host,
              port = Option(port).map(_.toInt).getOrElse(8086),
              https = protocol == "s",
              username = Option(username),
              password = Option(password)
            )
          )
        )
      case _ => None
    }
  }

}
