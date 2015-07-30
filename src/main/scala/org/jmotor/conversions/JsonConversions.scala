package org.jmotor.conversions

import scala.language.implicitConversions

/**
 * Component: 
 * Description: 
 * Date: 15/7/30
 *
 * @author Andy.Ai
 */
object JsonConversions {
  implicit def mapToJsonString(filters: Map[String, Any]): String =
    s"{${
      def toJsonNodeString(v: Any) = v match {
        case n: Number => n.toString
        case b: Boolean => b.toString
        case e: String => s""""$e""""
      }
      filters.foldLeft("")(
        (l, kv) => l + (if (l.isEmpty) "" else ",") + s""""${
          kv._1
        }":${
          kv._2 match {
            case n: Number => n.toString
            case b: Boolean => b.toString
            case m: Map[String, _] => mapToJsonString(m)
            case i: Iterable[_] => s"[${i.map(toJsonNodeString).reduce(_ + "," + _)}]"
            case a: Array[_] => s"[${a.map(toJsonNodeString).reduce(_ + "," + _)}]"
            case others => s""""${others.toString}""""
          }
        }"""
      )
    }}"
}
