#!/bin/bash

# This script sets up a Spark standalone cluster on Azure VMs

# Exit on any error
set -e

# Function to print usage instructions
function print_usage {
  echo "Usage: $0 --resource-group <RG_NAME> --master-vm <MASTER_VM_NAME> --worker-vms <WORKER1,WORKER2,...>"
  exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    --resource-group)
      RESOURCE_GROUP="$2"
      shift
      shift
      ;;
    --master-vm)
      MASTER_VM="$2"
      shift
      shift
      ;;
    --worker-vms)
      WORKER_VMS="$2"
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
if [ -z "$RESOURCE_GROUP" ] || [ -z "$MASTER_VM" ] || [ -z "$WORKER_VMS" ]; then
  print_usage
fi

# Convert comma-separated worker VMs to an array
IFS=',' read -ra WORKER_ARRAY <<< "$WORKER_VMS"

# Get the public IP of the master VM
MASTER_IP=$(az vm show -d -g $RESOURCE_GROUP -n $MASTER_VM --query publicIps -o tsv)
echo "Master IP: $MASTER_IP"

# Install required packages on master VM
echo "Setting up master node..."
ssh azureuser@$MASTER_IP << EOF
  # Update package lists
  sudo apt-get update -y

  # Install Java
  sudo apt-get install -y openjdk-8-jdk

  # Install Scala
  sudo apt-get install -y scala

  # Download and extract Spark
  wget https://archive.apache.org/dist/spark/spark-3.3.2/spark-3.3.2-bin-hadoop3.tgz
  tar -xzf spark-3.3.2-bin-hadoop3.tgz
  sudo mv spark-3.3.2-bin-hadoop3 /opt/spark
  
  # Configure Spark master
  cd /opt/spark/conf
  cp spark-env.sh.template spark-env.sh
  echo "export SPARK_MASTER_HOST=$MASTER_IP" >> spark-env.sh
  
  # Start Spark master
  /opt/spark/sbin/start-master.sh
  
  echo "Master node setup complete!"
EOF

# Set up worker nodes
for WORKER in "${WORKER_ARRAY[@]}"; do
  echo "Setting up worker node: $WORKER"
  
  # Get the public IP of the worker VM
  WORKER_IP=$(az vm show -d -g $RESOURCE_GROUP -n $WORKER --query publicIps -o tsv)
  echo "Worker IP: $WORKER_IP"
  
  ssh azureuser@$WORKER_IP << EOF
    # Update package lists
    sudo apt-get update -y

    # Install Java
    sudo apt-get install -y openjdk-8-jdk

    # Install Scala
    sudo apt-get install -y scala

    # Download and extract Spark
    wget https://archive.apache.org/dist/spark/spark-3.3.2/spark-3.3.2-bin-hadoop3.tgz
    tar -xzf spark-3.3.2-bin-hadoop3.tgz
    sudo mv spark-3.3.2-bin-hadoop3 /opt/spark
    
    # Configure Spark worker
    cd /opt/spark/conf
    cp spark-env.sh.template spark-env.sh
    echo "export SPARK_MASTER_HOST=$MASTER_IP" >> spark-env.sh
    
    # Start Spark worker
    /opt/spark/sbin/start-worker.sh spark://$MASTER_IP:7077
    
    echo "Worker node setup complete!"
EOF
done

echo "Spark cluster setup complete!"
echo "Spark Master UI: http://$MASTER_IP:8080"
