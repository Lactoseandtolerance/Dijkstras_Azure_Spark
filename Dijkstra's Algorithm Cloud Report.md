This report details the implementation and evaluation of Dijkstra’s algorithm using Apache Spark on Azure VMs using scala. The project aimed to find the shortest paths from a single source node to all other nodes in a weighted graph using parallel graph processing with Spark RDDs or Spark GraphX. The input is a graph in edge list format, and the output consists of the shortest distances from a designated source node to all reachable nodes, with "INF" for unreachable ones.

**Explanation of Implementation:**

Two distinct implementations of Dijkstra's algorithm were developed using Apache Spark:

1. **RDD-Based Implementation (DijkstraSparkApp):** This implementation utilizes Spark's Resilient Distributed Datasets (RDDs).

   * The graph data is loaded from a text file and parsed into RDDs of edges (source, destination, weight).  
   * These edges are transformed into an adjacency list representation using *groupByKey().*  
   * The adjacency list is collected to the driver node.  
   * The classic Dijkstra's algorithm is then executed on the driver using a priority queue to find the shortest distances. This approach leverages Spark for data loading and distribution but centralizes the core algorithm execution.  
   * Finally, the resulting distances are converted back to an RDD for output.

2. **GraphX-Based Implementation (DijkstraGraphXApp):** This implementation leverages Spark's GraphX library.

   * Vertices and edges are created from the input data to construct a GraphX Graph object.  
   * The Dijkstra's algorithm is implemented in a distributed manner using the Pregel API. This involves a series of supersteps with a Vertex Program (maintaining minimum distances), a Send Message phase (propagating distance updates), and a Merge Message phase (combining updates by taking the minimum). This approach follows a distributed computation model based on the Bulk Synchronous Parallel (BSP) paradigm.  
   * The shortest paths are available as vertex properties in the resulting graph after convergence.

**Performance Analysis:**

Performance testing was conducted on a standard Azure VM (D4s\_v3 with 4 vCPUs and 16GB RAM) running Ubuntu 22.04 with Apache Spark 3.3.2 in standalone mode. The test graph contained 10,000 nodes and approximately 100,000 edges. The runtime comparison between the two implementations is as follows:

| Implementation | Source Node | Runtime (seconds) |
| :---- | :---- | :---- |
| RDD-based | 0 | 8.519 |
| GraphX-based | 0 | 14.714 |

**Analysis:** The RDD-based implementation outperformed the GraphX-based implementation by approximately 42% for this specific graph size and configuration. This difference is attributed to:

* **Execution Strategy:** The RDD implementation's centralized algorithm execution on the driver avoids the overhead of distributed computation and communication for a graph of this scale.  
* **Priority Queue Efficiency:** The RDD implementation's use of a priority queue is highly efficient for Dijkstra's algorithm.  
* **Communication Overhead:** The GraphX implementation incurs higher communication costs due to message passing between vertices in the Pregel model.

However, it's important to consider scalability:

* **RDD Implementation:** May scale poorly for very large graphs due to the collection of the entire adjacency list to the driver, potentially leading to memory issues.  
* **GraphX Implementation:** Offers better theoretical scalability for extremely large graphs as it distributes both data and computation, although it has higher communication overhead that becomes less significant with increasing graph size.

**Challenges and Lessons Learned:**

Several technical challenges were encountered during the project:

* **Java-Scala Compatibility:** Incompatibility issues arose between a newer Java version (Java 23\) and Scala 2.12.x during local builds, causing compilation errors. The solution was to build the project directly on the Azure VM with a compatible Java version (OpenJDK 11).  
* **Azure VM Configuration:** Setting up the Spark environment on Azure involved challenges with binding Spark to the VM's external IP address, which was resolved by configuring Spark to bind to "0.0.0.0".  
* **Network Security:** Azure's default firewall blocked necessary Spark ports (7077, 8080), requiring the creation of explicit Network Security Group rules to allow traffic.  
* **Resource Limitations:** Azure quota limits were encountered when attempting to create a multi-node cluster, leading to the deployment of a single-node Spark cluster.

Algorithmic insights gained include:

* For moderately sized graphs, a hybrid approach (distributed data loading, centralized algorithm execution) can outperform fully distributed implementations like the GraphX-based one for Dijkstra's algorithm.  
* Expressing sequential algorithms like Dijkstra's in the vertex-centric Pregel model requires a different way of thinking about computation.  
* Performance bottlenecks in distributed graph processing often involve communication overhead, data skew, and serialization/deserialization costs.

Cloud deployment lessons learned include:

* The importance of selecting appropriate VM sizes based on memory and computation needs, with memory often being more critical for graph processing.  
* The significant impact of tuning Spark configuration parameters like executor and driver memory on performance.  
* Establishing an efficient development workflow involving local testing with small datasets, cloud deployment for larger datasets, and monitoring using Spark's web UIs.

This implementation demonstrates the execution of Dijkstra's algorithm on Apache Spark using both RDD and GraphX approaches. The RDD implementation showed better performance for the tested graph size, while GraphX offers better theoretical scalability for much larger graphs. The Azure deployment provided valuable experience with cloud-based big data processing. Future work could explore further optimizations and benchmarking with significantly larger graphs.

