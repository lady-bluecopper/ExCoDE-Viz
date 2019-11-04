package eu.unitn.disi.db.dence.graph;

import com.google.common.collect.Lists;
import com.koloboke.collect.map.hash.HashIntDoubleMap;

/**
 *
 * @author bluecopper
 */
public class WeightedDynamicGraph extends DynamicGraph {
    
    public WeightedDynamicGraph(int numNodes, int numEdges, int snapshots, int numEdgesPerSnap) { 
        super(numNodes, numEdges, snapshots, numEdgesPerSnap);
        this.edges = Lists.newArrayList();
    }

    public void addEdge(int id, int src, int dst, int label, HashIntDoubleMap series) {
        edges.add(new WeightedDynamicEdge(id, src, dst, label, series));
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
