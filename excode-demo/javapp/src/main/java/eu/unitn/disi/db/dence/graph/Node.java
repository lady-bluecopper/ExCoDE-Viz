package eu.unitn.disi.db.dence.graph;

import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;

/**
 *
 * @author bluecopper
 */
public class Node {
    
    private int nodeId;
    private Object nodeLabel;
    private HashIntSet reachableNodes;
    
    public Node(int nodeId, Object nodeLabel) {
        this.nodeId = nodeId;
        this.nodeLabel = nodeLabel;
        this.reachableNodes = HashIntSets.newMutableSet();
    }

    public int getNodeId() {
        return nodeId;
    }
    
    public Object getNodeLabel() {
        return nodeLabel;
    }
    
    public void setNodeLabel(Object nodeLabel) {
        this.nodeLabel = nodeLabel;
    }
    
    public int getDegree() {
        return reachableNodes.size();
    }
    
    public void addReachableNode(int node) {
        this.reachableNodes.add(node);
    }
    
    public void removeReachableNode(int node) {
        this.reachableNodes.remove(node);
    }
    
    public boolean isReachable(int node) {
        return reachableNodes.contains(node);
    }
    
    public HashIntSet getReachableNodes() {
        return reachableNodes;
    }
    
    public HashIntSet getReachableNodes(HashIntSet allowedNodes) {
        HashIntSet reachable = HashIntSets.newMutableSet(reachableNodes);
        reachable.retainAll(allowedNodes);
        return reachable;
    }
    
}
	