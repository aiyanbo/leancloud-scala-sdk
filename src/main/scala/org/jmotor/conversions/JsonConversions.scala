package org.jmotor.conversions

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import scala.language.implicitConversions

/**
 * Component:
 * Description:
 * Date: 15/7/30
 *
 * @author Andy.Ai
 */
object JsonConversions {
  private val separator = ","

  implicit def mapToJsonString(filters: Map[String, Any]): String =
    s"{${
      val toJsonNodeString: PartialFunction[Any, String] = {
        case n: Number          ⇒ n.toString
        case b: Boolean         ⇒ b.toString
        case e: String          ⇒ s""""$e""""
        case d: Date            ⇒ s""""${DateTimeFormatter.ISO_INSTANT.format(d.toInstant)}""""
        case ldt: LocalDateTime ⇒ s""""${ldt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT)}""""
      }
      filters.foldLeft("")(
        (l, kv) ⇒ l + (if (l.isEmpty) "" else separator) + s""""${
          kv._1
        }":${
          toJsonNodeString.applyOrElse(kv._2, PartialFunction[Any, String] {
            case m: Map[String, _] ⇒ mapToJsonString(m)
            case i: Iterable[_]    ⇒ s"[${i.map(toJsonNodeString).reduce(_ + separator + _)}]"
            case a: Array[_]       ⇒ s"[${a.map(toJsonNodeString).reduce(_ + separator + _)}]"
            case others            ⇒ s""""${others.toString}""""
          })
        }""")
    }}"
}
