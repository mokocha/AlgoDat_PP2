package ads2.ss14.etsppc;

import java.util.*;

public class ETSPPC extends AbstractETSPPC {

    private final ArrayList<Location> locationArray;
    private final ArrayList<PrecedenceConstraint> constraintList;
    private final HashMap<Integer, Location> locationMap;
    private final double threshold;
    private double lowerBound;

    private Double[][] distanceMatrix;
    private LinkedList<Location> bestTour;

    public ETSPPC(ETSPPCInstance instance) {

        constraintList = (ArrayList<PrecedenceConstraint>) instance.getConstraints();
        locationMap = (HashMap<Integer, Location>) instance.getAllLocations();
        locationArray = new ArrayList<Location>(instance.getAllLocations().values());
        threshold = instance.getThreshold();

        distanceMatrix = new Double[locationArray.size()][locationArray.size()];
        calculateNNmatrix();

        bestTour = calculateNNTour(copyMatrix(distanceMatrix), new LinkedList<Location>(), 0);
        lowerBound = cost(bestTour);

        setSolution(lowerBound,bestTour);
    }

    @Override
    public void run() {
        branchAndBound(copyMatrix(distanceMatrix), copyMatrix(distanceMatrix), new LinkedList<Location>(), 0, 0);
    }

    /**
     * @param matrix        NN distance matrix
     * @param remaining     unvisited distance matrix
     * @param tour          current list of locations
     * @param node          current node
     * @param visited       last visited
     */
    public void branchAndBound(Double[][] matrix, Double[][] remaining, LinkedList<Location> tour, int node, int visited) {

        if (visited == distanceMatrix.length - 1) return;

        int nearest = chooseNearestNeighbor(remaining, node);

        LinkedList<Location> newTour = new LinkedList<Location>(tour); //copy of the current tour for the left/right branch

        // left branching, pick a node and prove its a better tour
        if (!violatedConstraint(locationArray.get(node).getCityId(),newTour)) { //check if the new node would violate constraint

            newTour.add(locationArray.get(node)); //if not, add it

            Double[][] matrixCopy = removeNode(copyMatrix(matrix), node); //temporary matrix of proven NN nodes

            //then complete the tour with NN to see if its worth pursuing
            LinkedList<Location> newTourComplete = calculateNNTour(copyMatrix(matrixCopy), new LinkedList<Location>(newTour), nearest);
            Double low = cost(newTourComplete);

            if (low < threshold && low < lowerBound) { //if it is a good solution
                bestTour = newTourComplete;
                lowerBound = low;

                setSolution(lowerBound, bestTour);

                //re add the default matrix and overwrite remaining matrix
                branchAndBound(matrixCopy, matrixCopy, newTour, nearest, ++visited);
            }
        }

        // right branching,
            branchAndBound(copyMatrix(matrix), removeNode(copyMatrix(remaining), node), new LinkedList<Location>(tour), nearest, ++visited);
    }

    /**
     * Calculate the closest nodes for each node
     */
    public void calculateNNmatrix() {

        for (int i = 0; i < locationArray.size(); i++)
        {
            for (int j = 0; j < locationArray.size(); j++)
            {
                if(i==j) distanceMatrix[i][j] = Double.POSITIVE_INFINITY;
                else distanceMatrix[i][j] = locationArray.get(i).distanceTo(locationArray.get(j));
            }
        }
    }

    /**
     * Calculates the nearest neighbor tour
     *
     *
     * @param matrix    matrix of available NN nodes
     * @param tour      nodes visited so far
     * @param start
     * @return          complete tour
     */
    public LinkedList<Location> calculateNNTour(Double[][] matrix, LinkedList<Location> tour, int start) {

        int prev;
        int next = start;
        int size = tour.size();


        for (int i = 0; i < matrix.length - size; i++) {
            tour.add(locationArray.get(next));

            PrecedenceConstraint pc = returnViolatedConstraint(tour);

            if(pc != null) { //if a constraint was violated
                tour.remove(locationArray.get(pc.getSecond()-1)); //remove the second constraint
                matrix[pc.getSecond()-1] = distanceMatrix[pc.getSecond()-1];
                i--;
            }
            prev = next;
            next = chooseNearestNeighbor(matrix,prev);
            matrix = removeNode(matrix, prev);
        }
        return tour;
    }


