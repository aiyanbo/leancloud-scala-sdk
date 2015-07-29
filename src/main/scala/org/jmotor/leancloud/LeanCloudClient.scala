package org.jmotor.leancloud

import java.net.URLEncoder
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
  private val version: String = config.getString("leancloud.version")
  private val rootPath: String = s"${config.getString("leancloud.host")}/$version"
  private val apiPath: String = s"$rootPath/classes"
  private val batchPath: String = s"$rootPath/batch"

  def insert(body: String)(implicit className: String): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePost(s"$apiPath/$className")
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
    requestBuilder.setBody(body)
  }

  def delete(objectId: String)(implicit className: String): Future[Response] = execute {
    asyncHttpClient.prepareDelete(s"$apiPath/$className/$objectId")
  }

  def update(objectId: String, updates: Map[String, Any])(implicit className: String): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePut(s"$apiPath/$className/$objectId")
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
    requestBuilder.setBody(toJsonString(updates))
  }

  def update(filters: Map[String, Any], updates: Map[String, Any])(implicit className: String): String = {
    val response = filter(filters, keys = Some("objectId")).get()
    response match {
      case r if r.getStatusCode == 200 =>
        val resultRegex = """\{"results": *\[(.*)\]\}""".r
        val emptyBody = """\{"results": *\[ *\]\}""".r
        r.getResponseBody match {
          case emptyBody() => "[]"
          case resultRegex(objectIds) =>
            batch {
              val objectIdRegex = """\{.*"objectId" *: *"(\w+)".*""".r
              objectIds.split( """\}""").map {
                case objectIdRegex(objectId) => Request(s"/$version/classes/$className/$objectId", "PUT", toJsonString(updates))
              }.toList
            }.get().getResponseBody
          case _ => "[]"
        }
      case _ => "[]"
    }
  }

  def get(objectId: String)(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className/$objectId"))

  def get(limit: Option[Integer], skip: Option[Integer])(implicit className: String): Future[Response] =
    execute(asyncHttpClient.prepareGet(s"$apiPath/$className?limit=${limit.getOrElse(100)}&skip=${skip.getOrElse(0)}"))

  def existsObjectId(objectId: String)(implicit className: String): Boolean = get(objectId).get() match {
    case r if r.getStatusCode / 100 == 2 =>
      val emptyBody = """ *\{ *\} *""".r
      r.getResponseBody match {
        case emptyBody() => false
        case _ => true
      }
    case r => throw new IllegalAccessException(s"check exists exception className: $className, objectId: $objectId")
  }


  def exists(filters: Map[String, Any])(implicit className: String): Boolean = exists(toJsonString(filters))

  def exists(where: String)(implicit className: String): Boolean = {
    query(where = where, keys = Some("objectId"), limit = Some(1)).get() match {
      case r if r.getStatusCode / 100 == 2 =>
        val emptyBody = """ *\{"results":\[ *\]} *""".r
        r.getResponseBody match {
          case emptyBody() => false
          case _ => true
        }
      case r => throw new IllegalAccessException(s"check exists exception className: $className, where: $where")
    }
  }

  def filter(filters: Map[String, Any],
             order: Option[String] = None,
             keys: Option[String] = None,
             include: Option[String] = None,
             limit: Option[Integer] = None,
             skip: Option[Integer] = None)(implicit className: String): Future[Response] = query(toJsonString(filters), order, keys, include, limit, skip)

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
          case Some(o) => s"&order=$o"
          case None => ""
        }
      }${
        keys match {
          case Some(k) => s"&keys=$k"
          case None => ""
        }
      }${
        include match {
          case Some(i) => s"&include=$i"
          case None => ""
        }
      }${
        limit match {
          case Some(l) => s"&limit=$l"
          case None => ""
        }
      }${
        skip match {
          case Some(s) => s"&skip=$s"
          case None => ""
        }
      }"
    }
  }

  def batch(requests: List[Request]): Future[Response] = execute {
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.preparePost(batchPath)
    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
    val contents: String = requests
      .map(r => s"""{"method": "${r.method}", "path": "${r.path}", "body": ${r.body}}""")
      .foldLeft("")(
        (l, r) => l + (if (l.isEmpty) "" else ",") + r
      )
    requestBuilder.setBody( s"""{"requests": [$contents]}""")
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

  private def toJsonString(filters: Map[String, Any]): String =
    s"{${
      filters.foldLeft("")(
        (l, kv) => l + (if (l.isEmpty) "" else ",") + s""""${
          kv._1
        }":${
          if (kv._2.isInstanceOf[Number] || kv._2.isInstanceOf[Boolean])
            kv._2.toString
          else s""""${kv._2.toString}""""
        }"""
      )
    }}"
}
