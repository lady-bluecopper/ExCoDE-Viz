package eu.unitn.disi.db.dence.sparkutils;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.api.java.UDF2;
import org.apache.spark.sql.api.java.UDF3;

/**
 *
 * @author bluecopper
 */
public class UDFs {
    
    private static List<String> MONTHS = new ArrayList<String>(Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"));

    public static UDF1<Object, String> null_to_none = (el) -> {
        if (el == null) {
            return "None";
        }
        if (el.toString().length() == 0) {
            return "None";
        }
        return el.toString();
    };

    public static UDF1<String, Boolean> string_to_boolean = (el) -> {
        if (el != null) {
            if (el.length() > 0) {
                if (el.equalsIgnoreCase("YES")) {
                    return true;
                } else if (el.equalsIgnoreCase("NO")) {
                    return false;
                }
            }
        }
        return false;
    };

    public static UDF1<String, String> string_to_date = (el) -> {
        if (el != null) {
            try {
                if (!el.equalsIgnoreCase("None")) {
                    String[] els = el.split(" ");
                    return els[3] + "-" + getMonth(els[2]) + "-" + els[1];
                }
            } catch (Exception e) {
                System.out.println("WARNING: " + el);
            }
        }
        return "0000-00-00";
    };
    
    public static UDF1<Long, String> long_to_string = (el) -> {
        if (el != null) {
            return el.toString();
        }
        return "0000-00-00";
    };
    
    public static UDF1<String, String> unix_timestamp_to_day = (el) -> {
        if (el != null) {
            if (!el.equalsIgnoreCase("None")) {
                String[] ts = el.split(" ");
                return ts[0];
            }
        }
        return "0000-00-00";
    };
    
    public static UDF1<String, String> timestamp_to_min = (el) -> {
        if (el != null) {
            return el.substring(0, 16);
        }
        return "0";
    };           
    
    public static UDF1<String, String> string_full_to_date = (s) -> {
        if (s != null) {
            if (!s.equalsIgnoreCase("None")) {
                
                String[] els = s.trim().split(" ");
                String month = getMonth(els[1]);
                return els[els.length - 1] + "-" + month + "-" + els[2];
            }
        }
        return "0000-00-00";
    };
    
    public static UDF1<Long, String> from_timestamp_to_date = (epoch) -> {
        if (epoch > 0) {
            // Changed here
            Date date = getDateFromTimestamp(epoch);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return df.format(date);
        }
        return "0000-00-00";
    };
    
    public static UDF1<Long, String> from_timestamp_to_date_and_time = (epoch) -> {
        if (epoch > 0) {
            // Changed here
            Date date = getDateFromTimestamp(epoch);
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return df.format(date);
        }
        return "0000-00-00 00:00:00";
    };
    
    public static UDF3<Integer, Integer, Integer, Integer> sum_actions = (a1, a2, a3) -> {
        return a1 + a2 + a3;
    };
    
    public static UDF2<Long, Long, Long> sum_actions_long = (a1, a2) -> {
        return a1 + a2;
    };
    
    public static UDF2<Integer, Integer, Integer> sum_actions_int = (a1, a2) -> {
        return a1 + a2;
    };

    private static String getMonth(String s) {
        int index = MONTHS.indexOf(s) + 1;
        if (index > 0 && index < 10) {
            return "0" + index;
        } else if (index > 9) {
            return "" + index;
        }
        return "00";
    }
    
    private static Date getDateFromTimestamp(long t) {
        long toCheck = 10000000000000L;
        long value = t < toCheck ? t : t * 1000;
        return new Date(new Timestamp(value).getTime());
    }

}
