package eu.unitn.disi.db.dence.correlation;

import java.util.Arrays;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 *
 * @author bluecopper
 */
public class PearsonCorrelation implements CorrelationMeasure {
    
    PearsonsCorrelation measure;
    
    public PearsonCorrelation() {
        measure = new PearsonsCorrelation();
    }

    public double computeCorrelation(double[] e1, double[] e2) {
        double[] alwaysHere = new double[e1.length];
        Arrays.fill(alwaysHere, 1);
        if (e1.length < 2 || Arrays.equals(alwaysHere, e1) || Arrays.equals(alwaysHere, e2)) {
            return 0;
        }
        return measure.correlation(e1, e2);
    }
    
}
