import java.util.concurrent.Future

import com.ning.http.client.Response
import org.jmotor.leancloud.LeanCloudClient._
import org.scalatest.FunSuite

/**
 * Component:
 * Description:
 * Date: 15/5/12
 * @author Andy Ai
 */
class AppTest extends FunSuite {
  test("Make request") {
    val f: Future[Response] = get("stacks", "scala")
    val response: Response = f.get()
    println(response.getResponseBody)
  }
}
