package eu.unitn.disi.db.dence.sparkutils;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.countDistinct;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.log;

/**
 *
 * @author bluecopper
 */
public class Utils {
    
    public static Dataset<Row> tfIdf(Dataset<Row> docs, String docName, String wordName) {
        final long numDocs = docs.groupBy(col(docName)).count().count();
        return docs.groupBy(col(wordName))
                .agg(count(col(docName)).as("frequency"))
                .select(col(wordName), log(lit(numDocs).divide(col("frequency"))).as("tfidf"));
    }
    
    public static Dataset<Row> freq(Dataset<Row> docs, String docName, String wordName) {
        final long numDocs = docs.groupBy(col(docName)).count().count();
        return docs.groupBy(col(wordName))
                .agg(countDistinct(col(docName)).as("count"))
                .select(col(wordName), col("count"), col("count").divide(lit(numDocs)).as("freq"));
    }
    
}
