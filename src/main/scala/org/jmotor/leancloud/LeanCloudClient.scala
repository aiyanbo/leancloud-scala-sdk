package org.jmotor.leancloud

import java.net.URLEncoder
import java.nio.charset.Charset

import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, AsyncHttpClientConfig, Response}
import com.typesafe.config.ConfigFactory
import org.jboss.netty.handler.codec.http.HttpHeaders
import org.jmotor.conversions.JsonConversions
import org.jmotor.conversions.JsonConversions._
import org.jmotor.leancloud.utils.MD5Utilities

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/**
 * Component:
 * Description:
 * Date: 15/5/12
 *
 * @author Andy Ai
 */
object LeanCloudClient {
  private val config = ClientConfig()
  private val rootPath: String = s"${config.host}/${config.version}"
  private val apiPath: String = s"$rootPath/classes"
  private val batchPath: String = s"$rootPath/batch"
  val DEFAULT_SKIP = 0
  val DEFAULT_LIMIT = 100

  private val asyncHttpClient: AsyncHttpClient = new AsyncHttpClient(
    new AsyncHttpClientConfig.Builder().setConnectTimeout(config.connectionTimeout).build())

  private[this] object MediaTypes {
    val json = s"application/json; charset=${Charsets.UTF_8}"
  }

  private[this] object Charsets {
    val UTF_8 = "utf-8"
  }

  def insert(body: String)(implicit className: String): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePost(s"$apiPath/$className")
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, MediaTypes.json)
    requestBuilder.setBody(body.getBytes(Charsets.UTF_8))
  }

  def insert(properties: Map[String, Any])(implicit className: String): Future[Response] = insert(JsonConversions.mapToJsonString(properties))

  def batchInsert(entities: List[Map[String, Any]])(implicit className: String): Future[Response] = batch {
    entities.map(Request(s"/${config.version}/classes/$className", "POST", _))
  }

  def delete(objectId: String)(implicit className: String): Future[Response] = execute {
    asyncHttpClient.prepareDelete(s"$apiPath/$className/$objectId")
  }

  def update(objectId: String, updates: Map[String, Any])(implicit className: String): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePut(s"$apiPath/$className/$objectId")
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, MediaTypes.json)
    requestBuilder.setBody(updates.getBytes(Charsets.UTF_8))
  }

  def update(filters: Map[String, Any], updates: Map[String, Any])(implicit className: String): Future[Response] = {
    filter(filters, keys = Some("objectId")).flatMap {
      case response if response.getStatusCode == 200 ⇒
        val objectIds = """"objectId": *"(\w+)"""".r
        val ids = for (id ← objectIds findAllMatchIn response.getResponseBody) yield id group 1
        if (ids.isEmpty) {
          Future.successful(response)
        } else {
          batch {
            ids.toTraversable.map(id ⇒ Request(s"/${config.version}/classes/$className/$id", "PUT", updates))
          }
        }
      case response ⇒ Future.successful(response)
    }
  }

  def get(objectId: String)(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className/$objectId"))

  def select(limit: Option[Int], skip: Option[Int])(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className?limit=${limit.getOrElse(DEFAULT_LIMIT)}&skip=${skip.getOrElse(DEFAULT_SKIP)}"))

  def select(order: String, limit: Option[Int], skip: Option[Int])(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className?order=$order&limit=${limit.getOrElse(DEFAULT_LIMIT)}&skip=${skip.getOrElse(DEFAULT_SKIP)}"))

  def existsObjectId(objectId: String)(implicit className: String): Future[Boolean] = get(objectId).map {
    case response if response.getStatusCode / 100 == 2 ⇒
      val emptyBody = """^\{ *\}$""".r
      response.getResponseBody match {
        case emptyBody() ⇒ false
        case _           ⇒ true
      }
    case r ⇒ throw new IllegalAccessException(s"check exists exception className: $className, objectId: $objectId")
  }

  def exists(filters: Map[String, Any])(implicit className: String): Future[Boolean] = {
    count(filters, Some(1)).map {
      case number if number > 0 ⇒ true
      case _                    ⇒ false
    }
  }

  def count(filters: Map[String, Any], limit: Option[Int])(implicit className: String): Future[Int] = {
    query(where = filters, keys = Some("objectId"), limit = limit).map {
      case response if response.getStatusCode / 100 == 2 ⇒
        val emptyBody = """^\{"results": *\[ *\]}$""".r
        response.getResponseBody match {
          case emptyBody() ⇒ 0
          case body        ⇒ """("objectId")""".r.findAllMatchIn(body).size
        }
      case _ ⇒ throw new IllegalAccessException(s"check size exception className: $className, where: $filters")
    }
  }

  def filter(filters: Map[String, Any],
             order: Option[String] = None,
             keys: Option[String] = None,
             include: Option[String] = None,
             limit: Option[Int] = None,
             skip: Option[Int] = None)(implicit className: String): Future[Response] = query(filters, order, keys, include, limit, skip)

  def query(where: String,
            order: Option[String] = None,
            keys: Option[String] = None,
            include: Option[String] = None,
            limit: Option[Int] = None,
            skip: Option[Int] = None)(implicit className: String): Future[Response] = execute {
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

  def batch(requests: Traversable[Request]): Future[Response] = {
    if (requests.isEmpty) {
      throw new NullPointerException("Requests is empty")
    }
    execute {
      val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePost(batchPath)
      requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, MediaTypes.json)
      val contents: String = requests
        .map(r ⇒ s"""{"method": "${r.method}", "path": "${r.path}", "body": ${r.body}}""")
        .reduce(_ + ", " + _)
      requestBuilder.setBody(s"""{"requests": [$contents]}""".getBytes(Charsets.UTF_8))
    }
  }

  case class Request(path: String, method: String, body: String)

  private def execute(r: AsyncHttpClient#BoundRequestBuilder): Future[Response] = {
    val result = Promise[Response]
    val timestamp: Long = System.currentTimeMillis()
    r.addHeader("X-AVOSCloud-Application-Id", config.id)
    r.addHeader("X-AVOSCloud-Request-Sign", s"${
      MD5Utilities.encode(s"$timestamp${config.key}")
    },$timestamp")
    r.addHeader(HttpHeaders.Names.USER_AGENT, "leancloud-scala-sdk-1.0.0-SNAPSHOT")
    r.setRequestTimeout(config.requestTimeout)
    r.execute(new AsyncCompletionHandler[Response]() {
      override def onCompleted(response: Response): Response = {
        result.success(response)
        response
      }

      override def onThrowable(t: Throwable): Unit = {
        result.failure(t)
      }
    })
    result.future
  }

}
