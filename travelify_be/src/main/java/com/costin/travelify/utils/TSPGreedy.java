package com.costin.travelify.utils;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class TSPGreedy {
    private int numNodes;
    private int startNode;
    private double[][] costMatrix;
    private List<Integer> tour;
    private double tourCost;

    public TSPGreedy(double[][] costMatrix) {
        this(0, costMatrix);
    }

    public TSPGreedy(int startNode, double[][] costMatrix) {
        this.numNodes = costMatrix.length;
        this.startNode = startNode;
        this.costMatrix = costMatrix;
    }

    public void solve() {
        boolean[] visited = new boolean[numNodes];
        Arrays.fill(visited, false);

        List<Integer> tour = new ArrayList<>();
        tour.add(startNode);
        visited[startNode] = true;

        for (int i = 0; i < numNodes - 1; i++) {
            int current = tour.get(i);
            int next = findNearestNeighbor(current, visited);
            tour.add(next);
            visited[next] = true;
        }

        tour.add(startNode);

        this.tour = tour;
        this.tourCost = calculateTourLength(tour);
    }

    public int findNearestNeighbor(int node, boolean[] visited) {
        int nearestNeighbor = -1;
        double minCost = Integer.MAX_VALUE;
        for (int i = 0; i < numNodes; i++) {
            if (!visited[i] && costMatrix[node][i] < minCost) {
                nearestNeighbor = i;
                minCost = costMatrix[node][i];
            }
        }
        return nearestNeighbor;
    }

    public double calculateTourLength(List<Integer> tour) {
        double tourLength = 0;
        for (int i = 0; i < tour.size() - 1; i++) {
            int from = tour.get(i);
            int to = tour.get(i + 1);
            tourLength += costMatrix[from][to];
        }
        return tourLength;
    }

}