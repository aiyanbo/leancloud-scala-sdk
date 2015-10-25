package org.jmotor.leancloud

import java.net.URLEncoder
import java.util.concurrent.Future

import com.ning.http.client.{ AsyncHttpClient, Response }
import com.typesafe.config.ConfigFactory
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jmotor.conversions.JsonConversions
import org.jmotor.conversions.JsonConversions._
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
  private val version: String = config.getString("leancloud.version")
  private val rootPath: String = s"${config.getString("leancloud.host")}/$version"
  private val apiPath: String = s"$rootPath/classes"
  private val batchPath: String = s"$rootPath/batch"

  def insert(body: String)(implicit className: String): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePost(s"$apiPath/$className")
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
    requestBuilder.setBody(body)
  }

  def insert(properties: Map[String, Any])(implicit className: String): Future[Response] = insert(JsonConversions.mapToJsonString(properties))

  def delete(objectId: String)(implicit className: String): Future[Response] = execute {
    asyncHttpClient.prepareDelete(s"$apiPath/$className/$objectId")
  }

  def update(objectId: String, updates: Map[String, Any])(implicit className: String): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePut(s"$apiPath/$className/$objectId")
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
    requestBuilder.setBody(updates)
  }

  def update(filters: Map[String, Any], updates: Map[String, Any])(implicit className: String): Future[Response] = {
    val response = filter(filters, keys = Some("objectId"))
    response.get() match {
      case r if r.getStatusCode == 200 ⇒
        val objectIds = """"objectId": *"(\w+)"""".r
        val ids = for (id ← objectIds findAllMatchIn r.getResponseBody) yield id group 1
        if (ids.isEmpty) {
          response
        } else {
          batch {
            ids.map(id ⇒ Request(s"/$version/classes/$className/$id", "PUT", updates))
          }
        }
      case _ ⇒ response
    }
  }

  def get(objectId: String)(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className/$objectId"))

  def select(limit: Option[Integer], skip: Option[Integer])(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className?limit=${limit.getOrElse(100)}&skip=${skip.getOrElse(0)}"))

  def select(order: String, limit: Option[Integer], skip: Option[Integer])(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className?order=$order&limit=${limit.getOrElse(100)}&skip=${skip.getOrElse(0)}"))

  def existsObjectId(objectId: String)(implicit className: String): Boolean = get(objectId).get() match {
    case r if r.getStatusCode / 100 == 2 ⇒
      val emptyBody = """^\{ *\}$""".r
      r.getResponseBody match {
        case emptyBody() ⇒ false
        case _           ⇒ true
      }
    case r ⇒ throw new IllegalAccessException(s"check exists exception className: $className, objectId: $objectId")
  }

  def exists(filters: Map[String, Any])(implicit className: String): Boolean = {
    query(where = filters, keys = Some("objectId"), limit = Some(1)).get() match {
      case r if r.getStatusCode / 100 == 2 ⇒
        val emptyBody = """^\{"results": *\[ *\]}$""".r
        r.getResponseBody match {
          case emptyBody() ⇒ false
          case _           ⇒ true
        }
      case r ⇒ throw new IllegalAccessException(s"check exists exception className: $className, where: $filters")
    }
  }

  def count(filters: Map[String, Any], limit: Option[Int])(implicit className: String): Int = {
    query(where = filters, keys = Some("objectId"), limit = Some(1)).get() match {
      case r if r.getStatusCode / 100 == 2 ⇒
        val emptyBody = """^\{"results": *\[ *\]}$""".r
        r.getResponseBody match {
          case emptyBody() ⇒ 0
          case body        ⇒ """("objectId")""".r.findAllMatchIn(body).size
        }
      case r ⇒ throw new IllegalAccessException(s"check size exception className: $className, where: $filters")
    }
  }

  def filter(filters: Map[String, Any],
             order: Option[String] = None,
             keys: Option[String] = None,
             include: Option[String] = None,
             limit: Option[Integer] = None,
             skip: Option[Integer] = None)(implicit className: String): Future[Response] = query(filters, order, keys, include, limit, skip)

  def query(where: String,
            order: Option[String] = None,
            keys: Option[String] = None,
            include: Option[String] = None,
            limit: Option[Integer] = None,
            skip: Option[Integer] = None)(implicit className: String): Future[Response] = execute {
    asyncHttpClient.prepareGet {
      s"$apiPath/$className?where=${
        URLEncoder.encode(where, "utf-8")
      }${
        order match {
          case Some(o) ⇒ s"&order=$o"
          case None    ⇒ ""
        }
      }${
        keys match {
          case Some(k) ⇒ s"&keys=$k"
          case None    ⇒ ""
        }
      }${
        include match {
          case Some(i) ⇒ s"&include=$i"
          case None    ⇒ ""
        }
      }${
        limit match {
          case Some(l) ⇒ s"&limit=$l"
          case None    ⇒ ""
        }
      }${
        skip match {
          case Some(s) ⇒ s"&skip=$s"
          case None    ⇒ ""
        }
      }"
    }
  }

  def batch(requests: Iterator[Request]): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePost(batchPath)
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
    val contents: String = requests
      .map(r ⇒ s"""{"method": "${r.method}", "path": "${r.path}", "body": ${r.body}}""")
      .reduce(_ + ", " + _)
    requestBuilder.setBody(s"""{"requests": [$contents]}""")
  }

  case class Request(path: String, method: String, body: String)

  private def execute(r: AsyncHttpClient#BoundRequestBuilder): Future[Response] = {
    val timestamp: Long = System.currentTimeMillis()
    r.addHeader("X-AVOSCloud-Application-Id", id)
    r.addHeader("X-AVOSCloud-Request-Sign", s"${
      MD5Utilities.encode(s"$timestamp$key")
    },$timestamp")
    r.addHeader(HttpHeaders.Names.USER_AGENT, "leancloud-scala-sdk-1.0.0-SNAPSHOT")
  }.execute()

}
