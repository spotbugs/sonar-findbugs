import scala.util.Random

object Hello {
    def main(args: Array[String]) = {
        var x = "Hello, world"
        x = null
        
        println(x.toString)
        
        val result = Seq.fill(16)(Random.nextInt)
    }
    
    def isNumberNaN(mappedValue: Double): Double = mappedValue match {
        case Double.NaN => 0
        case _ => 1
    }
}
