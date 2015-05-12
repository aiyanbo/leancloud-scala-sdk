package org.jmotor.leancloud.utils

import java.security.MessageDigest

/**
 * Component:
 * Description:
 * Date: 15/5/12
 * @author Andy Ai
 */
object MD5Utilities {

  def encode(plainText: String): String = {
    try {
      val messageDigest: MessageDigest = MessageDigest.getInstance("MD5")
      messageDigest.update(plainText.getBytes("UTF-8"))
      val bytes = messageDigest.digest()
      val result: StringBuilder = new StringBuilder(bytes.length * 12)
      for (b <- bytes) {
        result.append(Integer.toHexString((0x000000ff & b) | 0xffffff00).substring(6))
      }
      result.toString();
    } catch {
      case e: Throwable => throw new RuntimeException("MD5 encode failure.")
    }
  }
}
