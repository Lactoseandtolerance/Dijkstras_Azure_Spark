import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import scala.collection.mutable.{PriorityQueue, Set => MutableSet}

object DijkstraSparkApp {
  // Define infinite distance for unreachable nodes
  val INF = Int.MaxValue

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: DijkstraSparkApp <input_file> <source_node>")
      System.exit(1)
    }

    val inputFile = args(0)
    val sourceNode = args(1).toInt

    // Set up Spark configuration
    val conf = new SparkConf().setAppName("Dijkstra's Algorithm with Spark")
    val sc = new SparkContext(conf)

    // Measure runtime
    val startTime = System.currentTimeMillis()

    // Read and parse input file
    val graphRDD = readGraphFromFile(sc, inputFile)
    
    // Run Dijkstra's algorithm
    val shortestPaths = dijkstra(sc, graphRDD, sourceNode)

    // Calculate runtime
    val endTime = System.currentTimeMillis()
    val runtime = (endTime - startTime) / 1000.0

    // Output results
    println(s"Shortest distances from node $sourceNode:")
    shortestPaths.collect().sortBy(_._1).foreach {
      case (node, distance) =>
        val distStr = if (distance == INF) "INF" else distance.toString
        println(s"Node $node: $distStr")
    }

    println(s"\nRuntime: $runtime seconds")

    sc.stop()
  }

  /**
   * Reads a graph from a file in edge list format and returns an RDD of edges.
   * Expected format:
   * num_nodes num_edges
   * u1 v1 weight1
   * u2 v2 weight2
   * ...
   */
  def readGraphFromFile(sc: SparkContext, filename: String): RDD[(Int, Int, Int)] = {
    // Read all lines from the file
    val lines = sc.textFile(filename)
    
    // Extract the first line to get num_nodes and num_edges (not used in this implementation)
    val firstLine = lines.first()
    
    // Filter out the first line and parse the edge list
    val edgesRDD = lines
      .filter(_ != firstLine)
      .map(line => {
        val parts = line.trim.split("\\s+")
        (parts(0).toInt, parts(1).toInt, parts(2).toInt)
      })
    
    edgesRDD
  }

  /**
   * Implements Dijkstra's algorithm using Spark RDDs.
   * Returns an RDD containing (node, shortest distance) pairs.
   */
  def dijkstra(sc: SparkContext, graphRDD: RDD[(Int, Int, Int)], sourceNode: Int): RDD[(Int, Int)] = {
    // Create an adjacency list representation of the graph
    val adjacencyListRDD = graphRDD
      .map { case (u, v, weight) => (u, (v, weight)) }
      .groupByKey()
      .cache()

    // Collect the adjacency list to the driver as this specific implementation
    // of Dijkstra's algorithm is not fully distributed
    val adjacencyList = adjacencyListRDD.collectAsMap()

    // Get all nodes from the graph
    val allNodes = sc.union(
      graphRDD.map(_._1),
      graphRDD.map(_._2)
    ).distinct().collect().toSet

    // Initialize distances map: source node to 0, all others to infinity
    val distances = collection.mutable.Map[Int, Int]()
    allNodes.foreach(node => distances(node) = if (node == sourceNode) 0 else INF)

    // Priority queue for Dijkstra's algorithm, ordering by distance (smallest first)
    val pq = PriorityQueue[(Int, Int)]()(Ordering.by(x => -x._2))
    pq.enqueue((sourceNode, 0))

    // Set to keep track of visited nodes
    val visited = MutableSet[Int]()

    // Main Dijkstra's algorithm loop
    while (pq.nonEmpty) {
      val (node, dist) = pq.dequeue()
      
      if (!visited.contains(node)) {
        visited += node
        
        // Get neighbors from adjacency list
        val neighbors = adjacencyList.getOrElse(node, Iterable.empty)
        
        for ((neighbor, weight) <- neighbors) {
          val newDist = dist + weight
          if (newDist < distances(neighbor)) {
            distances(neighbor) = newDist
            pq.enqueue((neighbor, newDist))
          }
        }
      }
    }

    // Convert the distances map to an RDD
    sc.parallelize(distances.toSeq)
  }
}
