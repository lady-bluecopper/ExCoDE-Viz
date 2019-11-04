package eu.unitn.disi.db.dence.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.spark.sql.Column;
import static org.apache.spark.sql.functions.col;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

/**
 *
 * @author bluecopper
 */
public class FileType {
    
    public enum FileTypeEnum {
        pinterest,
        tweets,
        tweet_hashtags,
        tweet_texts,
        twitter_followers,
        tweet_hashtags_bruno,
        mobile,
        wc,
        konect,
        taxi,
        stopwords;
    }
    
    public final FileTypeEnum fileType;
    private final Map<String, String> options;
    private final StructType schema;
    private final List<String> colsToKeep;
    
    public FileType(String type) {
        this.fileType = FileTypeEnum.valueOf(type);
        this.schema = createStruct();
        this.options = createOptions();
        this.colsToKeep = setColsToKeep();
    }
    
    private StructType createStruct() {
        StructType schema = null;
        switch (this.fileType) {
            case pinterest:
                schema = new StructType(new StructField[]{
                    new StructField("pin_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("pinner_id", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("description", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("board_id", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("board_name", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("source", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("likes_count", DataTypes.IntegerType, false, Metadata.empty()),
                    new StructField("repins_count", DataTypes.IntegerType, false, Metadata.empty()),
                    new StructField("comments_count", DataTypes.IntegerType, false, Metadata.empty()),
                    new StructField("actions_count", DataTypes.IntegerType, false, Metadata.empty()),
                    new StructField("pinned_from", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("repinned_via", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("originally_pinned_by", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("price", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("no_pins_on_board", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("explored", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("pinned_via", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("isrepin", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("pin_time", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("image_link", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("time_ago", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("originally_pinned_on", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("category", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("no_pins_board", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("no_followers_board", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("pinner_following", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("pinner_followers", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("pinner_pins", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("pinner_boards", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("pinner_about", DataTypes.StringType, true, Metadata.empty())});
                break;
            case tweets:
                schema = new StructType(new StructField[]{
                    new StructField("tweet_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("user_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("in_reply_to_status_id", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("in_reply_to_user_id", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("favorited", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("retweeted", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("retweet_count", DataTypes.IntegerType, true, Metadata.empty()),
                    new StructField("lang", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("created_at", DataTypes.StringType, true, Metadata.empty())});
                break;
            case tweet_hashtags:
                schema = new StructType(new StructField[]{
                    new StructField("tweet_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("user_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("hashtag_id", DataTypes.LongType, false, Metadata.empty())});
                break;
            case tweet_texts:
                schema = new StructType(new StructField[]{
                    new StructField("tweet_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("user_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("text", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("geo_lat", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("geo_long", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("place_full_name", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("place_id", DataTypes.LongType, true, Metadata.empty())});
                break;
            case twitter_followers:
                schema = new StructType(new StructField[]{
                    new StructField("user_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("follower_id", DataTypes.LongType, false, Metadata.empty())});
                break;
            case tweet_hashtags_bruno:
                schema = new StructType(new StructField[]{
                    new StructField("user_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("tweet_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("text", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("timestamp", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("source", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("retweets", DataTypes.IntegerType, false, Metadata.empty()),
                    new StructField("is_retweet", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("favorites", DataTypes.IntegerType, false, Metadata.empty()),
                    new StructField("hashtags", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("urls", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("mentions", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("is_geo", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("lat", DataTypes.StringType, true, Metadata.empty()),
                    new StructField("long", DataTypes.StringType, true, Metadata.empty())});
                break;
            case mobile:
                schema = new StructType(new StructField[]{
                    new StructField("timestamp", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("src_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("dst_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("strength", DataTypes.DoubleType, false, Metadata.empty())});
                break;
            case wc:
                schema = new StructType(new StructField[]{
                    new StructField("user", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("object", DataTypes.StringType, false, Metadata.empty()),
                    new StructField("timestamp", DataTypes.StringType, false, Metadata.empty())});
                break;
            case konect:
                schema = new StructType(new StructField[]{
                    new StructField("src_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("dst_id", DataTypes.LongType, false, Metadata.empty()),
                    new StructField("multiplicity", DataTypes.IntegerType, false, Metadata.empty()),
                    new StructField("timestamp", DataTypes.LongType, false, Metadata.empty())});
                break;
            case taxi:
                schema = new StructType(new StructField[]{
                    
                });
                break;
            case stopwords:
                schema = new StructType(new StructField[]{
                    new StructField("word", DataTypes.StringType, false, Metadata.empty())});
                break;
        }
        return schema;
    }
    
    private Map<String, String> createOptions() {
        Map<String, String> opts = new HashMap<String, String>();
        switch (this.fileType) {
            case pinterest:
                opts.put("header", "true");
                opts.put("inferSchema", "false");
                opts.put("mode", "DROPMALFORMED");
                opts.put("delimiter", ",");
                break;
            case tweets:
            case tweet_hashtags:
            case tweet_texts:
                opts.put("header", "false");
                opts.put("inferSchema", "false");
                opts.put("mode", "DROPMALFORMED");
                opts.put("delimiter", ",");
                break;
            case mobile:
            case twitter_followers:
            case tweet_hashtags_bruno:
            case stopwords:
                opts.put("header", "false");
                opts.put("inferSchema", "false");
                opts.put("mode", "DROPMALFORMED");
                opts.put("delimiter", "\t");
                break;
            case konect:
                opts.put("header", "false");
                opts.put("inferSchema", "false");
                opts.put("mode", "DROPMALFORMED");
                opts.put("delimiter", "\t");
                break;
            case wc:
                opts.put("header", "false");
                opts.put("inferSchema", "false");
                opts.put("mode", "DROPMALFORMED");
                opts.put("delimiter", " ");
                break;
        }
        return opts;
    }
    
    public Map<String, String> options() {
        return this.options;
    }

    public StructType schema() {
        return this.schema;
    }

    public List<Column> getColsToKeep() {
        List<Column> cols = new ArrayList<Column>();
        colsToKeep.stream().forEach((col) -> {
            cols.add(col(col));
        });
        return cols;
    }
    
    private List<String> setColsToKeep() {
        List<String> cols = null;
        switch (this.fileType) {
            case pinterest:
                cols = new ArrayList<String>();
                cols.add("pin_id");
                cols.add("pinner_id");
                cols.add("likes_count");
                cols.add("repins_count");
                cols.add("comments_count");
                cols.add("pinned_from");
                cols.add("repinned_via");
                cols.add("isrepin");
                cols.add("pin_time");
                cols.add("originally_pinned_by");
                cols.add("category");
                break;
            case tweets:
                cols = new ArrayList<String>();
                cols.add("tweet_id");
                cols.add("user_id");
                cols.add("favorited");
                cols.add("retweeted");
                cols.add("retweet_count");
                cols.add("created_at");
                break;
            case tweet_texts:
                cols = new ArrayList<String>();
                cols.add("tweet_id");
                cols.add("user_id");
                cols.add("text");
                break;
            case tweet_hashtags_bruno:
                cols = new ArrayList<String>();
                cols.add("user_id");
                cols.add("tweet_id");
                cols.add("text");
                cols.add("timestamp");
                cols.add("retweets");
                cols.add("is_retweet");
                cols.add("favorites");
                cols.add("hashtags");
                break;
            case konect:
                cols = new ArrayList<String>();
                cols.add("src_id");
                cols.add("dst_id");
                cols.add("timestamp");
                break;
            case tweet_hashtags:
            case twitter_followers:
            case mobile:
            case stopwords:
            case wc:
            default:
                if (this.schema != null) {
                    cols = new ArrayList<String>();
                    for (StructField field : this.schema.fields()) {
                        cols.add(field.name());
                    }
                }
                break;
        }
        return cols;
    }
        
}