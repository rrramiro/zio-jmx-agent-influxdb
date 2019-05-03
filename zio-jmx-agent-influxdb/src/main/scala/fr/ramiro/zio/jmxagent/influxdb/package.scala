package fr.ramiro.zio.jmxagent

import fr.ramiro.influxdbclient.SttpConfigWithDatabase
import zio.duration.Duration

package object influxdb {
  case class PremainConfiguration(
      delay: Duration,
      waitForCustomMBeanServer: Boolean,
      customMBeanServerTimeout: Duration
  )

  case class JmxInfluxConfigTmp(
      url: String,
      queries: List[JmxQuery]
  )

  case class JmxInfluxConfig(
      influxdbConfig: SttpConfigWithDatabase,
      queries: List[JmxQuery]
  )

  case class JmxQuery(
      objectName: String,
      attributes: List[String],
      interval: Duration
  )

}
