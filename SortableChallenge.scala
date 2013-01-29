import scala.util.parsing.json._
import scalax.io._
import scalax.file.Path
import scalax.io.StandardOpenOption._


// Classes used when partsing the input files
class Listing(val title:String, val manufacturer:String, val currency:String, val price:String){
	override def toString(): String =  title 
	
}

class Product(val name:String, val manufacturer:String, val family:String, val model:String){
	override def toString(): String =  name 
}

class Result(val productName:String, val listings:List[Listing]){
	override def toString(): String =  {
		val sb  = new StringBuilder 
		sb.append(productName).append('\n')
		for(listing <- this.listings){
			sb.append('\t').append(listing).append('\n')
		}
		sb.toString
	} 
	
}


// Product tree built using objects. Each level of the tree contains a list of the sub element keys
// sorted in longest name first, as well as a map with the sub elements. The reason a sorted map was not used
// is that sorting the map based on string lenght of the keys only kept one key of a given length. 
// Example, see how the first element is lost: 
// 		val t4 = TreeMap(("1","a"),("2","b"))(Ordering.by( -_.length))
//		t4: scala.collection.immutable.TreeMap[String,String] = Map(2 -> b)
//
// The 'longest-first' ordering allows for a greedy search when matching product attributes to listings.


class Family(val products:List[Product]){
	val modelOrder = products.map(_.model.toLowerCase).sortWith(_.length > _.length).distinct
	val models = ( for(m <- modelOrder) yield (m, products.find(_.model.toLowerCase == m).get ) ).toMap

	override def toString(): String =  {
		val sb  = new StringBuilder 
		for(model <- this.modelOrder){
			sb.append(model).append(" : ")
			sb.append(models(model)).append('\n')
		}
		sb.toString
	}
	
}

class Manufacturer(products:List[Product]){
	val famOrder = products.map(_.family.toLowerCase).sortWith(_.length > _.length).distinct
	val families = ( for(f <- famOrder) yield (f, new Family(products.filter(_.family.toLowerCase == f)) ) ).toMap

	override def toString(): String =  {
		val sb  = new StringBuilder 
		for(fam <- this.famOrder){
			sb.append(fam).append('\n')
			sb.append(families(fam))
		}
		sb.toString
	} 
	
}

class ProductTree(products:List[Product]){
	val manuOrder = products.map(_.manufacturer.toLowerCase).sortWith(_.length > _.length).distinct
	val manufacturers = ( for(m <- manuOrder) yield (m, new Manufacturer(products.filter(_.manufacturer.toLowerCase == m))) ).toMap

	def matchListing(listing:Listing):Option[String] ={

		val listingManu = listing.manufacturer.toLowerCase

		// check for an exact match in manufacturer
		val matchedManu:Option[String] =
			if(manufacturers.contains(listingManu))
				Some(listingManu)
			// maybe the manufacturer is not an exact match, then try a greedy match of manufacturers against the listing
			// manufacturer or in case the listing has no manufacturer, look in the title
			else
				manuOrder.find(manu => listingManu.contains(manu) || ( listingManu == "" && listing.title.toLowerCase.contains(manu) ) )
				//manuOrder.find(_.length == 3)
				//None

		matchedManu match {
			case Some(manu:String) => matchFamily(manufacturers(manu), listing)
			case _ => None
		}

	}

	private def matchFamily(manufacturer:Manufacturer, listing:Listing): Option[String] = {
		// TODO Implement
		
		val listingTitle = listing.title.toLowerCase

		// get best model match(es) per family
		val matchedhModels = 
		for( fam <- manufacturer.famOrder ;
			models = getMatchingModels(manufacturer.families(fam).modelOrder, listingTitle)
			if models.length > 0 )
			yield (fam, models )

		val numFamiliesInListing = manufacturer.famOrder.filter(listingTitle contains _ ).length

		// mm._1 is family, _2 is list of models
		val matchedFamilies = 
			matchedhModels.filter( 
				mm => (
					  JsonHelper.unknownFamily ==  mm._1 
					  || listingTitle.contains( mm._1 ) 
					  || numFamiliesInListing == 0
					  )
					&& mm._2.length == 1
				).map(mm => (mm._1, mm._2(0)) ) // all models are lenght 1 at this point so extract from list
		
		// if there are still multiple matches then trim, return most likely
		if(matchedFamilies.length >= 1){
			val matched = matchedFamilies.sortWith(_._2.length > _._2.length)(0)    // the longer model nr wins in this case, family is uncertain
			val matchedProduct = manufacturer.families( matched._1 ).models(matched._2)
			Some(matchedProduct.name)  // return the product name
		}
		else
			None
	}

