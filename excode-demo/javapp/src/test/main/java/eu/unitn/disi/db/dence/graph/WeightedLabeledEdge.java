package eu.unitn.disi.db.dence.graph;

/**
 *
 * @author bluecopper
 */
public class WeightedLabeledEdge extends LabeledEdge {
    
    private double weight;
    
    public WeightedLabeledEdge(int edgeId, int src, int dst, Object edgeLabel) {
        super(edgeId, src, dst, edgeLabel);
        this.weight = 0;
    }
    
    public WeightedLabeledEdge(int edgeId, int src, int dst, Object edgeLabel, double weight) {
        super(edgeId, src, dst, edgeLabel);
        this.weight = weight;
    }
    
    public WeightedLabeledEdge(LabeledEdge edge, double weight) {
        super(edge.getEdgeID(), edge.getSrc(), edge.getDst(), edge.getEdgeLabel());
        this.weight = weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public void incrementWeight() {
        this.weight += 1;
    }
    
    public void incrementWeight(int c) {
        this.weight += c;
    }
    
}
