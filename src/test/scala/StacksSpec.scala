import org.scalatest.FunSuite

/**
 * Component:
 * Description:
 * Date: 15/5/12
 * @author Andy Ai
 */
class StacksSpec extends FunSuite {

  test("Match results") {
    val str = s"""{"results": ["123456","dfjkdfjkdfj"]}"""
    val r = """\{"results": *\[(.*)\]\}""".r
    str match {
      case r(ids) => println(ids)
      case _ => println("non")
    }
  }
}
