package ads2.ss14.etsppc;

import java.util.*;

public class ETSPPC extends AbstractETSPPC {

    private final ArrayList<Location> locationArray;
    private final ArrayList<PrecedenceConstraint> constraintList;
    private final HashMap<Integer, Location> locationMap;
    private final double threshold;

    LinkedList<Location> bestTour;
    private HashMap<Integer, Integer[]> closestNodes;

	public ETSPPC(ETSPPCInstance instance) {

        constraintList = (ArrayList<PrecedenceConstraint>) instance.getConstraints();
        locationMap = (HashMap<Integer, Location>) instance.getAllLocations();
        locationArray = new ArrayList<Location>(instance.getAllLocations().values());
        threshold = instance.getThreshold();

        bestTour = new LinkedList<Location>();
        closestNodes = new HashMap<Integer, Integer[]>();

        calculateClosestNodes();
    }

	@Override
	public void run() {

        /** branch and bound **/
        for(int node = 1; node <= locationMap.size(); node++) {
            if(canThisBeAstartNode(node)) {
                LinkedList<Location> currentTour = new LinkedList<Location>();
                currentTour.add(locationMap.get(node));
                branchAndBound(node, currentTour);
            }
        }
	}

    public void branchAndBound(final int node, LinkedList<Location> tour) {
        //pick the closest to this node
        Integer[] neighbors = closestNodes.get(node);

        for(int i = 0; i < neighbors.length; i++)
        {
            if(!tour.contains(neighbors[i])) //is this node in the tour already?
            {
                if(!violatedConstraint(neighbors[i], tour)) //can this node be picked? //TODO, if not picked, add later
                {
                    tour.add(locationMap.get(neighbors[i])); //TODO branch here?
                } //if not violated constraint but some other shit has to come first
            }
        }

        //if no more nodes left, and this solution is better than currentTour, set the solution
        if(tour.size() == locationArray.size()) { //TODO and current upper bound check
            if(calculateUpperBound(tour) <= calculateUpperBound(bestTour)) bestTour = tour; //TODO is this check necessary?
            return;
        }

        //is this solution above the upper bound? should i check at the end?
        if(calculateUpperBound(tour) >= threshold) return;
    }

    /**
     *  This method checks if the node being considered for the tour violates any of the constraints
     *
     * @param node      the next tour candidate
     * @return          if true, then this node can be chosen for the tour, because it doesn't violate any constraints
     */
    public boolean violatedConstraint(final int node, LinkedList<Location> tour) {

        for(PrecedenceConstraint pc : constraintList) {
            if(pc.getSecond() == node) { // if this node has a constraint on it
                if(!tour.contains(pc.getFirst())) { // and the tour doesnt contain the previous node, you are fucked
                    return true;
                }
                if(violatedConstraint(pc.getFirst(), tour)) return true; // check if there is a constraint for the previous node
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

        for(int i = 0; i < locationArray.size(); i++) {
            Integer[] greedyNodes = new Integer[locationMap.size()-1];
            Map<Double, Integer> distances = new HashMap<Double, Integer>();

            for(int j = 0; j < locationArray.size(); j++) //order J nodes depending on their distance to I
            {
                if(j != i) distances.put(locationArray.get(j).distanceTo(locationArray.get(i)), j+1);
            }

            //order the nodes from the HashMap based on distance into an Integer[]
            Map<Double, Integer> treeMap = new TreeMap<Double, Integer>(distances);

            int x = 0;
            for(Map.Entry entry : treeMap.entrySet()) {
                greedyNodes[x]=(Integer)entry.getValue();
                x++;
            }
            closestNodes.put(i+1,greedyNodes);
        }
    }

    /**
     * checks if this node doesnt have any first constraints on it
     *
     * @param startNode     node to be checked
     * @return              true, if this node doesn't have any nodes before it
     */
    public boolean canThisBeAstartNode(int startNode) {

        for (int i = 0; i < constraintList.size(); i++) {
            if(constraintList.get(i).getSecond() == startNode) return false;
        }
        return true;
    }
}
