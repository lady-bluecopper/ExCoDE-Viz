package eu.unitn.disi.db.dence.correlation;

/**
 *
 * @author bluecopper
 */
public interface CorrelationMeasure {
    
    public double computeCorrelation(double[] e1, double[] e2);
    
}
