package cse512
object HotzoneUtils {
  def ST_Contains(queryRectangle: String, pointString: String ): Boolean = {
    // YOU NEED TO CHANGE THIS PART
    /// Parse Point Query and Convert to Double
    val data = pointString.split(",").map(_.toDouble)
    // Store x and y coordinates of point
    val data_x = data(0)
    val data_y = data(1)
    // Parse queryRectangle and Convert to Double
    val rectangle = queryRectangle.split(",").map(_.toDouble)
    // Store x and y coordinates of rectangle
    val rect_x1 = Math.min(rectangle(0), rectangle(2))
    val rect_y1 = Math.min(rectangle(1),rectangle(3))
    val rect_x2 = Math.max(rectangle(0), rectangle(2))
    val rect_y2 = Math.max(rectangle(1),rectangle(3))
    // Check if point is within the rectangle
    return (data_x >= rect_x1 && data_x <= rect_x2 && data_y >= rect_y1 && data_y <= rect_y2)
  }
  // YOU NEED TO CHANGE THIS PART
}
