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
    val ids = for (id <- r.findAllMatchIn(resp)) yield id group 1
    println(s"ids is empty: ${ids.isEmpty}")
    ids.foreach(id => println(id))
  }
}
