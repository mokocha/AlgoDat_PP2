package ads2.ss14.etsppc;

import java.util.*;

public class ETSPPC extends AbstractETSPPC {

    private final ArrayList<Location> locationArray;
    private final ArrayList<PrecedenceConstraint> constraintList;
    private final HashMap<Integer, Location> locationMap;
    private final double threshold;
    private double lowerBound;
    private double upperBound;

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

    public double calculateUpperBound(List<Location> solution) {
        return Main.calcObjectiveValue(solution);
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

//    /**
//     * create an identical matrix copy for further analysis
//     *
//     * @param matrix        original matrix
//     * @return              copy
//     */
//    public Double[][] duplicateMatrix(Double[][] matrix) {
//        Double[][] newMatrix = new Double[matrix.length][matrix[0].length];
//        for (int i = 0; i < matrix.length; i++) {
//            for (int j = 0; j < matrix[0].length; j++) {
//                newMatrix[i][j] = new Double(matrix[i][j]);
//            }
//        }
//        return newMatrix;
//    }

    /**
     * checks if this node doesnt have any first constraints on it
     *
     * @param startNode     node to be checked
     * @return              true, if this node doesn't have any nodes before it
     */
    public boolean canThisBeAstartNode(int startNode) {
        //TODO optimize by returning a list of possible start nodes instead

        for (int i = 0; i < constraintList.size(); i++) {
            if(constraintList.get(i).getSecond() == startNode) return false;
        }
        return true;
    }

    /**
     * returns the row corresponding to the distance matrix from this node
     *
     * @param row       where we are
     * @return          how far everything else is
     */
    public Double[] getRow(int row) {

        Double[] distances = new Double[locationArray.size()];

        for (int i = 0; i < locationArray.size(); i++) distances[i] = distanceMatrix[row-1][i];

        return distances;
    }

    //TODO create a duplicate, where a list with already visited nodes can be supplied
    public double nearestNeighbourUpperBound(int node) {

        LinkedList<Location> neighborRun = new LinkedList<Location>();
        neighborRun.add(locationArray.get(node-1));

        while(neighborRun.size() < locationArray.size()) {
            neighborRun.add(chooseNextNode(neighborRun));
        }
        System.out.println(calculateUpperBound(neighborRun) +"\t" + neighborRun.toString());
        return calculateUpperBound(neighborRun);
    }


    public void branchAndBound(int node, LinkedList<Location> currentTour) {

        //System.out.println(currentTour.toString() + "\t trying to add " + node);

        //is this solution above the upper bound? should i check at the end?
        if(calculateUpperBound(currentTour) >= upperBound) {
            //System.out.println("ending \t" + calculateUpperBound(currentTour));
            return;
        }

        double t1 = calculateUpperBound(currentTour);
        double t2 = calculateLocalLowerBound(currentTour);
        double t3 = t1 + t2;

        //TODO remove comment to win
        if(theoreticalCosts(currentTour) > upperBound) {
            //if(currentTour.get(0).getCityId() == 2) System.out.println(calculateUpperBound(currentTour) + "\t"  + calculateLocalLowerBound(currentTour) + "\t" + t3 + "\t" + upperBound + "\t" + currentTour.toString());
            return;
        }

        if(!currentTour.contains(node)) //is this node in the tour already?
        {
            if(!violatedConstraint(node, currentTour)) //can this node be picked?
            {
                currentTour.add(locationMap.get(node));

                //if no more nodes left, and this solution is better than currentTour, set the solution
                if(currentTour.size() == locationArray.size()) {
                    if(bestTour.size() == 0) bestTour = new LinkedList<Location>(currentTour);
                    else if(calculateUpperBound(currentTour) < calculateUpperBound(bestTour)) {
                        bestTour = new LinkedList<Location>(currentTour);
                        upperBound = calculateUpperBound(bestTour);
                    }
                    //System.out.println("ending with solution " + currentTour.toString());
                    return;
                }

                for(int i = 1; i <= locationArray.size(); i++) //CHOOSE NEXT NODE AND CONTINUE!!!!!!
                { //System.out.println(".");
                    if(!currentTour.contains(locationArray.get(i-1)))
                    {
                        LinkedList<Location> temp = new LinkedList<Location>(currentTour);
                        //System.out.println(currentTour.toString() + "\t branching to " + i);
                        if(!violatedConstraint(i, currentTour)) branchAndBound(i, temp);
                        //else System.out.println(currentTour.toString() + "\t Constraint violation " + i);

                    } //else System.out.println(currentTour.toString() + "\t already contains " + i);
                }
            } //else System.out.println(currentTour.toString() + "\t Constraint violation " + node);
        } //else System.out.println(currentTour.toString() + "\t already contains " + node);
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

//    public void calculateLowerBound() {
//        double lowestBound = 0.0;
//
//        for (int i = 0; i < locationArray.size(); i++) {
//            double smallest = Double.MAX_VALUE;
//
//            for (int j = 0; j < locationArray.size(); j++) {
//                double localSmall = distanceMatrix[i][j];
//
//                if (smallest > localSmall && localSmall != 0.0) {
//                    smallest = localSmall;
//                }
//            }
//
//            lowestBound += smallest;
//        }
//        lowerBound = lowestBound;
//    }

    public double calculateLocalLowerBound(LinkedList<Location> alreadyVisited) {
        double lowestBound = 0.0;

        ArrayList<Integer> visited = new ArrayList<Integer>();

        for (int i = 0; i < alreadyVisited.size(); i++) {
            visited.add(alreadyVisited.get(i).getCityId());
        }

        for (int i = 0; i < locationArray.size(); i++) {

            if(!visited.contains(i+1)){
                double smallest = Double.MAX_VALUE;

                for (int j = 0; j < locationArray.size(); j++) {

                    if(!visited.contains(i+1) && !visited.contains(j+1)) {

                        double localSmall = distanceMatrix[i][j];

                        if (smallest > localSmall && localSmall != 0.0) {
                            smallest = localSmall;
                        }
                    }
                }
                lowestBound += smallest;
            }
        }
        return lowestBound;
    }

    public double theoreticalCosts(LinkedList<Location> alreadyVisited) {

        LinkedList<Location> theory = new LinkedList<Location>(alreadyVisited);

        while (theory.size() < locationArray.size()) theory.add(chooseNextNode(theory));

        return calculateUpperBound(theory);
    }
}