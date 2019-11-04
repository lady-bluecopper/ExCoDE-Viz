package eu.unitn.disi.db.dence.sparkutils;

import eu.unitn.disi.db.dence.utils.Settings;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.SparkSession.Builder;
import org.apache.spark.sql.types.DataTypes;

/**
 *
 * @author bluecopper
 */
public class SparkUtils {

    public static void registerUDFs(SparkSession ss) {
        try {
            ss.udf().register("null_to_none", UDFs.null_to_none, DataTypes.StringType);
            ss.udf().register("string_to_boolean", UDFs.string_to_boolean, DataTypes.BooleanType);
            ss.udf().register("string_to_date", UDFs.string_to_date, DataTypes.StringType);
            ss.udf().register("unix_timestamp_to_day", UDFs.unix_timestamp_to_day, DataTypes.StringType);
            ss.udf().register("string_full_to_date", UDFs.string_full_to_date, DataTypes.StringType);
            ss.udf().register("from_timestamp_to_date", UDFs.from_timestamp_to_date, DataTypes.StringType);
            ss.udf().register("from_timestamp_to_date_and_time", UDFs.from_timestamp_to_date_and_time, DataTypes.StringType);
            ss.udf().register("long_to_string", UDFs.long_to_string, DataTypes.StringType);
            ss.udf().register("sum_actions", UDFs.sum_actions, DataTypes.IntegerType);
            ss.udf().register("sum_actions_long", UDFs.sum_actions_long, DataTypes.LongType);
            ss.udf().register("sum_actions_int", UDFs.sum_actions_int, DataTypes.IntegerType);
            ss.udf().register("timestamp_to_min", UDFs.timestamp_to_min, DataTypes.StringType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SparkSession getSparkSession() {
        Builder builder = SparkSession.builder()
                .appName("DenCE")
                .config("spark.driver.maxResultSize", "0")
                .config("spark.hadoop.validateOutputSpecs", "false")
                .config("spark.network.timeout", "1200s")
                .config("spark.sql.broadcastTimeout", "1200")
                .config("dfs.client.block.write.replace-datanode-on-failure.policy", "ALWAYS")
                .config("dfs.client.block.write.replace-datanode-on-failure.best-effort", "true")
                .config("spark.sql.tungsten.enabled", "false")
                .config("spark.worker.cleanup.enabled", "true");
        if (Settings.local) {
            System.out.println("Running locally...");
            return builder.config("spark.executor.memory", "90g")
                    .config("spark.driver.memory", "90g")
                    .master("local[*]")
                    .getOrCreate();
        }
        System.out.println("Running remotely...");
        return builder.getOrCreate();
    }

}
