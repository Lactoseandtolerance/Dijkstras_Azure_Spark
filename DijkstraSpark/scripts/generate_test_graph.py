#!/usr/bin/env python3

import random
import sys

def generate_graph(num_nodes, num_edges, max_weight=100):
    """Generate a random graph with the specified number of nodes and edges."""
    edges = set()
    
    for i in range(1, num_nodes):
        j = random.randint(0, i-1)
        weight = random.randint(1, max_weight)
        edges.add((j, i, weight))
    
    while len(edges) < num_edges:
        u = random.randint(0, num_nodes-1)
        v = random.randint(0, num_nodes-1)
        if u != v: 
            weight = random.randint(1, max_weight)
            edges.add((u, v, weight))
    
    return sorted(list(edges))

def write_graph(filename, edges, num_nodes):
    """Write the graph to a file in the required format."""
    with open(filename, 'w') as f:
        f.write(f"{num_nodes} {len(edges)}\n")
        for u, v, weight in edges:
            f.write(f"{u} {v} {weight}\n")

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python generate_test_graph.py <num_nodes> <num_edges> <output_file>")
        sys.exit(1)
    
    num_nodes = int(sys.argv[1])
    num_edges = int(sys.argv[2])
    output_file = sys.argv[3]
    
    if num_edges < num_nodes - 1:
        print(f"Error: Number of edges must be at least {num_nodes-1} to ensure the graph is connected.")
        sys.exit(1)
    
    max_possible_edges = num_nodes * (num_nodes - 1)
    if num_edges > max_possible_edges:
        print(f"Error: Maximum possible number of edges for {num_nodes} nodes is {max_possible_edges}.")
        sys.exit(1)
    
    edges = generate_graph(num_nodes, num_edges)
    write_graph(output_file, edges, num_nodes)
    
    print(f"Generated a graph with {num_nodes} nodes and {num_edges} edges, written to {output_file}.")
