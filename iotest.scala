import scalax.io._
import Resource._
 

 object IoTest{
	def main(args: Array[String]){
		// see codec example for why codec is required
		implicit val codec = Codec.UTF8
		 
		val input = Resource.fromFile("../products.txt")
		 
		// by default the line terminator is stripped and is
		// auto detected
		val lin = input.lines().toList // foreach println _
		 
		println(lin.length )
		 lin foreach println _
		// val output:Output = Resource.fromFile("delme.txt")
	 
		// output.write("Joe was here 2")(Codec.UTF8)
		
	}
}