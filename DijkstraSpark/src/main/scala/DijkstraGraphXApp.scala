import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

object DijkstraGraphXApp {
  // Define infinite distance for unreachable nodes
  val INF = Double.PositiveInfinity

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      println("Usage: DijkstraGraphXApp <input_file> <source_node>")
      System.exit(1)
    }

    val inputFile = args(0)
    val sourceNode = args(1).toLong

    // Set up Spark configuration
    val conf = new SparkConf().setAppName("Dijkstra's Algorithm with GraphX")
    val sc = new SparkContext(conf)

    // Measure runtime
    val startTime = System.currentTimeMillis()

    // Read and parse input file
    val (verticesRDD, edgesRDD) = readGraphFromFile(sc, inputFile)
    
    // Run Dijkstra's algorithm using GraphX
    val shortestPaths = dijkstra(sc, verticesRDD, edgesRDD, sourceNode)

    // Calculate runtime
    val endTime = System.currentTimeMillis()
    val runtime = (endTime - startTime) / 1000.0

    // Output results
    println(s"Shortest distances from node $sourceNode:")
    shortestPaths.collect().sortBy(_._1).foreach {
      case (node, distance) =>
        val distStr = if (distance == INF) "INF" else distance.toInt.toString
        println(s"Node $node: $distStr")
    }

    println(s"\nRuntime: $runtime seconds")

    sc.stop()
  }

  /**
   * Reads a graph from a file in edge list format and returns RDDs for vertices and edges.
   * Expected format:
   * num_nodes num_edges
   * u1 v1 weight1
   * u2 v2 weight2
   * ...
   */
  def readGraphFromFile(sc: SparkContext, filename: String): (RDD[(VertexId, Double)], RDD[Edge[Double]]) = {
    // Read all lines from the file
    val lines = sc.textFile(filename)
    
    // Extract the first line to get num_nodes and num_edges
    val firstLine = lines.first()
    val parts = firstLine.trim.split("\\s+")
    val numNodes = parts(0).toInt
    
    // Create vertices RDD with initial distances (Infinity for all except source)
    val verticesRDD = sc.parallelize(0 until numNodes).map(id => (id.toLong, INF))
    
    // Parse the edge list
    val edgesRDD = lines
      .filter(_ != firstLine)
      .map(line => {
        val parts = line.trim.split("\\s+")
        Edge(parts(0).toLong, parts(1).toLong, parts(2).toDouble)
      })
    
    (verticesRDD, edgesRDD)
  }

  /**
   * Implements Dijkstra's algorithm using Spark GraphX.
   * Returns an RDD containing (node, shortest distance) pairs.
   */
  def dijkstra(sc: SparkContext, verticesRDD: RDD[(VertexId, Double)], edgesRDD: RDD[Edge[Double]], sourceNode: VertexId): RDD[(VertexId, Double)] = {
    // Initialize source node with distance 0
    val initialGraph = Graph(verticesRDD, edgesRDD)
    val sourceRDD = sc.parallelize(Array((sourceNode, 0.0)))
    val initializedGraph = initialGraph.outerJoinVertices(sourceRDD) {
      (_, oldDist, newDistOpt) => newDistOpt.getOrElse(oldDist)
    }

    // Define message propagation in Pregel
    val sssp = initializedGraph.pregel(INF, maxIterations = Int.MaxValue, EdgeDirection.Out)(
      // Vertex Program: Keep minimum distance
      (_, dist, newDist) => math.min(dist, newDist),
      
      // Send Message: Calculate new distance and send to adjacent vertices
      triplet => {
        if (triplet.srcAttr + triplet.attr < triplet.dstAttr) {
          Iterator((triplet.dstId, triplet.srcAttr + triplet.attr))
        } else {
          Iterator.empty
        }
      },
      
      // Merge Message: Keep minimum of received messages
      (a, b) => math.min(a, b)
    )

    // Return the vertices with their distances
    sssp.vertices
  }
}
