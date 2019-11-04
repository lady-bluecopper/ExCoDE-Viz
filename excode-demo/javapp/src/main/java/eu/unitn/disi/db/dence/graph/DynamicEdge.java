package eu.unitn.disi.db.dence.graph;

/**
 *
 * @author bluecopper
 */
public abstract class DynamicEdge extends LabeledEdge {
    
    protected double support;

    public DynamicEdge(int edgeId, int src, int dst, Object edgeLabel) {
        super(edgeId, src, dst, edgeLabel);
    }
    
    public abstract boolean existsInT(int t);
    
    public abstract double computeSupport();
    
    public double getSupport() {
        return support;
    }
    
}
