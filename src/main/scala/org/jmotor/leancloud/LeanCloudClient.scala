package org.jmotor.leancloud

import java.util.concurrent.Future

import com.ning.http.client.{AsyncHttpClient, Response}
import com.typesafe.config.ConfigFactory
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jmotor.leancloud.utils.MD5Utilities

/**
 * Component:
 * Description:
 * Date: 15/5/12
 * @author Andy Ai
 */
object LeanCloudClient {
  private val config = ConfigFactory.load()
  private val id = config.getString("leancloud.app-id")
  private val key = config.getString("leancloud.app-key")
  private val asyncHttpClient: AsyncHttpClient = new AsyncHttpClient()
  private val apiPath = s"${config.getString("leancloud.host")}/${config.getString("leancloud.version")}/classes"

  def insert(className: String, body: String): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePost(s"$apiPath/$className")
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
    requestBuilder.setBody(body)
  }

  def get(className: String, objectId: String): Future[Response] = execute(asyncHttpClient.prepareGet(s"$apiPath/$className/$objectId"))

  private def execute(requestBuilder: AsyncHttpClient#BoundRequestBuilder): Future[Response] = {
    val timestamp: Long = System.currentTimeMillis()
    requestBuilder.addHeader("X-AVOSCloud-Application-Id", id)
    requestBuilder.addHeader("X-AVOSCloud-Request-Sign", s"${MD5Utilities.encode(s"$timestamp$key")},$timestamp")
    requestBuilder.execute()
  }
}
