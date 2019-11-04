package eu.unitn.disi.db.dence.graph;

/**
 *
 * @author bluecopper
 */
public class LabeledEdge extends Edge {
    
    private Object edgeLabel;
    
    public LabeledEdge(int edgeId, int src, int dst, Object edgeLabel) {
        super(edgeId, src, dst);
        this.edgeLabel = edgeLabel;
    }
    
    public Object getEdgeLabel() {
        return edgeLabel;
    }
    
    public void setEdgeLabel(Object label) {
        this.edgeLabel = label;
    }
    
    @Override
    public boolean equals(Object other) {
        //check for self-comparison
        if (this == other) {
            return true;
        }
        //actual comparison
        if (other instanceof LabeledEdge) {
            LabeledEdge o = (LabeledEdge) other;
            return (this.edgeId == o.edgeId);
        } else {
            throw new IllegalArgumentException("Objects not comparable");
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.edgeId;
        return hash;
    }
    
    @Override
    public String toString() {
        return "(" + this.src + "," + this.dst + ") - " + this.edgeLabel;
    }
    
}