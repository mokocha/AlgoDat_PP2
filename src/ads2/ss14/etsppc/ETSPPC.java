package ads2.ss14.etsppc;

import java.util.*;

public class ETSPPC extends AbstractETSPPC {

    private final ArrayList<Location> locationArray;
    private final ArrayList<PrecedenceConstraint> constraintList;
    private final HashMap<Integer, Location> locationMap;
    private double lowerBound;
    private double upperBound;

    private Double[][] distanceMatrix;
    private LinkedList<Location> bestTour;

	public ETSPPC(ETSPPCInstance instance) {

        constraintList = (ArrayList<PrecedenceConstraint>) instance.getConstraints();
        locationMap = (HashMap<Integer, Location>) instance.getAllLocations();
        locationArray = new ArrayList<Location>(instance.getAllLocations().values());

        bestTour = new LinkedList<Location>();
        distanceMatrix = new Double[locationArray.size()][locationArray.size()];

        calculateClosestNodes();
    }

	@Override
	public void run() {

        lowerBound = 0.0;
        calculateLowerBound();
        upperBound = nearestNeighbourUpperBound(1);

        /** branch and bound **/
        for(int node = 1; node <= locationMap.size(); node++) {
            if(canThisBeAstartNode(node)) {
                //calculate upper bound for this start node
                LinkedList<Location> currentTour = new LinkedList<Location>();
                branchAndBound(node, currentTour);
            }
        }
        setSolution(calculateUpperBound(bestTour), bestTour);
	}

    public void branchAndBound(int node, LinkedList<Location> currentTour) {

        //System.out.println(currentTour.toString() + "\t trying to add " + node);

        //is this solution above the upper bound? should i check at the end?
        if(calculateUpperBound(currentTour) >= upperBound) {
            //System.out.println("ending \t" + calculateUpperBound(currentTour));
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
                    else if(calculateUpperBound(currentTour) < calculateUpperBound(bestTour)) bestTour = new LinkedList<Location>(currentTour);
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
     *  This method checks if the node being considered for the tour violates any of the constraints
     *
     * @param node      the next tour candidate
     * @return          if true, then this node can be chosen for the tour, because it doesn't violate any constraints
     */
    public boolean violatedConstraint(final int node, LinkedList<Location> currentTour) {

        for(PrecedenceConstraint pc : constraintList) {
            if(pc.getSecond() == node) { // if this node has a constraint on it
                if(!currentTour.contains(locationArray.get(pc.getFirst() - 1))) { // and the tour doesnt contain the previous node, you are fucked
                    return true;
                }
                if(violatedConstraint(pc.getFirst(), currentTour)) return true; // check if there is a constraint for the previous node
            }
        }
        return false; // otherwise, you will live to see another day
    }

    public double calculateUpperBound(List<Location> solution) {
        return Main.calcObjectiveValue(solution);
    }

    /**
     * Calculate the closest nodes for each node
     */
    public void calculateClosestNodes() {

        for (int i = 0; i < locationArray.size(); i++)
        {
            for (int j = 0; j < locationArray.size(); j++)
            {
                distanceMatrix[i][j] = locationArray.get(i).distanceTo(locationArray.get(j));
            }
        }
    }

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

    public double nearestNeighbourUpperBound(int node) {

        LinkedList<Location> neighborRun = new LinkedList<Location>();
        neighborRun.add(locationArray.get(node-1));

        while(neighborRun.size() < locationArray.size()) {
            chooseNextNode(neighborRun);
        }
        return calculateUpperBound(neighborRun);
    }

    /**
     * Choose the next node to go to
     *
     * @param neighborRun       where we have been
     * @return                  where we will go
     */
    public void chooseNextNode(LinkedList<Location> neighborRun) {

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
        neighborRun.add(locationArray.get(id-1));
    }

    public void calculateLowerBound() {
        double lowestBound = 0.0;

        for (int i = 0; i < locationArray.size(); i++) {
            double smallest = Double.MAX_VALUE;

            for (int j = 0; j < locationArray.size(); j++) {
                double localSmall = distanceMatrix[i][j];

                if (smallest > localSmall && localSmall != 0.0) {
                    smallest = localSmall;
                }
            }

            lowestBound += smallest;
        }
        lowerBound = lowestBound;
    }
}