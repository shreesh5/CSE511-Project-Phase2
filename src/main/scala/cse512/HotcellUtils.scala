package cse512

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar

import org.apache.spark.rdd.RDD

import scala.collection.mutable.ListBuffer

object HotcellUtils {
  val coordinateStep = 0.01

  def CalculateCoordinate(inputString: String, coordinateOffset: Int): Int =
  {
    // Configuration variable:
    // Coordinate step is the size of each cell on x and y
    var result = 0
    coordinateOffset match
    {
      case 0 => result = Math.floor((inputString.split(",")(0).replace("(","").toDouble/coordinateStep)).toInt
      case 1 => result = Math.floor(inputString.split(",")(1).replace(")","").toDouble/coordinateStep).toInt
      // We only consider the data from 2009 to 2012 inclusively, 4 years in total. Week 0 Day 0 is 2009-01-01
      case 2 => {
        val timestamp = HotcellUtils.timestampParser(inputString)
        result = HotcellUtils.dayOfMonth(timestamp) // Assume every month has 31 days
      }
    }
    return result
  }

  def timestampParser (timestampString: String): Timestamp =
  {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    val parsedDate = dateFormat.parse(timestampString)
    val timeStamp = new Timestamp(parsedDate.getTime)
    return timeStamp
  }

  def dayOfYear (timestamp: Timestamp): Int =
  {
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(timestamp.getTime)
    return calendar.get(Calendar.DAY_OF_YEAR)
  }

  def dayOfMonth (timestamp: Timestamp): Int =
  {
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(timestamp.getTime)
    return calendar.get(Calendar.DAY_OF_MONTH)
  }

  // YOU NEED TO CHANGE THIS PART

  // function to create map with x,y,z as keys and count as values
  def createMap(len: Int, xVals: Array[Any], yVals: Array[Any], zVals: Array[Any], countVals: Array[Any]): Map[String, Int] = {

    var countMap:Map[String, Int] = Map();
    for (i <- 0 to len - 1) {

      // creating the key
      val key = xVals(i).toString + "-" + yVals(i).toString + "-" + zVals(i).toString;
      // creating the value
      val value = countVals(i).toString.toInt;
      // storing the (key, value) pair in the hashmap
      countMap += key -> value;
    }
    return countMap
  }

  // function to calculate g score
  def calcGScore(x:Int, y:Int, z:Int, minX:Double, maxX:Double, minY:Double, maxY:Double, minZ:Double, maxZ:Double, stand_dev: Double, mean: Double, numCells: Double, countMap: Map[String, Int]): Double = {

    /*
      In order to calculate the adjacent cells, we need to create lists
      which contain the previous, current and next coordinate for x,y and z separately
     */
    // list of previous, current and next x coordinates
    val xVals = List(x-1, x, x+1);
    // list of previous, current and next y coordinates
    val yVals = List(y-1, y, y+1);
    // list of previous, current and next z coordinates
    val zVals = List(z-1, z, z+1);

    // list to store the count of adjacent cells
    var adjCells = new ListBuffer[Long]()

    // three for loops to iterate through all combinations of the x,y,z coordinate triplets
    for (i <- xVals) {
      for (j <- yVals) {
        for (k <- zVals) {

          if (i>=minX && i<= maxX && j>=minY && j<= maxY && k>=minZ && k<= maxZ) {

            // if the map created earlier contains the coordinate triplet as a key, then add the count (value in the map)
            // to the adjCells list
            if (countMap.contains(String.valueOf(i) + "-" + String.valueOf(j) + "-" + String.valueOf(k))) {
              adjCells += countMap(String.valueOf(i) + "-" + String.valueOf(j) + "-" + String.valueOf(k))
            }
          }
        }
      }
    }

    // calculate number of adjacent cells
    val num_adjCells = adjCells.size
    // calculate sum of adjacent cell counts
    val sum_adjCells = adjCells.sum

    val numerator = sum_adjCells - (mean * num_adjCells)
    val denominator = stand_dev * math.sqrt( ((numCells * num_adjCells) - (num_adjCells*num_adjCells)) / (numCells-1) )

    val res = numerator/denominator

    return res

  }

}
