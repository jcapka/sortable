Sortable Challenge
==================

My attempt at the matching of listings and products as defined in the challenge. I decided to use Scala since that was the preferred language to code this in. My day to day work is in Microsoft C# / HTML / JavaScript and this challenge seemed like a good opportunity to learn something new.  It's my first program in Scala so I am sure there are some silly things in my code, but at the same time I see a lot of overlap with the latest C# functionality and thus hope I didn't do anything too stupid. 

I have made my life a little easier by keeping the code in one file. I realize production code would be split up between more files but I was trying to focus on the code rather than the complexities of the environment. 

The program is dependent on the scalax.io library as IO functionality in Scala seems to be very limited from what I could find. The necessary jar files are located in the source folder, again keeping my life a little simpler. 

Please note that the code does produce two warnings upon compilation. I was not too happy about this but am not sure if there is a way that they can be avoided. Beyond my Scala skills at the moment I am afraid.

Running the Code
----------------

1. Compile the code:
	`scalac -classpath "*.jar:." SortableChallenge.scala`
2. Run the program:
	`scala -classpath "*.jar:." SortableChallenge products.txt listings.txt`
3. Find output in a file called `results.txt` that is to the spec of the challenge. Note that while each line is valid JSON, the file itself is NOT, but this is the same as the product and listing data files. 

Data Structures and Algorithms
------------------------------
I decided to try a fairly simple approach, and it turned out to work quite well. I first organized the products into a Product tree that is easier / faster to search through, and then compared the listings to the tree. I used simple string matching to find model identifiers in the listings, and by ordering the product models longest string first, I did a greedy search across the product tree. 
The product tree is built of domain objects, and each object contains two collections: 

1. A map of the sub-objects that belong to it
2. An ordered list of keys to the map. The ordering is by key length and ordered maps didn't work too well with this ordering. (Items were erased) Thus to avoid resorting the keys every time, a sort order list is kept.

Algorithm
---------
### For each listing
	1. match Manufacturer - no match => next listing
	2. match all models in all families of matched manu [1] 
		--> results in list of models that need to be filtered down
	3. match Family
	4. trim to 1 match


### match Manufacturer:
	* match manu field in listing and product
	* OR
	* listing has no manu AND product manu found in listing title

### match Model:
	* model variation in listing title [2]  

### match Family:
	* family matches only one model [3]
	* AND
		* family is unknown 
		* OR
		* family in listing title
		* OR
		* none of manu families in listing title [4]

### trim to 1 match:
	* select product with longest model match


[1]: Family is not always provided, and sometimes misused (two families listed etc). Therefore the algorithm gathers all reasonable model matches across all models in all families

[2]: Theoretically multiple variations are possible but never occurs in this data set. Making assumption that this will almost always be the case. If multiple variations are found, they are treated as one model match.

[3]: If a family  matches multiple models, then the product is likely an accessory, etc.

[4]: Unknown family means ignore family for that model, family in listing title means good match, none of families in listing title means listing doesn't use family.

Data Structure
--------------
<pre>
Tree
	Product
	Product
	Product
		Family
		Family
			Model
			Model
		Family
	Product

</pre>






