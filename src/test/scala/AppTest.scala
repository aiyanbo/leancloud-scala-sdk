import java.util.concurrent.Future

import com.ning.http.client.{AsyncHttpClient, Response}
import com.typesafe.config.{Config, ConfigFactory}
import org.jmotor.leancloud.utils.MD5Utilities
import org.scalatest.FunSuite

/**
 * Component:
 * Description:
 * Date: 15/5/12
 * @author Andy Ai
 */
class AppTest extends FunSuite {
  test("Make request") {
    val config: Config = ConfigFactory.load()
    val asyncHttpClient: AsyncHttpClient = new AsyncHttpClient()
    val requestBuilder: AsyncHttpClient#BoundRequestBuilder = asyncHttpClient.prepareGet("https://leancloud.cn:443/1.1/classes/stacks/scala")
    val timestamp: Long = System.currentTimeMillis()
    requestBuilder.addHeader("X-AVOSCloud-Application-Id", config.getString("leancloud.app-id"))
    requestBuilder.addHeader("X-AVOSCloud-Request-Sign", s"${MD5Utilities.encode(s"$timestamp${config.getString("leancloud.app-key")}")},$timestamp")
    val f: Future[Response] = requestBuilder.execute()
    val response: Response = f.get()
    println(response.getResponseBody)
  }
}
