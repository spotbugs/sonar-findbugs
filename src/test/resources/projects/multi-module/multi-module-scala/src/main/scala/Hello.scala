import scala.util.Random

object Hello {
    def main(args: Array[String]) = {
        println("Hello, world".toString())
        
        val result = Seq.fill(16)(Random.nextInt)
    }
}
