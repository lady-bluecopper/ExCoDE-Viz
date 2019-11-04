package eu.unitn.disi.db.dence.graph;

import com.google.common.collect.Lists;
import com.koloboke.collect.set.hash.HashIntSet;

/**
 *
 * @author bluecopper
 */
public class BinaryDynamicGraph extends DynamicGraph {
    
    public BinaryDynamicGraph(int numNodes, int numEdges, int snapshots, int numEdgesPerSnap) {
        super(numNodes, numEdges, snapshots, numEdgesPerSnap);
        this.edges = Lists.newArrayList();
    }
    
    public void addEdge(int id, int src, int dst, int label, HashIntSet series) {
        edges.add(new BinaryDynamicEdge(id, src, dst, label, series));
        nodes[src].addReachableNode(dst);
        nodes[dst].addReachableNode(src);
        nodeEdges[src][dst] = id;
        nodeEdges[dst][src] = id;
    }

    @Override
    public void addEdge(int id, int src, int dst) {
        addEdge(id, src, dst, 1, null);
    }
    
}
