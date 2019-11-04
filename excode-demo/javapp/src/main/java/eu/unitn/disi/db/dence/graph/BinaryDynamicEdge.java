package eu.unitn.disi.db.dence.graph;

import com.koloboke.collect.set.hash.HashIntSet;

/**
 *
 * @author bluecopper
 */
public class BinaryDynamicEdge extends DynamicEdge {
    
    protected HashIntSet edgeSeries;

    public BinaryDynamicEdge(int edgeId, int src, int dst, Object edgeLabel, HashIntSet edgeSeries) {
        super(edgeId, src, dst, edgeLabel);
        this.edgeSeries = edgeSeries;
        this.support = computeSupport();
    }

    public boolean existsInT(int t) {
        return edgeSeries.contains(t);
    }

    public double computeSupport() {
        return edgeSeries.size();
    }
    
    public HashIntSet getEdgeSeries() {
        return edgeSeries;
    }
    
}
