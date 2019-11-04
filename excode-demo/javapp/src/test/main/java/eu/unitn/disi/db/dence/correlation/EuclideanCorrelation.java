package eu.unitn.disi.db.dence.correlation;

import eu.unitn.disi.db.dence.utils.Pair;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

/**
 *
 * @author bluecopper
 */
public class EuclideanCorrelation implements CorrelationMeasure {
    
    EuclideanDistance measure;
    
    public EuclideanCorrelation() {
        this.measure = new EuclideanDistance();
    }

    @Override
    public double computeCorrelation(double[] e1, double[] e2) {
        return 1 / (1 + measure.compute(e1, e2));
    }

}