	// Recursive function that searches through the listing title for models. The list of models should
	// be ordered longest to shortest to ensure a greedy search. Want to ensure that 'X100S' is found before 'X100'
	private def getMatchingModels(models:List[String], listingTitle:String ): List[String] = {
		
		val model = models.head
		val modelVariations = List(
			model, 
			model.replace(' ','-'), 
			model.replace('-', ' '), 
			model.filter(_ != '-'), 
			model.replace(' ','_'), 
			model.replace('_', ' '), 
			model.filter( _ != '_'), 
			model.filter( _ != ' ')
			).distinct

		// collect all model variations found, these all represent the same model
		val variationsFound = modelVariations.filter( listingTitle contains _ )


		val variationsChecked = variationsFound.filter(checkEnds(listingTitle, _))
			

		// remove all the model variations from the listing title to see if other models still present
		val listingTitleAdjusted = removeSubstrings(listingTitle, variationsChecked)
		

		//TODO can this section below be made prettier? tired.....
		val otherMatches =  if (models.tail.length >0) 
			getMatchingModels(models.tail, listingTitleAdjusted)
		else 
			List()

	    if ( variationsChecked.length >0 )	
			model :: otherMatches
		else 
			otherMatches
	}

	private def removeSubstrings(input:String, substringsToRemove:List[String]):String = {
		if (substringsToRemove.length !=0) 
			removeSubstrings(input.replace(substringsToRemove.head,""),substringsToRemove.tail) 
		else input
	}

	private def checkEnds(listingTitle:String,variation:String):Boolean = {
		    val startIndex = listingTitle.indexOf(variation)
			val endIndex = startIndex + variation.length
			val startOk = if(startIndex > 0 ) notAlpaNumeric(listingTitle(startIndex-1)) else true
			val endOk = if(endIndex < listingTitle.length) notAlpaNumeric(listingTitle(endIndex)) else true 
			startOk && endOk
	}


	private def notAlpaNumeric(toCheck:Char):Boolean ={
		// thought about using regex but it seems overkill
		!('a' to 'z' contains toCheck) && !('0' to '9' contains toCheck) 
	}

	override def toString(): String =  {
		val sb  = new StringBuilder 
		for (manu <- this.manuOrder){
			sb.append(manu).append('\n')
			sb.append(manufacturers(manu))
		}
		sb.toString
	} 
	

}
// End Product tree


object SortableChallenge{
	def main(args: Array[String]){

		if(args.length < 2){
			println("Usage SortableChallenge <productFile> <listingFile>")
			return
		}

		val poductsFileName = args(0)
		val listingsFileName = args(1)

		//TODO: The reading and opening of the two files has a few lines of ideantical code, this shold be refactored 
		// to be in a single method somehow. Need to understand scala generics and function params better to do this refactor properly.
		// main concern is that the .close call should be handled by the refactored function.
		println("Imporing products.....")
		val productsLines = Resource.fromFile(poductsFileName).lines()
		val products = productsLines.toList.map(JsonHelper.productFromJson(_)).flatten
		//productsFile.close()

		println("Importing listings......")
		val listingsLines = Resource.fromFile(listingsFileName).lines()
		//val listingsFile = io.Source.fromFile(listingsFileName)
		//val listings = listingsFile.getLines.toList.map(JsonHelper.listingFormJson(_)).flatten
		val listings = listingsLines.toList.map(JsonHelper.listingFormJson(_)).flatten
		//listingsFile.close()

		println("Processing......")
		val productTree =  new ProductTree(products)

		val matchedProducts = listings.map(productTree.matchListing(_) )
		val zipped = matchedProducts.zip(listings).filter( r=> r._1.isDefined )
								

		val results = for(p<- matchedProducts.flatten.distinct)
			yield(new Result(p, zipped.filter( r => r._1.get == p ).map( r => r._2) ))
		
		val resultsJson = JsonHelper.resultsToJson(results)

		Path("results.txt").outputStream(WriteTruncate:_*).write(resultsJson)

			
	}
	
}

// Singleton used to hold helper methods for json specific work 
object JsonHelper{
	val unknownFamily = "unknownfamily"
	def productFromJson(line:String):Option[Product] = {
		JSON.parseFull(line) collect {
			case x:Map[String,String] => 
				new Product(x("product_name"), x("manufacturer"), if (x.contains("family")) x("family") else unknownFamily , x("model"))
		}
	}

	def listingFormJson(line:String):Option[Listing] = {
		JSON.parseFull(line) collect {
			case x:Map[String,String] => new Listing(x("title"), x("manufacturer"), x("currency"), x("price"))
		}
	}

    //Since the scala json class only does parsing, rolling own implementation of creatig json
    //If more would be needed, a 3rd party library should be used, plenty available
	def resultsToJson(results:List[Result]):String ={
		val sb = new StringBuilder 
		for (r<-results){
			sb.append("{\"product_name\": \"")
			  .append( escapeChars(r.productName) )
			  .append("\"")
			  .append(",\"listings\":[")
			//foreah listing print
			r.listings.map(listingToJson(_)).addString(sb,",")
			sb.append("]}\n")
		}
		sb.toString
	}

	private def listingToJson(listing:Listing):String = {	
		val sb = new StringBuilder 
		sb.append("{\"title\":\"")
		 .append( escapeChars(listing.title) )
		 .append("\",\"manufacturer\":\"")
		 .append( escapeChars(listing.manufacturer) )
		 .append("\",\"currency\":\"")
		 .append( escapeChars(listing.currency) )
		 .append("\",\"price\":\"")
		 .append( escapeChars(listing.price) )
		 .append("\"}")
		
		sb.toString
	}

	def escapeChars(raw:String):String = {
		raw.replace("\"","\\\"")
	}
}



 
