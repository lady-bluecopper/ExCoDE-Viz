package eu.unitn.disi.db.dence.utils;

/**
 *
 * @author bluecopper
 */
public class Settings {
    // Folders and Input Files
    public static String dataFolder = "/Users/bluecopper/Documents/PhD/CorrelatedEvents/2018-correlated-events-in-temporal-networks-code/Datasets/BGP/";
    public static String outputFolder = "/Users/bluecopper/Desktop/";
    public static String edgeFile = "bgp.csv";
    public static String cliqueFile;
    // Graph Properties
    public static boolean isWeighted = false;
    public static boolean isLabeled = false;
    // Correlation
    public static double minCor = 0.15;
    // Approximate
    public static boolean isExact = true;
    public static int numHashRuns = 2;
    public static int numHashFuncs = 5;
    /*If true, cliques are loaded from file*/
    public static boolean test = false;
    // Density
    public static double minDen = 1.82;
    public static int minEdgesInSnap = 90;
    /*If true, use the MA density function; use AA otherwise*/
    public static boolean isMA = false;
    public static int maxCCSize = 5000;
    public static boolean singleSub = false;
    // Diverse Subgraphs
    public static boolean diverseOn = false;
    public static double maxJac;
    // Stats
    public static String exactFile = "DenCE_twitter";
    public static String axFile = "DenCE_twitter";
    // Dataset creation
    public static String inputDataset = "contact/out.contact";
    public static String fileType = "konect";
    public static String inputDataset2 = "tweet_hashtag_01.csv";
    public static String fileType2 = "tweet_hashtags";
    public static String inputDataset3 = "tweet_text_01.csv";
    public static String fileType3 = "tweet_texts";
    
    public static Double tfidfThreshold = 0.8;
    public static int minF = 3;
    public static int numNodes;
    
    public static boolean local = true;
    
    public static int statNo = 0;
}
