package org.jmotor

import com.typesafe.config.ConfigFactory

/**
 * Component:
 * Description:
 * Date: 16/3/25
 *
 * @author Andy Ai
 */
final case class ClientConfig(id: String, key: String, host: String, version: String, connectionTimeout: Int, requestTimeout: Int)

object ClientConfig {
  def apply(): ClientConfig = {
    val config = ConfigFactory.load()
    val host = config.getString("leancloud.host")
    val id = config.getString("leancloud.app-id")
    val key = config.getString("leancloud.app-key")
    val version = config.getString("leancloud.version")
    val requestTimeout = config.getInt("leancloud.timeout.request")
    val connectionTimeout = config.getInt("leancloud.timeout.connection")
    new ClientConfig(id, key, host, version, connectionTimeout, requestTimeout)
  }
}