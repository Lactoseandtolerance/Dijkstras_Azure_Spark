# Dijkstra's Algorithm Implementation with Apache Spark on Azure VMs
**Programming Assignment Report**

## 1. Implementation Explanation

In this assignment, I implemented Dijkstra's shortest path algorithm using Apache Spark and deployed it on Microsoft Azure. Two distinct implementations were developed to compare different approaches in Spark's ecosystem:

### 1.1 RDD-Based Implementation

The first implementation uses Spark's Resilient Distributed Datasets (RDDs) as the primary abstraction. The algorithm follows these steps:

1. **Data Loading**: The graph data is read from a text file in edge list format and parsed into RDDs representing edges as tuples (source, destination, weight).

2. **Graph Representation**: The edges are transformed into an adjacency list representation using `groupByKey()` operation, creating a structure where each node maps to its list of neighbors with corresponding weights.

3. **Algorithm Execution**: 
   - The adjacency list is collected to the driver using `collectAsMap()`.
   - A priority queue is used to maintain vertices ordered by their tentative distances.
   - The classic Dijkstra's algorithm is executed on the driver, using the priority queue to always select the unvisited vertex with the smallest distance.
   - Upon completion, the distances map is converted back to an RDD for output.

4. **Output**: The shortest paths from the source node to all other nodes are returned as an RDD of (node, distance) pairs.

This implementation is a hybrid approach that leverages Spark for data loading and distribution but executes the core algorithm logic centrally on the driver.

### 1.2 GraphX-Based Implementation

The second implementation uses Spark's GraphX library, which provides graph-parallel computation primitives:

1. **Graph Construction**: Vertices and edges are created from the input data and used to build a GraphX `Graph` object.

2. **Algorithm Execution**: 
   - The Pregel API is used to implement Dijkstra's algorithm in a distributed manner.
   - The algorithm is expressed as a series of supersteps where:
     - Vertex Program: Maintains the minimum distance at each vertex.
     - Send Message: Propagates new distance information along edges.
     - Merge Message: Combines multiple distance updates by taking the minimum.
   - This approach is fully distributed, with computation occurring at the vertex level across the cluster.

3. **Output**: After convergence, the shortest paths are available as vertex properties in the resulting graph.

The GraphX implementation follows a more distributed computation model aligned with the Bulk Synchronous Parallel (BSP) paradigm.

## 2. Performance Analysis

Performance testing was conducted on a standard Azure VM (D4s_v3 with 4 vCPUs and 16GB RAM) running Ubuntu 22.04 with Apache Spark 3.3.2 in standalone mode. The test graph contained 10,000 nodes and approximately 100,000 edges.

### 2.1 Runtime Comparison

| Implementation | Source Node | Runtime (seconds) |
|----------------|-------------|-------------------|
| RDD-based      | 0           | 8.519             |
| GraphX-based   | 0           | 14.714            |

**Analysis**:
- The RDD-based implementation outperformed the GraphX-based implementation by approximately 42% for this particular graph size and configuration.
- This performance difference can be attributed to several factors:
  1. **Execution Strategy**: The RDD implementation executes the core algorithm on the driver after collecting the adjacency list, avoiding the overhead of distributed computation and communication for a graph of this scale.
  2. **Priority Queue Efficiency**: The RDD implementation uses a priority queue for node selection, which is highly efficient for Dijkstra's algorithm.
  3. **Communication Overhead**: The GraphX implementation incurs higher communication costs due to message passing between vertices in the Pregel model.

### 2.2 Scalability Considerations

While the RDD implementation is faster for the test graph, its scalability characteristics differ from the GraphX implementation:

- **RDD Implementation**: Scales poorly for very large graphs as it collects the entire adjacency list to the driver, potentially causing memory pressure. The algorithm's time complexity remains O(E log V) where E is the number of edges and V is the number of vertices.

- **GraphX Implementation**: Offers better theoretical scalability for extremely large graphs as it distributes both the graph data and computation across the cluster. However, this comes with higher communication overhead, which becomes less significant as the graph size increases.

## 3. Challenges and Lessons Learned

### 3.1 Technical Challenges

1. **Java-Scala Compatibility**: One of the most significant challenges was the incompatibility between newer Java versions (Java 23) and Scala 2.12.x when trying to build the project locally. This resulted in compilation errors related to the Scala compiler bridge.
   
   **Solution**: The project was built directly on the Azure VM which had a compatible Java version (OpenJDK 11) installed.

2. **Azure VM Configuration**: Setting up the Spark environment on Azure required careful configuration:
   - Initially encountered issues with binding Spark to the VM's external IP address.
   - Solved by configuring Spark to bind to "0.0.0.0" instead of a specific IP.
   
3. **Network Security**: Azure's default firewall settings blocked the necessary Spark ports (7077 for communication, 8080 for UI).
   
   **Solution**: Created explicit Network Security Group rules to allow traffic on these ports.

4. **Resource Limitations**: Encountered Azure quota limits when attempting to create a multi-node cluster.
   
   **Solution**: Deployed a single-node Spark cluster with both master and worker on the same VM.

### 3.2 Algorithmic Insights

1. **Centralized vs. Distributed Algorithms**: Discovered that for certain graph algorithms like Dijkstra's, a hybrid approach (distributed data loading/storage, centralized algorithm execution) can outperform fully distributed implementations for moderately sized graphs.

2. **GraphX Pregel Model**: Learning to express Dijkstra's algorithm in terms of the Pregel compute model required a paradigm shift from thinking sequentially to thinking in terms of vertex-centric computation steps.

3. **Performance Factors**: Identified that the main performance bottlenecks in distributed graph processing are often related to:
   - Communication overhead between partitions
   - Data skew (some vertices having significantly more edges)
   - Serialization/deserialization costs

### 3.3 Cloud Deployment Lessons

1. **Azure VM Selection**: Learned the importance of selecting appropriate VM sizes based on memory and computation requirements. For graph processing, memory is often more critical than CPU cores.

2. **Spark Configuration**: Discovered that tuning parameters like executor memory, driver memory, and serialization settings can significantly impact performance.

3. **Development Workflow**: Established an efficient workflow for developing and testing Spark applications on Azure:
   - Develop and test with small datasets locally
   - Deploy and test with larger datasets on cloud infrastructure
   - Monitor using Spark's web UIs to identify bottlenecks

## Conclusion

This project successfully implemented Dijkstra's shortest path algorithm using two different approaches in Apache Spark. The RDD-based implementation demonstrated superior performance for moderately sized graphs, while the GraphX-based implementation offers better theoretical scalability for very large graphs.

The deployment on Azure VMs provided valuable experience with cloud-based big data processing. The challenges encountered led to a deeper understanding of distributed graph algorithms, Spark's execution model, and cloud infrastructure configuration.

Future work could explore optimizations such as custom graph partitioning strategies, more sophisticated priority queue implementations, and benchmarking with much larger graphs to identify the crossover point where the GraphX implementation begins to outperform the RDD implementation.