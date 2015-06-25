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
class StacksSpec extends FunSuite {
  implicit val className: String = "stacks"

  test("Get stack") {
    val f: Future[Response] = get("scala")
    val response: Response = f.get()
    println(response.getResponseBody)
    assertResult(200)(response.getStatusCode)
  }
}
