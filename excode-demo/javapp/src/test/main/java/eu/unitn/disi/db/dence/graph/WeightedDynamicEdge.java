package eu.unitn.disi.db.dence.graph;

import com.koloboke.collect.map.hash.HashIntDoubleMap;

/**
 *
 * @author bluecopper
 */
public class WeightedDynamicEdge extends DynamicEdge {
    
    protected HashIntDoubleMap edgeWeights;
    
    public WeightedDynamicEdge(int edgeId, int src, int dst, Object edgeLabel, HashIntDoubleMap edgeWeights) {
        super(edgeId, src, dst, edgeLabel);
        this.edgeWeights = edgeWeights;
        this.support = computeSupport();
    }
    
    public double getEdgeWeight(int t) {
        return edgeWeights.getOrDefault(t, 0.0);
    }
    
    public double computeSupport() {
        return edgeWeights.values().stream().mapToDouble(x -> x).sum();
    }
    
    public void zetaNormalization() {
        final double mean = support / edgeWeights.size();
        double sum = edgeWeights.values().stream().mapToDouble(d -> Math.pow(d - mean, 2)).sum();
        double var = Math.sqrt(sum / edgeWeights.size());
        edgeWeights.keySet().stream().forEach(i -> edgeWeights.put((int) i, (edgeWeights.get(i) - mean) / var));
    }

    public boolean existsInT(int t) {
        return edgeWeights.getOrDefault(t, 0.0) > 0.0;
    }
    
    public HashIntDoubleMap getEdgeSeries() {
        return edgeWeights;
    }
}
