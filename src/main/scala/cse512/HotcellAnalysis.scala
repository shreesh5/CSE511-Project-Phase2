package cse512

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions._

object HotcellAnalysis {
  Logger.getLogger("org.spark_project").setLevel(Level.WARN)
  Logger.getLogger("org.apache").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("com").setLevel(Level.WARN)

def runHotcellAnalysis(spark: SparkSession, pointPath: String): DataFrame =
{
  // Load the original data from a data source
  var pickupInfo = spark.read.format("com.databricks.spark.csv").option("delimiter",";").option("header","false").load(pointPath);
  pickupInfo.createOrReplaceTempView("nyctaxitrips")
  pickupInfo.show()

  // Assign cell coordinates based on pickup points
  spark.udf.register("CalculateX",(pickupPoint: String)=>((
    HotcellUtils.CalculateCoordinate(pickupPoint, 0)
    )))
  spark.udf.register("CalculateY",(pickupPoint: String)=>((
    HotcellUtils.CalculateCoordinate(pickupPoint, 1)
    )))
  spark.udf.register("CalculateZ",(pickupTime: String)=>((
    HotcellUtils.CalculateCoordinate(pickupTime, 2)
    )))
  pickupInfo = spark.sql("select CalculateX(nyctaxitrips._c5),CalculateY(nyctaxitrips._c5), CalculateZ(nyctaxitrips._c1) from nyctaxitrips")
  var newCoordinateName = Seq("x", "y", "z")
  pickupInfo = pickupInfo.toDF(newCoordinateName:_*)
  pickupInfo.createOrReplaceTempView("pickupInfo")
  pickupInfo.show()

  // Define the min and max of x, y, z
  val minX = -74.50/HotcellUtils.coordinateStep
  val maxX = -73.70/HotcellUtils.coordinateStep
  val minY = 40.50/HotcellUtils.coordinateStep
  val maxY = 40.90/HotcellUtils.coordinateStep
  val minZ = 1
  val maxZ = 31
  val numCells = (maxX - minX + 1)*(maxY - minY + 1)*(maxZ - minZ + 1)

  // getting all the points which fall between the min and max both included
  val selectedCellVals = spark.sql("select x,y,z from pickupInfo where x>=" + minX + " and x<=" + maxX + " and y>=" + minY + " and y<=" + maxY + " and z>=" + minZ + " and z<=" + maxZ + " order by x,y,z").persist();
  // creating a temporary view of the points
  selectedCellVals.createOrReplaceTempView("selectedCellVals")
  println("selectedCellVals")
  selectedCellVals.show()

  // getting the count of the cells along with the points
  val selectedCellHotness = spark.sql("select x,y,z,count(*) as hotCells from selectedCellVals group by x,y,z order by z,y,x");
  // creating a temporary view of the count and points
  selectedCellHotness.createOrReplaceTempView("selectedCellHotness")
  println("selectedCellHotness")
  selectedCellHotness.show()

  // calculating sum of selected cells
  val sumOfSelectedCells = spark.sql("select sum(hotCells) as sumHotCells from selectedCellHotness")
  sumOfSelectedCells.createOrReplaceTempView("sumOfSelectedCells")
  println("sumSelectedCells")
  sumOfSelectedCells.show()

  // creating function to calculate square of a number
  spark.udf.register("squared", (inputX: Int) => ((Math.pow(inputX, 2).toDouble)))

  // calculating the mean
  val mean = (sumOfSelectedCells.first().getLong(0).toDouble / numCells.toDouble).toDouble
  println("mean = " + mean)

  // calculating the sum of the sqaures of the selected cells
  val sumOfSquares = spark.sql("select sum(squared(hotCells)) as sumOfSquares from selectedCellHotness")
  sumOfSquares.createOrReplaceTempView("sumOfSquares")
  println("sumofSquares")
  sumOfSquares.show()

  // calculating the standard deviation
  val standardDeviation = scala.math.sqrt(((sumOfSquares.first().getDouble(0).toDouble / numCells.toDouble) - (mean.toDouble * mean.toDouble))).toDouble
  println("standard deviation = " + standardDeviation)

  // getting the x values and storing it in a list
  var xVals = selectedCellHotness.select("x").rdd.map(i => i(0)).collect();
  // getting the y values and storing it in a list
  var yVals = selectedCellHotness.select("y").rdd.map(i => i(0)).collect();
  // getting the z values and storing it in a list
  var zVals = selectedCellHotness.select("z").rdd.map(i => i(0)).collect();
  // getting the count and storing it in a list
  var countVals = selectedCellHotness.select("hotCells").rdd.map(i => i(0)).collect();

  // getting length of array
  var xlength = xVals.length;

  // creating a map for x,y,z values as keys and count as values
  var testMap: Map[String, Int] = Map();
  testMap = HotcellUtils.createMap(xlength, xVals, yVals, zVals, countVals)

  // creating a function to calculate Getis Ord score
  spark.udf.register("calcGScore", (x:Int, y:Int, z:Int) => (HotcellUtils.calcGScore(x, y, z, minX, maxX, minY, maxY, minZ, maxZ, standardDeviation, mean, numCells, testMap)))
  // retrieving points and calculating Getis Ord score, and then ordering in descending order according to Getis Ord score
  var finalpickup = spark.sql("select x, y, z from selectedCellHotness order by calcGScore(x, y, z) desc limit 50").persist()
  println("finalpickup")
  finalpickup.show()
  return finalpickup // YOU NEED TO CHANGE THIS PART
}
}
