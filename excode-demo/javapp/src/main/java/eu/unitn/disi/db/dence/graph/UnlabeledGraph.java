package eu.unitn.disi.db.dence.graph;

import com.google.common.collect.Lists;

/**
 *
 * @author bluecopper
 */
public class UnlabeledGraph extends Graph<Edge>{
    
    public UnlabeledGraph(int numNodes, int numEdges) {
        super(numNodes, numEdges);
        this.edges = Lists.newArrayList();
    }

    @Override
    public void addEdge(int id, int src, int dst) {
        edges.add(new Edge(id, src, dst));
        nodes[src].addReachableNode(dst);
        nodes[dst].addReachableNode(src);
        nodeEdges[src][dst] = id;
        nodeEdges[dst][src] = id;
    }
    
}
