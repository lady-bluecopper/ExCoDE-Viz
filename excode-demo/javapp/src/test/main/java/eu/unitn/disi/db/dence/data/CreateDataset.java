package eu.unitn.disi.db.dence.data;

import eu.unitn.disi.db.dence.sparkutils.SparkUtils;
import eu.unitn.disi.db.dence.sparkutils.Utils;
import eu.unitn.disi.db.dence.utils.CommandLineParser;
import eu.unitn.disi.db.dence.utils.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import static org.apache.spark.sql.functions.array;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import static org.apache.spark.sql.functions.avg;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;
import static org.apache.spark.sql.functions.callUDF;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.collect_list;
import static org.apache.spark.sql.functions.collect_set;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.countDistinct;
import static org.apache.spark.sql.functions.monotonically_increasing_id;
import static org.apache.spark.sql.functions.sum;
import static org.apache.spark.sql.functions.unix_timestamp;

/**
 *
 * @author bluecopper
 */
public class CreateDataset {
    
    static StructType schema_ids = new StructType(new StructField[]{
            new StructField("src_id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("dst_id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("time series", DataTypes.StringType, false, Metadata.empty())});
    
    static StructType schema_names = new StructType(new StructField[]{
            new StructField("src_id", DataTypes.StringType, false, Metadata.empty()),
            new StructField("dst_id", DataTypes.StringType, false, Metadata.empty()),
            new StructField("time series", DataTypes.StringType, false, Metadata.empty())});

    public static void main(String[] args) {
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);
        // Getting Parameters 
        CommandLineParser.parse(args);
        // Create Spark Session
        SparkSession ss = SparkUtils.getSparkSession();
        SparkUtils.registerUDFs(ss);
        // Create Graph
        switch (Settings.fileType) {
            case "tweets":
                createTwitterGraph(ss);
                break;
            case "tweet_hashtags_bruno":
                createTwitterBrunoGraph(ss);
                break;
            case "mobile":
                createPrunedMobileDataGraph(ss);
                break;
            case "konect":
                createKonectGraph(ss);
                break;
            case "prune":
                pruneInfrequent(ss);
                break;
            case "sample":
                sampleGraph(ss);
                break;
            case "wc":
                createWCGraph(ss);
                break;
            default:
                throw new IllegalArgumentException("banana");
        }
        ss.close();
    }
    