    /**
     *  This method checks if the node being considered for the tour violates any of the constraints
     *
     * @param node      the next tour candidate
     * @return          if true, then this node can be chosen for the tour, because it doesn't violate any constraints
     */
    public boolean violatedConstraint(final int node, LinkedList<Location> currentTour) {

        for(PrecedenceConstraint pc : constraintList) {
            if(pc.getSecond() == node) { // if this node has a constraint on it
                if(!currentTour.contains(locationArray.get(pc.getFirst() - 1))) { // and the tour doesnt contain the previous node, you are in trouble
                    return true;
                }
                if(violatedConstraint(pc.getFirst(), currentTour)) return true; // check if there is a constraint for the previous node
            }
        }
        return false; // otherwise, you will live to see another day
    }

    /**
     * checks if a constraint is violated
     *
     * @return      the violated constraint
     */
    public PrecedenceConstraint returnViolatedConstraint(LinkedList<Location> currentTour) {
        Location first,second;

        for(PrecedenceConstraint pc : constraintList) {
            first = locationArray.get(pc.getFirst() - 1);
            second = locationArray.get(pc.getSecond() - 1);

            if (currentTour.contains(first) && currentTour.contains(second) && currentTour.indexOf(first) > currentTour.indexOf(second)) {
                return pc;
            }
        }
        return null;
    }

    /**
     * checks if there are any unvisited nodes left in the matrix
     *
     *
     * @param matrix        list of unvisited NN
     * @param node          current node
     * @return              [true/false] anything left to explore
     */
    public boolean nodeVisited(Double[][] matrix, int node) {
        for(int i = 0; i < matrix[0].length; i++) {
            if(matrix[node][i] != Double.POSITIVE_INFINITY)
                return false;
        }
        return true;
    }

    /**
     * Removes a node from the NN matrix, by filling its data with POS INF
     *
     *
     * @param matrix    original matrix
     * @param node      node to be removed
     * @return          new matrix without the node data
     */
    public Double[][] removeNode(Double[][] matrix, int node) {
        Double[][] newMatrix = copyMatrix(matrix);
        for (int i = 0; i < matrix[0].length; i++) {
            newMatrix[node][i] = Double.POSITIVE_INFINITY;
        }
        return newMatrix;
    }

    /**
     * @param m The array we want to copy
     * @return Returns a copy of the array m
     */
    private Double[][] copyMatrix(Double[][] m) {
        Double[][] c = new Double[m.length][m[0].length];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                c[i][j] = new Double(m[i][j]);
            }
        }

        return c;
    }


    /**
     * Calculates the next nearest neighbor
     *
     *
     * @param matrix    NN matrix
     * @param node      last visited node
     * @return          node's nearest neighbor
     */
    public int chooseNearestNeighbor(Double[][] matrix, int node) {

        double distance = Double.POSITIVE_INFINITY;
        int next = -1;

        for (int i = 0; i < matrix[0].length; i++) {
            if(matrix[node][i] < distance && !nodeVisited(matrix, i)) {
                distance = matrix[node][i];
                next = i;
            }
        }
        return next;
    }

    /**
     * calculates the cost of the tour
     *
     *
     * @param tour      current tour
     * @return          cost
     */
    private double cost(List<Location> tour) {
        double sum = 0.0;

        for (int i = 0; i < tour.size() - 1; i++) {
            sum += tour.get(i).distanceTo(tour.get(i+1));
        }

        //add the first node distance again
        sum += tour.get(tour.size()-1).distanceTo(tour.get(0));
        return sum;
    }

    /**
     * Choose the next NN node to go to
     *
     * @param neighborRun       where we have been
     * @return                  where we will go
     */
    public Location chooseNextNode(LinkedList<Location> neighborRun) {

        int id = 0;
        double dist = Double.MAX_VALUE;

        //get the closest node
        for (int i = 1; i <= locationArray.size(); i++) {

            if(!neighborRun.contains(locationArray.get(i-1)) && !violatedConstraint(i, neighborRun)) //if the node is not in the tour yes
            {
                double distTOthisNode = distanceMatrix[neighborRun.get(neighborRun.size()-1).getCityId()-1][i-1];

                if(dist > distTOthisNode) {
                    dist = distTOthisNode;
                    id = i;
                }
            }
        }
        return locationArray.get(id-1);
    }

}