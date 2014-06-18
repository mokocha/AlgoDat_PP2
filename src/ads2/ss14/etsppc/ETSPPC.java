package ads2.ss14.etsppc;

import java.util.*;

public class ETSPPC extends AbstractETSPPC {

    private final ArrayList<Location> locationArray;
    private final ArrayList<PrecedenceConstraint> constraintList;
    private final HashMap<Integer, Location> locationMap;
    private final double threshold;

    private Double[][] distanceMatrix;
    private LinkedList<Location> bestTour;

	public ETSPPC(ETSPPCInstance instance) {

        constraintList = (ArrayList<PrecedenceConstraint>) instance.getConstraints();
        locationMap = (HashMap<Integer, Location>) instance.getAllLocations();
        locationArray = new ArrayList<Location>(instance.getAllLocations().values());
        threshold = instance.getThreshold();

        bestTour = new LinkedList<Location>();
        distanceMatrix = new Double[locationArray.size()][locationArray.size()];

        calculateClosestNodes();
    }

	@Override
	public void run() {

        /** branch and bound **/
        for(int node = 1; node <= locationMap.size(); node++) {
            if(canThisBeAstartNode(node)) {
                LinkedList<Location> currentTour = new LinkedList<Location>();
                branchAndBound(node, currentTour);
            }
        }
        System.out.println("solution " + bestTour.toString());
        setSolution(calculateUpperBound(bestTour), bestTour);
	}

    public void branchAndBound(int node, LinkedList<Location> currentTour) {

        System.out.println(currentTour.toString() + "\t trying to add " + node);

        //is this solution above the upper bound? should i check at the end?
        if(calculateUpperBound(currentTour) >= threshold) {
            System.out.println("ending \t" + calculateUpperBound(currentTour));
            return;
        }

        if(!currentTour.contains(node)) //is this node in the tour already?
        {
            if(!violatedConstraint(node, currentTour)) //can this node be picked?
            {
                currentTour.add(locationMap.get(node));

                //if no more nodes left, and this solution is better than currentTour, set the solution
                if(currentTour.size() == locationArray.size() && calculateUpperBound(currentTour) < calculateUpperBound(bestTour)) {
                    bestTour = currentTour;
                    System.out.println("ending with solution " + currentTour.toString());
                    return;
                }

                for(int i = 1; i <= locationArray.size(); i++) //CHOOSE NEXT NODE AND CONTINUE!!!!!!
                { //System.out.println(".");
                    if(!currentTour.contains(locationArray.get(i-1)))
                    {
                        LinkedList<Location> temp = new LinkedList<Location>(currentTour);
                        System.out.println(currentTour.toString() + "\t branching to " + i);
                        if(!violatedConstraint(i, currentTour)) branchAndBound(i, temp);
                        else System.out.println(currentTour.toString() + "\t Constraint violation " + i);

                    } else System.out.println(currentTour.toString() + "\t already contains " + i);
                }
            } else System.out.println(currentTour.toString() + "\t Constraint violation " + node);
        } else System.out.println(currentTour.toString() + "\t already contains " + node);
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

//    /**
//     * Choose the next node to go to
//     *
//     * @param currentTour       where we have been
//     * @param destinations      where we can go
//     * @return                  where we want to go
//     */
//    public int chooseNextNode(LinkedList<Location> currentTour, Double[] destinations) {
//
//        for (int i = 0; i < locationArray.size(); i++) {
//            if(!currentTour.contains(i+1)) {
//
//            }
//        }
//        return 0;
//    }
}