    public static void pruneInfrequent(SparkSession ss) {
        Dataset<Row> pruned = ss.read().option("sep", " ").schema(schema_ids)
                .csv(Settings.dataFolder + Settings.inputDataset)
                .filter((Row r) -> {
                    int ones = 0;
                    String[] present = r.getString(2).split(",");
                    for (String s : present) {
                        if (s.equals("1")) {
                            ones ++;
                        }
                    }
                    return (ones > Settings.minF) && (ones != present.length);
                });
        pruned.coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "edges_frequent_" + Settings.minF);
    }
    
    public static void createWCGraph(SparkSession ss) {
        StructType time_schema = new StructType(new StructField[]{
                    new StructField("time", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("timeID", DataTypes.LongType, false, Metadata.empty())});
        StructType output_schema = new StructType(new StructField[]{
                    new StructField("src", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("dst", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("existenceString", DataTypes.StringType, false, Metadata.empty())});
        FileType wc_type = new FileType("wc");
        Seq<Column> toKeep = JavaConversions.asScalaBuffer(wc_type.getColsToKeep()).seq();
        Dataset<Row> data = ss.read().options(wc_type.options()).schema(wc_type.schema())
                .csv(Settings.dataFolder + Settings.inputDataset)
                .select(toKeep)
                .withColumn("time", callUDF("timestamp_to_min", col("timestamp")))
                .drop(col("timestamp"));
        JavaRDD<Row> timeIDrdd = data.select("time").distinct().orderBy(col("time"))
                .toJavaRDD()
                .zipWithIndex()
                .map(x -> RowFactory.create(x._1.getString(0), x._2));
        Dataset<Row> timeIDs = ss.createDataFrame(timeIDrdd, time_schema);
        Dataset<Row> data2 = data.withColumnRenamed("object", "object2");
        Dataset<Row> joined = data.join(data2, data.col("user").equalTo(data2.col("user")).and(data.col("time").equalTo(data2.col("time"))))
                .select(data.col("object"), data2.col("object2"), data.col("time"))
                .filter(data.col("object").leq(data2.col("object2")));
        Dataset<Row> test = joined.join(timeIDs, joined.col("time").equalTo(timeIDs.col("time")))
                .select(joined.col("object"), joined.col("object2"), timeIDs.col("timeID"))
                .groupBy(col("object"), col("object2"), col("timeID"))
                .agg(count("*").as("weight"))
                .groupBy(col("object"), col("object2"))
                .agg(collect_list(array(col("weight"), col("timeID"))).as("existence"));
        test.map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    List<Seq<Long>> iter = JavaConversions.seqAsJavaList(r.getSeq(2));
                    Iterator<Seq<Long>> it = iter.iterator();
                    while (it.hasNext()) {
                         List<Long> cur = JavaConversions.seqAsJavaList(it.next());
                         builder.append("," + cur.get(0) + ":" + cur.get(1));
                    }
                    if (r.getString(0).equals(r.getString(1))) {
                        return RowFactory.create("0", r.getString(0), builder.substring(1).trim());
                    } else {
                        return RowFactory.create(r.getString(0), r.getString(1), builder.substring(1).trim());
                    }
                }, RowEncoder.apply(output_schema))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.dataFolder + "wc_weighted");
    }

    public static void createTwitterGraph(SparkSession ss) {
        //Read tweets
        FileType tweets_type = new FileType("tweets");
        Seq<Column> toKeep = JavaConversions.asScalaBuffer(tweets_type.getColsToKeep()).seq();
        Dataset<Row> tweets = ss.read().options(tweets_type.options()).schema(tweets_type.schema())
                .csv(Settings.dataFolder + Settings.inputDataset)
                .select(toKeep)
                .withColumn("timestamp", callUDF("null_to_none", unix_timestamp(callUDF("unix_timestamp_to_day", col("created_at")), "yyyy-MM-dd")))
                .drop(col("created_at"));
        //Read tweet hashtags
        FileType tweet_hashtags_type = new FileType("tweet_hashtags");
        Seq<Column> toKeep2 = JavaConversions.asScalaBuffer(tweet_hashtags_type.getColsToKeep()).seq();
        Dataset<Row> tweet_hashtags = ss.read().options(tweet_hashtags_type.options()).schema(tweet_hashtags_type.schema())
                .csv(Settings.dataFolder + Settings.inputDataset2)
                .select(toKeep2);
        //Create hashtag pairs
        Dataset<Row> tweets_with_meta = tweets.join(tweet_hashtags, tweet_hashtags.col("tweet_id").equalTo(tweets.col("tweet_id")).and(tweet_hashtags.col("user_id").equalTo(tweets.col("user_id"))))
                .select(tweets.col("tweet_id"), tweet_hashtags.col("hashtag_id"), tweets.col("timestamp"), callUDF("sum_actions", tweets.col("favorited"), tweets.col("retweeted"), tweets.col("retweet_count")).as("actions"));
        final long numTweets = tweets_with_meta.groupBy(col("tweet_id")).count().count();
        System.out.println("NUM TWEETS: " + numTweets);
        // Remove infrequent hashtags
        final Set<Long> infrequents = new HashSet<Long>(
                JavaConversions.seqAsJavaList(
                    tweets_with_meta.groupBy(col("hashtag_id"))
                    .agg(countDistinct("tweet_id").as("count"))
                    .filter((Row r) -> (r.getLong(1) / (double) numTweets) <= Settings.tfidfThreshold)
                    .groupBy()
                    .agg(collect_set(col("hashtag_id")))
                    .first()
                    .getSeq(0)
                )
        );
        tweets_with_meta.cache();
        System.out.println("INFREQUENT HASHTAGS: " + infrequents.size());
        Dataset<Row> frequent_hashtags = tweets_with_meta.filter((Row r) -> (!infrequents.contains(r.getLong(1))));
        System.out.println("FREQUENT HASHTAGS: " + frequent_hashtags.groupBy(col("hashtag_id")).count().count());
        
        Dataset<Row> frequent_hashtags_2 = frequent_hashtags.select(col("tweet_id").as("tweet_id_2"), col("hashtag_id").as("hashtag_id_2"));
        Dataset<Row> hashtag_pairs_with_popularity = frequent_hashtags.join(frequent_hashtags_2, frequent_hashtags.col("tweet_id").equalTo(frequent_hashtags_2.col("tweet_id_2")))
                .filter(frequent_hashtags.col("hashtag_id").lt(frequent_hashtags_2.col("hashtag_id_2")))
                .select(frequent_hashtags.col("tweet_id"), frequent_hashtags.col("hashtag_id"), frequent_hashtags_2.col("hashtag_id_2"), col("timestamp"), col("actions"))
                .groupBy(col("hashtag_id"), col("hashtag_id_2"), col("timestamp"))
                .agg(countDistinct(col("tweet_id")).as("numTweets"), sum(col("actions")).as("actions"))
                .select(col("hashtag_id"), col("hashtag_id_2"), callUDF("sum_actions_long", col("numTweets"), col("actions")).as("actions"), col("timestamp"));
        System.out.println("NUM HASHTAG PAIRS: " + hashtag_pairs_with_popularity.groupBy(col("hashtag_id"), col("hashtag_id_2")).count().count());
        System.out.println("NUM HASHTAG PAIRS (freq >= " + Settings.minF + "): " + hashtag_pairs_with_popularity.groupBy(col("hashtag_id"), col("hashtag_id_2"))
                .agg(countDistinct(col("timestamp")).as("appear"))
                .filter(col("appear").geq(Settings.minF))
                .count());
        // Nodes
        hashtag_pairs_with_popularity.select(col("hashtag_id"), col("hashtag_id_2"))
                .flatMap((Row t) -> {
                    List<Long> list = new ArrayList<Long>();
                    list.add(t.getLong(0));
                    list.add(t.getLong(1));
                    return list.iterator();
                }, Encoders.LONG())
                .distinct()
                .coalesce(1).write().mode(SaveMode.Overwrite).csv(Settings.outputFolder + "nodes");
        System.out.println("Node file created");
        // Edges
        final Seq<String> timestamps = hashtag_pairs_with_popularity.select(col("timestamp"))
                .distinct()
                .orderBy(col("timestamp"))
                .groupBy()
                .agg(collect_list(col("timestamp")))
                .first()
                .getSeq(0);
        // Edges with binary time series
        hashtag_pairs_with_popularity.groupBy(col("hashtag_id"), col("hashtag_id_2"))
                .agg(collect_list(col("timestamp")).as("time_list"))
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Set<String> time_set = new HashSet<String>(JavaConversions.seqAsJavaList(r.getSeq(2)));
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    while (it.hasNext()) {
                        String cur = it.next();
                        if (time_set.contains(cur)) {
                            builder.append(",1");
                        } else {
                            builder.append(",0");
                        }
                    }
                    return RowFactory.create(r.getLong(0), r.getLong(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema_ids))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "twitter_time_series");
        // Edges with weighted series
        hashtag_pairs_with_popularity.groupBy(col("hashtag_id"), col("hashtag_id_2"))
                .agg(collect_list(col("timestamp")).as("time_list"), collect_list(col("actions")).as("weight_list"))
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Map<String, Double> weight_map = new HashMap<String, Double>();
                    Seq<String> times = r.getSeq(2);
                    Seq<Double> weights = r.getSeq(3);
                    for (int i = 0; i < times.size(); i++) {
                        weight_map.put(times.apply(i), weights.apply(i));
                    }
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    while (it.hasNext()) {
                        String cur = it.next();
                        builder.append("," + weight_map.getOrDefault(cur, 0.));
                    }
                    return RowFactory.create(r.getLong(0), r.getLong(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema_ids))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "twitter_weighted_time_series");
        System.out.println("Edge file created");
    }
    
    public static void createKonectGraph(SparkSession ss) {
        FileType fb_type = new FileType("konect");
        Seq<Column> toKeep = JavaConversions.asScalaBuffer(fb_type.getColsToKeep()).seq();
        
        Dataset<Row> data = ss
                .read().options(fb_type.options()).schema(fb_type.schema())
                .csv(Settings.dataFolder + Settings.inputDataset)
                .select(toKeep)
//                .withColumn("date", callUDF("long_to_string", col("timestamp")))
                .withColumn("date", callUDF("from_timestamp_to_date_and_time", col("timestamp")))
                .drop(col("timestamp"))
                // take portion 
                .map((Row r) -> (r.getLong(1) < r.getLong(0)) ? 
                        RowFactory.create(r.getLong(1), r.getLong(0), r.getString(2).substring(0,2)) :
                        RowFactory.create(r.getLong(0), r.getLong(1), r.getString(2).substring(0,2)), RowEncoder.apply(schema_ids));
                // divide the day into morning, afternoon, evening-night
//                .map((Row r) -> {
//                    String date = "";
//                    Integer hour = Integer.parseInt(r.getString(2).substring(11,13));
//                    if (hour >= 6 && hour <= 13) {
//                        date = r.getString(2).substring(0,10) + " 0";
//                    } else if (hour >= 14 && hour <= 21) {
//                        date = r.getString(2).substring(0,10) + " 1";
//                    } else if (hour >= 22 || hour <= 5) {
//                        date = r.getString(2).substring(0,10) + " 2";
//                    }
//                    if (r.getLong(1) < r.getLong(0)) {
//                        return RowFactory.create(r.getLong(1), r.getLong(0), date);
//                    }
//                    return RowFactory.create(r.getLong(0), r.getLong(1), date);
//                }, RowEncoder.apply(schema));
        
        data.show();
        final Seq<String> timestamps = data.select(col("time series"))
                .filter(t -> !t.getString(0).startsWith("0000-00-00"))
                .distinct()
                .orderBy(col("time series"))
                .groupBy()
                .agg(collect_list(col("time series")))
                .first()
                .getSeq(0);
        System.out.println(timestamps.size());
        // Edges with binary time series
        data.groupBy(col("src_id"), col("dst_id"))
                .agg(collect_list(col("time series")).as("time_list"))
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Set<String> time_set = new HashSet<String>(JavaConversions.seqAsJavaList(r.getSeq(2)));
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    if (time_set.size() == 1 && time_set.contains(0)) {
                        while (it.hasNext()) {
                            it.next();
                            builder.append(",1");
                        }
                    } else {
                        while (it.hasNext()) {
                            String cur = it.next();
                            if (time_set.contains(cur)) {
                                builder.append(",1");
                            } else {
                                builder.append(",0");
                            }
                        }
                    }
                    return RowFactory.create(r.getLong(0), r.getLong(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema_ids))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "konect_time_series");
        System.out.println("Edge file created");
    }

    public static void createMobileDataGraph(SparkSession ss) {
        FileType mobile_type = new FileType("mobile");
        Seq<Column> toKeep = JavaConversions.asScalaBuffer(mobile_type.getColsToKeep()).seq();
        
        StructType und_edges = new StructType(new StructField[]{
            new StructField("src_id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("dst_id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("strength", DataTypes.DoubleType, false, Metadata.empty()),
            new StructField("date", DataTypes.StringType, false, Metadata.empty())});
        
        Dataset<Row> data = ss.read().options(mobile_type.options()).schema(mobile_type.schema())
                .csv(Settings.dataFolder + Settings.inputDataset)
                .select(toKeep)
                .withColumn("date", callUDF("from_timestamp_to_date_and_time", col("timestamp")))
                .drop(col("timestamp"))
                // take portion
                 .map((Row r) -> (r.getLong(1) < r.getLong(0)) ? 
                         RowFactory.create(r.getLong(1), r.getLong(0), r.getDouble(2), r.getString(3).substring(0,13)) :
                         RowFactory.create(r.getLong(0), r.getLong(1), r.getDouble(2), r.getString(3).substring(0,13)), RowEncoder.apply(und_edges))
                // divide the day into morning, afternoon, evening-night
//                .map((Row r) -> {
//                    String date = "";
//                    Integer hour = Integer.parseInt(r.getString(3).substring(11,13));
//                    if (hour >= 6 && hour <= 13) {
//                        date = r.getString(3).substring(0,10) + " 0";
//                    } else if (hour >= 14 && hour <= 21) {
//                        date = r.getString(3).substring(0,10) + " 1";
//                    } else if (hour >= 22 || hour <= 5) {
//                        date = r.getString(3).substring(0,10) + " 2";
//                    }
//                    if (r.getLong(1) < r.getLong(0)) {
//                        return RowFactory.create(r.getLong(1), r.getLong(0), r.getDouble(2), date);
//                    }
//                    return RowFactory.create(r.getLong(0), r.getLong(1), r.getDouble(2), date);
//                }, RowEncoder.apply(und_edges));
                .groupBy(col("src_id"), col("dst_id"), col("date"))
                .agg(sum("strength").as("strength"))
                .select(col("src_id"), col("dst_id"), col("strength"), col("date"));
        
        data.show();
        System.out.println(
        data.count() + " " +
        data.filter(col("strength").gt(0.001)).count() + " " +        
        data.filter(col("strength").gt(0.01)).count());
        final Seq<String> timestamps = data.select(col("date"))
                .distinct()
                .orderBy(col("date"))
                .groupBy()
                .agg(collect_list(col("date")))
                .first()
                .getSeq(0);
        // Edges with binary time series
        data.filter(col("strength").gt(0.01))
                .groupBy(col("src_id"), col("dst_id"))
                .agg(collect_list(col("date")).as("time_list"))
                .filter((Row r) -> r.getSeq(2).size() != timestamps.size())
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Set<String> time_set = new HashSet<String>(JavaConversions.seqAsJavaList(r.getSeq(2)));
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    while (it.hasNext()) {
                        String cur = it.next();
                        if (time_set.contains(cur)) {
                            builder.append(",1");
                        } else {
                            builder.append(",0");
                        }
                    }
                    return RowFactory.create(r.getLong(0), r.getLong(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema_ids))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "mobile_time_series");
        // Edges with weighted time series
        data.groupBy(col("src_id"), col("dst_id"))
                .agg(collect_list(col("date")).as("time_list"), collect_list(col("strength")).as("weight_list"))
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Map<String, Double> weight_map = new HashMap<String, Double>();
                    Seq<String> times = r.getSeq(2);
                    Seq<Double> weights = r.getSeq(3);
                    for (int i = 0; i < times.size(); i++) {
                        weight_map.put(times.apply(i), weights.apply(i));
                    }
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    while (it.hasNext()) {
                        String cur = it.next();
                        builder.append(",").append(weight_map.getOrDefault(cur, 0.));
                    }
                    return RowFactory.create(r.getLong(0), r.getLong(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema_ids))
                .coalesce(1).write().mode(SaveMode.Overwrite).csv(Settings.outputFolder + "mobile_weighted_time_series");
        System.out.println("Edge file created");
    }
    
    public static void createPrunedMobileDataGraph(SparkSession ss) {
        FileType mobile_type = new FileType("mobile");
        Seq<Column> toKeep = JavaConversions.asScalaBuffer(mobile_type.getColsToKeep()).seq();
        
        StructType und_edges = new StructType(new StructField[]{
            new StructField("src_id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("dst_id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("strength", DataTypes.DoubleType, false, Metadata.empty()),
            new StructField("date", DataTypes.StringType, false, Metadata.empty())});
        WindowSpec byEndpoints = Window.partitionBy(col("src_id"), col("dst_id"));
        Dataset<Row> data = ss.read().options(mobile_type.options()).schema(mobile_type.schema())
                .csv(Settings.dataFolder + Settings.inputDataset)
                .select(toKeep)
                .withColumn("date", callUDF("from_timestamp_to_date_and_time", col("timestamp")))
                .drop(col("timestamp"))
                // take portion
                .map((Row r) -> (r.getLong(1) < r.getLong(0)) ? 
                         RowFactory.create(r.getLong(1), r.getLong(0), r.getDouble(2), r.getString(3).substring(0,15)) :
                         RowFactory.create(r.getLong(0), r.getLong(1), r.getDouble(2), r.getString(3).substring(0,15)), RowEncoder.apply(und_edges))
                .groupBy(col("src_id"), col("dst_id"), col("date"))
                .agg(sum("strength").as("strength"))
                .select(col("src_id"), col("dst_id"), col("strength"), col("date"), avg(col("strength")).over(byEndpoints).as("average"))
                .filter(col("strength").gt(col("average")));
        
        data.show();
        
        final Seq<String> timestamps = data.select(col("date"))
                .distinct()
                .orderBy(col("date"))
                .groupBy()
                .agg(collect_list(col("date")))
                .first()
                .getSeq(0);
        System.out.println(timestamps.size());
        // Edges with binary time series
        data.groupBy(col("src_id"), col("dst_id"))
                .agg(collect_list(col("date")).as("time_list"))
                .filter((Row r) -> r.getSeq(2).size() != timestamps.size())
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Set<String> time_set = new HashSet<String>(JavaConversions.seqAsJavaList(r.getSeq(2)));
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    while (it.hasNext()) {
                        String cur = it.next();
                        if (time_set.contains(cur)) {
                            builder.append(",1");
                        } else {
                            builder.append(",0");
                        }
                    }
                    return RowFactory.create(r.getLong(0), r.getLong(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema_ids))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "mobile_time_series");
        // Edges with weighted time series
        data.groupBy(col("src_id"), col("dst_id"))
                .agg(collect_list(col("date")).as("time_list"), collect_list(col("strength")).as("weight_list"))
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Map<String, Double> weight_map = new HashMap<String, Double>();
                    Seq<String> times = r.getSeq(2);
                    Seq<Double> weights = r.getSeq(3);
                    for (int i = 0; i < times.size(); i++) {
                        weight_map.put(times.apply(i), weights.apply(i));
                    }
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    while (it.hasNext()) {
                        String cur = it.next();
                        builder.append(",").append(weight_map.getOrDefault(cur, 0.));
                    }
                    return RowFactory.create(r.getLong(0), r.getLong(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema_ids))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "mobile_weighted_time_series");
        System.out.println("Edge file created");
    }

    public static void createTwitterBrunoGraph(SparkSession ss) {
        StructType schema = new StructType(new StructField[]{
            new StructField("tweet_id", DataTypes.LongType, false, Metadata.empty()),
            new StructField("term", DataTypes.StringType, false, Metadata.empty()),
            new StructField("actions", DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("date", DataTypes.StringType, false, Metadata.empty())});
        FileType fileType = new FileType("tweet_hashtags_bruno");
        Seq<Column> toKeep = JavaConversions.asScalaBuffer(fileType.getColsToKeep()).seq();
        //Read data
        Dataset<Row> data = ss.read().options(fileType.options()).schema(fileType.schema())
                .csv(Settings.dataFolder + Settings.inputDataset)
                .select(toKeep)
                .withColumn("date", callUDF("string_full_to_date", col("timestamp")))
                .select(col("tweet_id"), col("text"), col("hashtags"), col("date"), callUDF("sum_actions_int", col("retweets"), col("favorites")).as("actions"))
                .filter(col("hashtags").isNotNull());
        System.out.println("NUM TWEETS: " + data.groupBy(col("tweet_id")).count().count());

        Dataset<Row> hashtagData = data
                .flatMap((Row r) -> {
                    List<Row> rows = new ArrayList<Row>();
                    String content = r.getString(1);
                    // HASHTAGS
                    content = r.getString(2);
                    if (content != null) {
                        if (!content.isEmpty()) {
                            String[] hashtags = content.split(" ");
                            for (String hashtag : hashtags) {
                                hashtag = hashtag.replaceAll("\\W", "").toLowerCase();
                                if (!hashtag.isEmpty()) {
                                    rows.add(RowFactory.create(r.getLong(0), hashtag, r.getInt(4), r.getString(3)));
                                }
                            }
                        }
                    }
                    return rows.iterator();
                }, RowEncoder.apply(schema));
        final Set<String> infrequents = new HashSet<String>(
                JavaConversions.seqAsJavaList(Utils
                        .freq(hashtagData, "tweet_id", "term")
                        .filter(col("freq").leq(Settings.tfidfThreshold))
                        .groupBy()
                        .agg(collect_set(col("term")))
                        .first()
                        .getSeq(0)
                ));
        System.out.println("INFREQUENT HASHTAGS: " + infrequents.size());
        Dataset<Row> frequent_hashtags = hashtagData.filter((Row r) -> (!infrequents.contains(r.getString(1))));
        System.out.println("FREQUENT HASHTAGS: " + frequent_hashtags.groupBy(col("term")).count().count());
        //Nodes 
        frequent_hashtags.select(col("term"))
                .distinct()
                .javaRDD()
                .zipWithIndex()
                .map((Tuple2<Row, Long> t) -> t._2 + " " + t._1.getString(0))
                .coalesce(1).saveAsTextFile(Settings.outputFolder + "nodes");
        Dataset<Row> nodes = ss.read().option("sep", " ").csv(Settings.outputFolder + "nodes");
        String[] columns = nodes.columns();
        nodes = nodes.withColumnRenamed(columns[0], "term_id")
                .withColumnRenamed(columns[1], "term");
        //Create hashtag pairs
        Dataset<Row> frequent_hashtags_with_id = frequent_hashtags.join(nodes, nodes.col("term").equalTo(frequent_hashtags.col("term")));
        Dataset<Row> frequent_hashtags_with_id_2 = frequent_hashtags_with_id.select(col("tweet_id").as("tweet_id_2"), col("term_id").as("term_id_2"));
        Dataset<Row> hashtag_pairs_with_popularity = frequent_hashtags_with_id.join(frequent_hashtags_with_id_2, frequent_hashtags_with_id.col("tweet_id").equalTo(frequent_hashtags_with_id_2.col("tweet_id_2")))
                .filter(frequent_hashtags_with_id.col("term_id").lt(frequent_hashtags_with_id_2.col("term_id_2")))
                .select(frequent_hashtags_with_id.col("tweet_id"), frequent_hashtags_with_id.col("term_id"), frequent_hashtags_with_id_2.col("term_Id_2"), frequent_hashtags_with_id.col("date"), frequent_hashtags_with_id.col("actions"))
                .groupBy(col("term_id"), col("term_id_2"), col("date"))
                .agg(countDistinct(col("tweet_id")).as("numTweets"), sum(col("actions")).as("actions"))
                .select(col("term_id"), col("term_id_2"), callUDF("sum_actions_long", col("numTweets"), col("actions")).as("actions"), col("date"));
        System.out.println("NUM HASHTAG PAIRS: " + hashtag_pairs_with_popularity.groupBy(col("term_id"), col("term_id_2")).count().count());
        // Edges
        final Seq<String> timestamps = hashtag_pairs_with_popularity.select(col("date"))
                .distinct()
                .orderBy(col("date"))
                .groupBy()
                .agg(collect_list(col("date")))
                .first()
                .getSeq(0);
        // Edges with binary time series
        System.out.println("NUM HASHTAG PAIRS (freq >= " + Settings.minF + "): " + hashtag_pairs_with_popularity.groupBy(col("term_id"), col("term_id_2"))
                .agg(countDistinct(col("date")).as("appear"))
                .filter(col("appear").geq(Settings.minF))
                .count());
        hashtag_pairs_with_popularity.groupBy(col("term_id"), col("term_id_2"))
                .agg(collect_list(col("date")).as("time_list"))
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Set<String> time_set = new HashSet<String>(JavaConversions.seqAsJavaList(r.getSeq(2)));
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    while (it.hasNext()) {
                        String cur = it.next();
                        if (time_set.contains(cur)) {
                            builder.append(",1");
                        } else {
                            builder.append(",0");
                        }
                    }
                    return RowFactory.create(r.getString(0), r.getString(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema_names))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "bruno_time_series");
        // Edges with weighted time series
        hashtag_pairs_with_popularity.groupBy(col("term_id"), col("term_id_2"))
                .agg(collect_list(col("date")).as("time_list"), collect_list(col("actions")).as("weight_list"))
                .map((Row r) -> {
                    StringBuilder builder = new StringBuilder();
                    Map<String, Double> weight_map = new HashMap<String, Double>();
                    Seq<String> times = r.getSeq(2);
                    Seq<Double> weights = r.getSeq(3);
                    for (int i = 0; i < times.size(); i++) {
                        weight_map.put(times.apply(i), weights.apply(i));
                    }
                    Iterator<String> it = JavaConversions.seqAsJavaList(timestamps).iterator();
                    while (it.hasNext()) {
                        String cur = it.next();
                        builder.append(",").append(weight_map.getOrDefault(cur, 0.));
                    }
                    return RowFactory.create(r.getString(0), r.getString(1), builder.toString().substring(1));
                }, RowEncoder.apply(schema))
                .coalesce(1).write().mode(SaveMode.Overwrite).csv(Settings.outputFolder + "bruno_weighted_time_series");
        System.out.println("Edge file created");
    }
    
    public static void sampleGraph(SparkSession ss) {
        Dataset<Row> data = ss.read().option("sep", " ").schema(schema_ids)
                .csv(Settings.dataFolder + Settings.inputDataset);
        Dataset<Row> srcDegree = data.groupBy(col("src_id"))
                .agg(count("dst_id").as("srcDegree"))
                .orderBy(col("srcDegree").desc())
                .limit(Settings.numNodes);
        data.join(srcDegree, data.col("src_id").equalTo(srcDegree.col("src_id")))
                .select(data.col("src_id"), col("dst_id"), col("time series"))
                .coalesce(1).write().mode(SaveMode.Overwrite).option("sep", " ").csv(Settings.outputFolder + "edges_pruned");
    }

}
