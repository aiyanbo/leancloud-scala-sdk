import org.jmotor.conversions.JsonConversions._
import org.scalatest.FunSuite

/**
 * Component:
 * Description:
 * Date: 15/5/12
 * @author Andy Ai
 */
class StacksSpec extends FunSuite {

  test("Match objectId") {
    val resp = """{"results": [{"createdAt":"2015-07-29T09:33:58.189Z","updatedAt":"2015-07-29T09:33:58.189Z","objectId":"55b89e0600b0ed9c142e221a"}, {"createdAt":"2015-07-29T09:33:58.189Z","updatedAt":"2015-07-29T09:33:58.189Z","objectId":"55b89e0600b0ed9c142e221c"}]}"""
    val r = """"objectId": *"(\w+)"""".r
    val ids = for (id ← r.findAllMatchIn(resp)) yield id group 1
    assertResult(false)(ids.isEmpty)
    assertResult("55b89e0600b0ed9c142e221a, 55b89e0600b0ed9c142e221c")(ids.reduce(_ + ", " + _))
  }

  test("To json string") {
    val map = Map("username" -> "aiyanbo", "objectId" -> "55b89e0600b0ed9c142e221a", "age" -> 15, "hobbies" -> List("java", "scala", "clojure"), "others" -> Map("gate" -> 3))
    assertResult("""{"hobbies": ["java", "scala", "clojure"], "username": "aiyanbo", "others": {"gate": 3}, "age": 15, "objectId": "55b89e0600b0ed9c142e221a"}""")(mapToJsonString(map))
  }

}
