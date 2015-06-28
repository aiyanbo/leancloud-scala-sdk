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

  def insert(body: String)(implicit className: String): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePost(s"$apiPath/$className")
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
    requestBuilder.setBody(body)
  }

  def get(objectId: String)(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className/$objectId"))

  def get(limit: Option[Integer], skip: Option[Integer])(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className?limit=${limit.getOrElse(100)}&skip=${skip.getOrElse(0)}"))

  private def execute(r: AsyncHttpClient#BoundRequestBuilder): Future[Response] = {
    val timestamp: Long = System.currentTimeMillis()
    r.addHeader("X-AVOSCloud-Application-Id", id)
    r.addHeader("X-AVOSCloud-Request-Sign", s"${MD5Utilities.encode(s"$timestamp$key")},$timestamp")
    r.addHeader(HttpHeaders.Names.USER_AGENT, "leancloud-scala-sdk-1.0.0-SNAPSHOT")
  }.execute()
}
