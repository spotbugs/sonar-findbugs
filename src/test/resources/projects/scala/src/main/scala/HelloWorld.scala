object Hello {
    def main(args: Array[String]) = {
        var x = "Hello, world"
        x = null
        
        println(x.toString)
    }
    
    def isNumberNaN(mappedValue: Double): Double = mappedValue match {
        case Double.NaN => 0
        case _ => 1
    }
}