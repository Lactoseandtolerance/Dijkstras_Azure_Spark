# Dijkstra's Algorithm Implementation with Apache Spark on Azure

This project implements Dijkstra's shortest path algorithm using Apache Spark to find the shortest path from a source node to all other nodes in a large graph. The implementation is deployed on Microsoft Azure virtual machines.

## Project Structure

```
DijkstraApp/
├── src/main/scala/
│   ├── DijkstraSparkApp.scala     # RDD-based implementation
│   └── DijkstraGraphXApp.scala    # GraphX-based implementation
├── build.sbt                      # SBT build configuration
└── README.md                      # This file
```

## Implementations

Two different implementations of Dijkstra's algorithm are provided:

1. **RDD-based implementation (DijkstraSparkApp)**: Uses Spark's RDDs for representing the graph, with the main algorithm logic running on the driver after collecting the adjacency list.

2. **GraphX-based implementation (DijkstraGraphXApp)**: Leverages Spark's GraphX library for graph processing, using the Pregel API for distributed computation.

## Requirements

- Java 8 or Java 11 (Java 11 recommended)
- Scala 2.12.x 
- SBT 1.x
- Apache Spark 3.3.x
- Microsoft Azure account

## Azure VM Setup Process

### 1. Create Resource Group in Azure
```bash
az group create --name DijkstraSparkRG --location eastus
```

### 2. Create VM for Spark
```bash
az vm create \
    --resource-group DijkstraSparkRG \
    --name spark-master \
    --image Ubuntu2204 \
    --admin-username azureuser \
    --generate-ssh-keys \
    --size Standard_D4s_v3
```

### 3. Open Required Ports
```bash
# Open port 7077 for Spark communication
az network nsg rule create \
    --resource-group DijkstraSparkRG \
    --nsg-name spark-masterNSG \
    --name Spark \
    --protocol tcp \
    --priority 1001 \
    --destination-port-range 7077

# Open port 8080 for Spark UI
az network nsg rule create \
    --resource-group DijkstraSparkRG \
    --nsg-name spark-masterNSG \
    --name SparkUI \
    --protocol tcp \
    --priority 1002 \
    --destination-port-range 8080
```

### 4. Set Up Spark on the VM
SSH into the VM and run the following commands:

```bash
# Update package lists
sudo apt-get update -y

# Install Java
sudo apt-get install -y openjdk-11-jdk

# Install Scala
sudo apt-get install -y scala

# Download and extract Spark
wget https://archive.apache.org/dist/spark/spark-3.3.2/spark-3.3.2-bin-hadoop3.tgz
tar -xzf spark-3.3.2-bin-hadoop3.tgz
sudo mv spark-3.3.2-bin-hadoop3 /opt/spark

# Configure Spark 
cd /opt/spark/conf
cp spark-env.sh.template spark-env.sh
# Set to bind to all interfaces
echo "export SPARK_MASTER_HOST=0.0.0.0" >> spark-env.sh

# Start Spark master
/opt/spark/sbin/start-master.sh

# Start a worker on the same machine
/opt/spark/sbin/start-worker.sh spark://spark-master:7077
```

## Building and Running the Project

### 1. Upload Graph Data
```bash
# From your local machine
scp weighted_graph.txt azureuser@<VM_IP>:~
```

### 2. Prepare Project on VM
SSH into the VM and set up the project:

```bash
# Create project directories
mkdir -p ~/DijkstraApp/src/main/scala
cd ~/DijkstraApp

# Create build.sbt (see project files)
# Create scala source files (see project files)

# Install SBT if needed
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install -y sbt

# Compile the project
sbt package
```

### 3. Run the Spark Job

#### RDD Implementation:
```bash
/opt/spark/bin/spark-submit \
  --class DijkstraSparkApp \
  --master spark://spark-master:7077 \
  --executor-memory 2G \
  --driver-memory 2G \
  --total-executor-cores 4 \
  target/scala-2.12/dijkstraspark_2.12-1.0.jar \
  ~/weighted_graph.txt \
  0
```

#### GraphX Implementation:
```bash
/opt/spark/bin/spark-submit \
  --class DijkstraGraphXApp \
  --master spark://spark-master:7077 \
  --executor-memory 2G \
  --driver-memory 2G \
  --total-executor-cores 4 \
  target/scala-2.12/dijkstraspark_2.12-1.0.jar \
  ~/weighted_graph.txt \
  0
```

## Performance Results

Performance testing on a graph with 10,000 nodes and approximately 100,000 edges:

| Implementation | Source Node | Runtime (seconds) |
|----------------|-------------|-------------------|
| RDD-based      | 0           | 8.519             |
| GraphX-based   | 0           | 14.714            |

## Troubleshooting

### Java Compatibility Issues
- Scala 2.12.x works best with Java 8 or Java 11
- Java 23 and newer versions may cause compilation errors with Scala 2.12.x
- If encountering Java compatibility issues when building locally, build directly on the VM with OpenJDK 11

### Spark Configuration
- If Spark fails to start, check logs in `/opt/spark/logs/`
- Ensure SPARK_MASTER_HOST is set to "0.0.0.0" to bind to all interfaces
- Verify ports 7077 and 8080 are open in Azure NSG

### Memory Issues
- If encountering out-of-memory errors, adjust `--executor-memory` and `--driver-memory` settings
- For very large graphs, consider increasing VM size or using multiple worker nodes

## Monitoring and Debugging

- Spark Master UI: http://<VM_IP>:8080
- Application UI during execution: http://<VM_IP>:4040

## Conclusion

This implementation demonstrates the execution of Dijkstra's algorithm on Apache Spark using both RDD and GraphX approaches. The RDD implementation shows better performance for moderately sized graphs, while the GraphX implementation would likely have better scalability for very large graphs.