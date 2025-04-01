#!/bin/bash

# This script runs the Dijkstra's algorithm Spark job on Azure VM

# Exit on any error
set -e

# Function to print usage instructions
function print_usage {
  echo "Usage: $0 --master-ip <MASTER_IP> --jar <JAR_FILE> --input <INPUT_FILE> --source <SOURCE_NODE> [--graphx]"
  echo "Options:"
  echo "  --master-ip   : Public IP of the Spark master node"
  echo "  --jar         : Path to the application JAR file"
  echo "  --input       : Path to the input graph file"
  echo "  --source      : Source node ID for Dijkstra's algorithm"
  echo "  --graphx      : (Optional) Use GraphX implementation instead of RDD"
  echo "  --memory      : (Optional) Executor memory (default: 4G)"
  echo "  --cores       : (Optional) Total executor cores (default: 8)"
  exit 1
}

# Default values
EXECUTOR_MEMORY="4G"
TOTAL_CORES="8"
USE_GRAPHX=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    --master-ip)
      MASTER_IP="$2"
      shift
      shift
      ;;
    --jar)
      JAR_FILE="$2"
      shift
      shift
      ;;
    --input)
      INPUT_FILE="$2"
      shift
      shift
      ;;
    --source)
      SOURCE_NODE="$2"
      shift
      shift
      ;;
    --graphx)
      USE_GRAPHX=true
      shift
      ;;
    --memory)
      EXECUTOR_MEMORY="$2"
      shift
      shift
      ;;
    --cores)
      TOTAL_CORES="$2"
      shift
      shift
      ;;
    *)
      echo "Unknown option: $1"
      print_usage
      ;;
  esac
done

# Check required parameters
if [ -z "$MASTER_IP" ] || [ -z "$JAR_FILE" ] || [ -z "$INPUT_FILE" ] || [ -z "$SOURCE_NODE" ]; then
  print_usage
fi

# Determine which implementation to use
if [ "$USE_GRAPHX" = true ]; then
  CLASS_NAME="DijkstraGraphXApp"
  echo "Using GraphX implementation"
else
  CLASS_NAME="DijkstraSparkApp"
  echo "Using RDD implementation"
fi

# Run the Spark job
echo "Running Dijkstra's algorithm on Spark cluster..."
ssh azureuser@$MASTER_IP << EOF
  /opt/spark/bin/spark-submit \
    --class $CLASS_NAME \
    --master spark://$MASTER_IP:7077 \
    --executor-memory $EXECUTOR_MEMORY \
    --total-executor-cores $TOTAL_CORES \
    $JAR_FILE \
    $INPUT_FILE \
    $SOURCE_NODE
EOF

echo "Job completed!"
