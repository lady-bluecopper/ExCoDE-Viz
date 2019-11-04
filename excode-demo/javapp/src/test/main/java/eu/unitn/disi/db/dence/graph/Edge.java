package eu.unitn.disi.db.dence.graph;

/**
 *
 * @author bluecopper
 */
public class Edge {
    
    protected final int edgeId;
    protected final int src;
    protected final int dst;
    
    public Edge(int edgeId, int src, int dst) {
        this.edgeId = edgeId;
        this.src = src;
        this.dst = dst;
    }
    
    public int getEdgeID() {
        return edgeId;
    }
    
    public int getSrc() {
        return src;
    }
    
    public int getDst() {
        return dst;
    }
    
}
