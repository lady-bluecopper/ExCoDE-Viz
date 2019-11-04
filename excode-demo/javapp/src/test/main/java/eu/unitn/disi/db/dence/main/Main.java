package eu.unitn.disi.db.dence.main;

import com.koloboke.collect.IntCursor;
import com.koloboke.collect.map.hash.HashIntDoubleMap;
import com.koloboke.collect.map.hash.HashIntDoubleMaps;
import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashObjIntMap;
import com.koloboke.collect.map.hash.HashObjIntMaps;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import eu.unitn.disi.db.dence.bucketization.MinHashBucketization;
import eu.unitn.disi.db.dence.densesub.DenseSubgraphsFinder;
import eu.unitn.disi.db.dence.graph.BinaryDynamicEdge;
import eu.unitn.disi.db.dence.graph.DynamicGraph;
import eu.unitn.disi.db.dence.graph.BinaryDynamicGraph;
import eu.unitn.disi.db.dence.graph.DynamicEdge;
import eu.unitn.disi.db.dence.graph.WeightedDynamicEdge;
import eu.unitn.disi.db.dence.graph.WeightedDynamicGraph;
import eu.unitn.disi.db.dence.utils.CommandLineParser;
import eu.unitn.disi.db.dence.graph.GPCorrelationGraph;
import eu.unitn.disi.db.dence.maxclique.GPCliqueFinder;
import eu.unitn.disi.db.dence.utils.Pair;
import eu.unitn.disi.db.dence.utils.Settings;
import eu.unitn.disi.db.dence.utils.StopWatch;
import eu.unitn.disi.db.dence.utils.Utilities;
import eu.unitn.disi.db.dence.webserver.utils.Configuration;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;
import jersey.repackaged.com.google.common.collect.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author bluecopper
 */
public class Main {

    static StopWatch watch;

    public static void main(String[] args) throws Exception {
        //parse the command line arguments
        CommandLineParser.parse(args);
        
        watch = new StopWatch();
        StopWatch watch2 = new StopWatch();
        //load dynamic graph
        System.out.println(new Date(System.currentTimeMillis()).toGMTString());
        System.out.println("Loading graph.");
        watch.start();
        watch2.start();
        DynamicGraph graph;
        if (Settings.edgeFile.startsWith("wc")) {
            graph = loadWCGraph(Settings.dataFolder + Settings.edgeFile, Settings.minEdgesInSnap);
        } else if (Settings.edgeFile.startsWith("bgp")) {
            graph = loadBGPGraph(Settings.dataFolder + Settings.edgeFile, Settings.dataFolder + Settings.edgeFile + "_mapping", Settings.minEdgesInSnap);
        } else {
            graph = loadEdges(Settings.dataFolder + Settings.edgeFile, Settings.minEdgesInSnap);
        }
        System.out.println(".......................Graph loaded in (ms) " + watch2.getElapsedTime());
        List<HashIntSet> gpcliques;
        List<HashIntSet> result;
        if (Settings.test) {
            gpcliques = loadCliquesFromFile(Settings.dataFolder + Settings.cliqueFile);
        } else {
            Collection<Pair<Integer, Integer>> correlatedPairs;
            System.out.println(new Date(System.currentTimeMillis()).toGMTString());
            System.out.println("Searching correlated pairs.");
            watch2.start();
            if (Settings.isExact) {
                correlatedPairs = graph.findCorrelatedPairs(Settings.minCor);
            } else {
                MinHashBucketization buck = new MinHashBucketization(Settings.numHashFuncs, Settings.numHashRuns, graph.getNumSnaps());
                Collection<Pair<Integer, Integer>> candPairs = buck.getCandidatePairs(graph.getEdges());
                correlatedPairs = graph.findCorrelatedPairs(candPairs, Settings.minCor);
            }
            System.out.println(".......................Found " + correlatedPairs.size() + " Correlated Pairs in (s) " + watch2.getElapsedTimeInSec());
            //find maximal cliques
            GPCorrelationGraph gpgraph = new GPCorrelationGraph(graph.getNumEdges(), correlatedPairs);
            GPCliqueFinder gpfinder = new GPCliqueFinder(gpgraph);
            System.out.println(new Date(System.currentTimeMillis()).toGMTString());
            System.out.println("Searching cliques.");
            watch2.start();
            gpcliques = gpfinder.findMaxCliquesWithCCs();
            System.out.println(".......................Found " + gpcliques.size() + " Cliques in (s) " + watch2.getElapsedTimeInSec());
            watch2.start();
            writeCliques(gpcliques);
        }
        //find dense subgraphs
        DenseSubgraphsFinder DSFinder = new DenseSubgraphsFinder(graph);
        System.out.println(new Date(System.currentTimeMillis()).toGMTString());
        if (Settings.diverseOn) {
            System.out.println("Finding diverse subgraphs.");
            result = DSFinder.findDiverseDenseSubgraphs(gpcliques);
        } else {
            System.out.println("Finding dense subgraphs.");
            result = DSFinder.findAllDenseSubgraphs(gpcliques);
        }
        watch.stop();
        System.out.println("TOTAL TIME (min): " + watch.getElapsedTimeInMin());
        //store results
        writeResults(graph, result);
    }
    
    public static HashIntObjMap<String> loadMapping(String path) throws IOException {
        final BufferedReader rows = new BufferedReader(new FileReader(path));
        HashIntObjMap<String> mapping = HashIntObjMaps.newMutableMap();
        String line;
        while((line = rows.readLine()) != null) {
            String[] lst = line.split("\t");
            mapping.put(Integer.parseInt(lst[0]), lst[1]);
        }
        return mapping;
    }

    public static DynamicGraph loadEdges(String edgePath, int numEdgesPerSnap) throws IOException {
        final BufferedReader rows = new BufferedReader(new FileReader(edgePath));
        Map<Integer, Integer> srcNodes = new HashMap<Integer, Integer>();
        Map<Integer, Integer> dstNodes = new HashMap<Integer, Integer>();
        List<DynamicEdge> edges = Lists.newArrayList();
        String line;
        int node_counter = 0;
        int edge_counter = 0;
        int numSnaps = 0;

        while ((line = rows.readLine()) != null) {
            String[] parts = line.split(" ");
            int srcId = Integer.parseInt(parts[0]);
            int dstId = Integer.parseInt(parts[1]);
            if (srcNodes.putIfAbsent(srcId, node_counter) == null) {
                node_counter ++;
            }
            if (dstNodes.putIfAbsent(dstId, node_counter) == null) {
                node_counter ++;
            }
            int label;
            String[] series;
            if (Settings.isLabeled) {
                label = Integer.parseInt(parts[2]);
                series = parts[3].split(",");
            } else {
                label = 1;
                series = parts[2].split(",");
            }
            DynamicEdge edge;
            if (Settings.isWeighted) {
                edge = new WeightedDynamicEdge(edge_counter, srcNodes.get(srcId), dstNodes.get(dstId), label, createWeightedSeries(series));
            } else {
                edge = new BinaryDynamicEdge(edge_counter, srcNodes.get(srcId), dstNodes.get(dstId), label, createBinarySeries(series));
            }
            numSnaps = series.length;
            edges.add(edge);
            edge_counter++;
        }
        rows.close();
        DynamicGraph g;
        if (Settings.isWeighted) {
            g = new WeightedDynamicGraph(srcNodes.size() + dstNodes.size(), edges.size(), numSnaps, numEdgesPerSnap);
        } else {
            g = new BinaryDynamicGraph(srcNodes.size() + dstNodes.size(), edges.size(), numSnaps, numEdgesPerSnap);
        }
        List<Entry<Integer, Integer>> node_list = Lists.newArrayList(srcNodes.entrySet());
        node_list.addAll(dstNodes.entrySet());
        Collections.sort(node_list, (Entry<Integer, Integer> e1, Entry<Integer, Integer> e2) 
                -> Integer.compare(e1.getValue(), e2.getValue()));
        node_list.stream().forEach(e -> g.addNode(e.getValue(), e.getKey()));
        edges.stream().forEachOrdered(edge -> g.addEdge(edge));
        System.out.println("Nodes: " + g.getNumNodes() + " Edges: " + g.getNumEdges() + " Snaps: " + numSnaps);
        return g;
    }
    
    public static DynamicGraph loadWCGraph(String edgePath, int numEdgesPerSnap) throws IOException {
        Set<String> labels = new HashSet<String>(Arrays.asList(
                new String[]{"images", "competition", "playing", "history", "venues",
                "news", "tickets", "teams", "enfetes", "help", "hosts", "individuals",
                "js", "member", "souvenirs", "welcome"}));
        final BufferedReader rows = new BufferedReader(new FileReader(edgePath));
        HashObjIntMap<String> srcNodes = HashObjIntMaps.newMutableMap();
        HashObjIntMap<String> dstNodes = HashObjIntMaps.newMutableMap();
        List<DynamicEdge> edges = Lists.newArrayList();
        String line = rows.readLine();
        int node_counter = 0;
        int edge_counter = 0;
        HashIntSet snapshots = HashIntSets.newMutableSet();

        while (line != null) {
            String[] parts = line.split(" ");
            String srcId = parts[0];
            if (srcId.equals("0")) {
                if (!dstNodes.containsKey(parts[1])) {
                    dstNodes.put(parts[1], node_counter);
                    node_counter ++;
                }
                String edgeLabel;
                if (parts[1].length() == 1) {
                    edgeLabel = "home";
                } else {
                    String[] dst = parts[1].split("/");
                    edgeLabel = (labels.contains(dst[1])) ? dst[1] : "home";
                }
                if (!srcNodes.containsKey(srcId)) {
                    srcNodes.put(srcId, node_counter);
                    node_counter ++;
                }
                String[] series = parts[2].split(",");
                DynamicEdge edge;
                if (Settings.isWeighted) {
                    edge = new WeightedDynamicEdge(edge_counter, srcNodes.get(srcId), dstNodes.get(parts[1]), edgeLabel, createWeightedSeries(series, snapshots));
                } else {
                    edge = new BinaryDynamicEdge(edge_counter, srcNodes.get(srcId), dstNodes.get(parts[1]), edgeLabel, createUnWeightedSeries(series, snapshots));
                }
                edges.add(edge);
                edge_counter++;
            }
            line = rows.readLine();
        }
        rows.close();

        DynamicGraph g = new WeightedDynamicGraph(srcNodes.size() + dstNodes.size(), edges.size(), snapshots.size(), numEdgesPerSnap);
        List<Entry<String, Integer>> node_list = Lists.newArrayList(srcNodes.entrySet());
        node_list.addAll(dstNodes.entrySet());
        Collections.sort(node_list, (Entry<String, Integer> e1, Entry<String, Integer> e2) 
                -> Integer.compare(e1.getValue(), e2.getValue()));
        node_list.stream().forEach(e -> g.addNode(e.getValue(), e.getKey()));
        edges.stream().forEachOrdered(edge -> g.addEdge(edge));
        System.out.println("Nodes: " + g.getNumNodes() + " Edges: " + g.getNumEdges());
        return g;
    }
    
    public static DynamicGraph loadBGPGraph(String edgePath, String mappingFile, int numEdgesPerSnap) throws IOException {
        HashIntObjMap<String> labels = HashIntObjMaps.newMutableMap();
        try (Stream<String> stream = Files.lines(Paths.get(mappingFile), Charset.forName("ISO-8859-1"))) {
            stream.forEach(line -> {
                String[] lst = line.trim().split("\t");
                StringBuilder builder = new StringBuilder();
                for (int i = 1; i < lst.length; i ++) {
                    builder.append(lst[i]).append("\t");
                }
                labels.put(Integer.parseInt(lst[0]), builder.toString().trim());
            });
        }
        HashIntIntMap nodes = HashIntIntMaps.newMutableMap();
        List<DynamicEdge> edges = Lists.newArrayList();
        int node_counter = 0;
        int edge_counter = 0;
        int numSnapshot = 0;
        String line;
        try (BufferedReader rows = new BufferedReader(new FileReader(edgePath))) {
            while ((line = rows.readLine()) != null) {
                String[] parts = line.split(" ");
                int srcName = Integer.parseInt(parts[0]);
                int dstName = Integer.parseInt(parts[1]);
                if (!nodes.containsKey(srcName)) {
                    nodes.put(srcName, node_counter);
                    node_counter++;
                }
                if (!nodes.containsKey(dstName)) {
                    nodes.put(dstName, node_counter);
                    node_counter++;
                }
                String edgeLabel = "";
                if (labels.getOrDefault(srcName, "nolabel").substring(0, 3).equals(labels.getOrDefault(dstName, "nolabel").substring(0, 3))) {
                    edgeLabel = "intra";
                } else {
                    edgeLabel = "inter";
                }
                String[] series = parts[2].split(",");
                numSnapshot = series.length;
                DynamicEdge edge = new BinaryDynamicEdge(edge_counter, nodes.get(srcName), nodes.get(dstName), edgeLabel, createBinarySeries(series));
                edges.add(edge);
                edge_counter++;
            }
        }
        DynamicGraph g = new WeightedDynamicGraph(nodes.size(), edges.size(), numSnapshot, numEdgesPerSnap);
        List<Entry<Integer, Integer>> node_list = Lists.newArrayList(nodes.entrySet());
        Collections.sort(node_list, (Entry<Integer, Integer> e1, Entry<Integer, Integer> e2) 
                -> Integer.compare(e1.getValue(), e2.getValue()));
        node_list.stream().forEach(e -> g.addNode(e.getValue(), labels.get(e.getKey())));
        edges.stream().forEachOrdered(edge -> g.addEdge(edge));
        System.out.println("Nodes: " + g.getNumNodes() + " Edges: " + g.getNumEdges());
        return g;
    }
    
    private static HashIntSet createBinarySeries(String[] elems, HashIntSet snapshots) {
        HashIntSet series = HashIntSets.newMutableSet();
        for (String e : elems) {
            int snap = Integer.parseInt(e);
            series.add(snap);
            snapshots.add(snap);
        }
        return series;
    }
    
    private static HashIntSet createBinarySeries(String[] elems) {
        HashIntSet series = HashIntSets.newMutableSet();
        for (int i = 0; i < elems.length; i++) {
            if (!elems[i].equals("0")) {
                series.add(i);
            }
        }
        return series;
    }
    
    private static HashIntDoubleMap createWeightedSeries(String[] elems, HashIntSet snapshots) {
        HashIntDoubleMap series = HashIntDoubleMaps.newMutableMap();
        for (String e : elems) {
            String[] p = e.split(":");
            int snap = Integer.parseInt(p[0]);
            series.put(snap, Double.parseDouble(p[1]));
            snapshots.add(snap);
        }
        return series;
    }
    
    private static HashIntDoubleMap createWeightedSeries(String[] elems) {
        HashIntDoubleMap series = HashIntDoubleMaps.newMutableMap();
        for (int i = 0; i < elems.length; i++) {
            series.put(i, Double.parseDouble(elems[i]));
        }
        return series;
    }
    
    private static HashIntSet createUnWeightedSeries(String[] elems, HashIntSet snapshots) {
        HashIntSet series = HashIntSets.newMutableSet();
        for (String e : elems) {
            String[] p = e.split(":");
            int snap = Integer.parseInt(p[0]);
            series.add(snap);
            snapshots.add(snap);
        }
        return series;
    }
    
    private static List<HashIntSet> loadCliquesFromFile(String fileName) throws IOException {
        List<HashIntSet> ccs = Lists.newArrayList();
        final BufferedReader rows = new BufferedReader(new FileReader(fileName));
        String line = rows.readLine();
        while (line != null) {
            String[] elements = line.split(" ");
            HashIntSet set = HashIntSets.newMutableSet();
            for (String s : elements) {
                set.add(Integer.parseInt(s));
            }
            ccs.add(set);
            line = rows.readLine();
        }
        rows.close();
        System.out.println(ccs.size() + " cliques loaded.");
        return ccs;
    }
    
    public static void writeGraph(List<DynamicEdge> original, List<Pair<Integer, Integer>> edges) {
        try {
            FileWriter fw = new FileWriter(Settings.outputFolder + "correlationGraph.csv");
            FileWriter fw2 = new FileWriter(Settings.outputFolder + "mapping.csv");
            for (Pair<Integer, Integer> edge : edges) {
                fw.write(edge.getA() + " " + edge.getB() + "\n");
            }
            fw.close();
            for (DynamicEdge or : original) {
                fw2.write(or.getSrc() + " " + or.getDst() + " " + or.getEdgeID() + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }  
    }
    
    public static void writeCliques(List<HashIntSet> cliques) {
        int count = 0;
        try {
            FileWriter fw = new FileWriter(Settings.dataFolder + "cliques_" + Settings.edgeFile);
            for (HashIntSet clique : cliques) {
                StringBuilder builder = new StringBuilder();
                IntCursor cur = clique.cursor();
                while (cur.moveNext()) {
                    builder.append(" ").append(cur.elem());
                }
                if (builder.length() > 0) {
                    fw.write(builder.toString().substring(1) + "\n");
                    count++;
                }
            }
            fw.close();
            System.out.println(count + " cliques written.");
        } catch (Exception e) {
            e.printStackTrace();
        }  
    }

    public static void writeResults(DynamicGraph graph, Collection<HashIntSet> densCorEdges) throws IOException {
        try {
            FileWriter fw = new FileWriter(Settings.outputFolder + "statistics.csv", true);
            fw.write(String.format("%s\t%s\t%f\t%d\t%s\t%f\t%f\t%d\t%d\t%s\t%s\n",
                    Settings.edgeFile,
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    watch.getElapsedTime() / 1000.0D,
                    densCorEdges.size(),
                    Settings.isMA,
                    Settings.minCor,
                    Settings.minDen,
                    Settings.maxCCSize,
                    Settings.minEdgesInSnap,
                    (!Settings.isExact) ? Settings.numHashFuncs + "+" + Settings.numHashRuns : "",
                    (Settings.diverseOn) ? Settings.maxJac : ""));
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String fName = "DenCE_" + Settings.edgeFile + 
                    "_C" + Settings.minCor + 
                    "D" + Settings.minDen + 
                    ((Settings.isMA) ? "M" : "A") + 
                    "K" + Settings.minEdgesInSnap + 
                    "S" + Settings.maxCCSize +
                    ((!Settings.isExact) ? "AX" + Settings.numHashFuncs + "+" + Settings.numHashRuns : "") +
                    ((Settings.diverseOn) ? "Div" + Settings.maxJac : "");
            FileWriter fwP = new FileWriter(Settings.outputFolder + fName);
            for (Set<Integer> denseSub : densCorEdges) {
                StringBuilder builder = new StringBuilder();
                // Write src and dst
//                denseSub.stream().forEach(edge -> {
//                    builder.append(graph.getSrc(edge))
//                            .append("(").append(graph.getSrcLabel(edge)).append("),")
//                            .append(graph.getDst(edge))
//                            .append("(").append(graph.getDstLabel(edge)).append(")\t");
//                });
                // Write edge ids
                denseSub.stream().forEach(edge -> builder.append(edge).append("\t"));
                fwP.write(builder.toString() + "\n");
            }
            fwP.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static String runTask(Configuration conf, boolean withGraph) {
        System.out.println("Starting Task...");
        try {
            DynamicGraph graph;
            if (conf.Dataset.Path.contains("wc")) {
                graph = loadWCGraph(conf.Dataset.Path, conf.Task.EdgesPerSnapshot);
            } else {
                graph = loadEdges(conf.Dataset.Path, conf.Task.EdgesPerSnapshot);
            }
            System.out.println("Searching correlated pairs.");
            Collection<Pair<Integer, Integer>> correlatedPairs = graph.findCorrelatedPairs(conf.Task.Correlation);
            GPCorrelationGraph gpgraph = new GPCorrelationGraph(graph.getNumEdges(), correlatedPairs);
            System.out.println("Searching cliques.");
            GPCliqueFinder gpfinder = new GPCliqueFinder(gpgraph);
            List<HashIntSet> gpcliques = gpfinder.findMaxCliquesWithCCs();
            System.out.println("Finding dense subgraphs.");
            DenseSubgraphsFinder DSFinder = new DenseSubgraphsFinder(graph, conf);
            List<Pair<HashIntSet, Double>> result = DSFinder.findDiverseDenseSubgraphsWithScore(gpcliques);
            System.out.println("Done");
            if (withGraph) {
                return graph2JSON(graph, result);
            }
            return subs2JSON(graph, result);
        } catch (IOException | JSONException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }
    
    public static String runExplorationTask(Configuration conf) {
        System.out.println("Starting Exploration Task...");
        try {
            DynamicGraph graph = loadEdges(conf.Dataset.Path, conf.Task.EdgesPerSnapshot);
            HashIntObjMap<Pair<HashIntSet, Double>> views = graph.generateExplodeViewWithDensity(conf.Subgraph, conf.Task.EdgesPerSnapshot);
            System.out.println("Done");
            try {
                HashIntObjMap<String> mapping = loadMapping(conf.Dataset.Path + "_mapping");
                HashIntObjMap<String> snapMapping = loadMapping(conf.Dataset.Path + "_snapMapping");
                return labeled_views2JSON(graph, views, mapping, snapMapping);
            } catch (IOException e) {
                System.out.println("[WARN] Mapping not found!");
                return views2JSON(graph, views);
            }
        } catch (IOException | JSONException e) {
            System.out.println(e.getMessage());
            return "";
        }
    }
    
    public static String subs2JSON(DynamicGraph graph, List<Pair<HashIntSet, Double>> result) throws JSONException {
        if (result.isEmpty()) {
            return "";
        }
        Collections.sort(result, (Pair<HashIntSet, Double> p1, Pair<HashIntSet, Double> p2) -> - Double.compare(p1.getB(), p2.getB()));
        HashIntObjMap<Pair<Integer, HashIntSet>> nodes = HashIntObjMaps.newMutableMap();
        HashObjObjMap<Pair<Integer, Integer>, HashIntSet> edges = HashObjObjMaps.newMutableMap();
        HashIntDoubleMap densities = HashIntDoubleMaps.newMutableMap();
        DecimalFormat df = new DecimalFormat("#.###");
        int nodeCount = 0;
        double previousDensity = Double.parseDouble(df.format(result.get(0).getB()));
        int index = 1;
        densities.put(index, previousDensity);
        for (int s = 0; s < result.size(); s++) {
            for (int edge : result.get(s).getA()) {
                int src = graph.getSrc(edge);
                int dst = graph.getDst(edge);
                int srcId;
                int dstId;
                double thisDensity = Double.parseDouble(df.format(result.get(s).getB()));
                if (thisDensity < previousDensity && index < 6) {
                    index ++;
                    previousDensity = thisDensity;
                    densities.put(index, previousDensity);
                }
                if (nodes.containsKey(src)) {
                    Pair<Integer, HashIntSet> srcP = nodes.get(src);
                    srcId = srcP.getA();
                    HashIntSet srcA = srcP.getB();
                    srcA.add(Integer.parseInt(String.format("%d%d", index, s)));
                    nodes.put(src, new Pair<Integer, HashIntSet>(srcId, srcA));
                } else {
                    srcId = nodeCount;
                    HashIntSet srcA = HashIntSets.newMutableSet();
                    srcA.add(Integer.parseInt(String.format("%d%d", index, s)));
                    nodes.put(src, new Pair<Integer, HashIntSet>(nodeCount, srcA));
                    nodeCount++;
                }
                if (nodes.containsKey(dst)) {
                    Pair<Integer, HashIntSet> dstP = nodes.get(dst);
                    dstId = dstP.getA();
                    HashIntSet dstA = dstP.getB();
                    dstA.add(Integer.parseInt(String.format("%d%d", index, s)));
                    nodes.put(dst, new Pair<Integer, HashIntSet>(dstId, dstA));
                } else {
                    dstId = nodeCount;
                    HashIntSet dstA = HashIntSets.newMutableSet();
                    dstA.add(Integer.parseInt(String.format("%d%d", index, s)));
                    nodes.put(dst, new Pair<Integer, HashIntSet>(nodeCount, dstA));
                    nodeCount++;
                }
                Pair<Integer, Integer> thisEdge = new Pair<>(srcId, dstId);
                HashIntSet apps = edges.getOrDefault(thisEdge, HashIntSets.newMutableSet());
                apps.add(Integer.parseInt(String.format("%d%d", index, s)));
                edges.put(thisEdge, apps);
            }
        }
        JSONObject obj = new JSONObject();
        JSONArray JSONnodes = new JSONArray();
        JSONArray JSONedges = new JSONArray();
        JSONArray JSONdensities = new JSONArray();
        List<Entry<Integer, Pair<Integer, HashIntSet>>> nodeList = new ArrayList(nodes.entrySet());
        Collections.sort(nodeList, (Entry<Integer, Pair<Integer, HashIntSet>> o1, Entry<Integer, Pair<Integer, HashIntSet>> o2) -> Integer.compare(o1.getValue().getA(), o2.getValue().getA()));
        for (Entry<Integer, Pair<Integer, HashIntSet>> entry : nodeList) {
            JSONObject n = new JSONObject();
            n.put("name", entry.getKey());
            StringBuilder builder = new StringBuilder();
            entry.getValue().getB().stream().forEach(sub -> builder.append(",").append(sub));
            n.put("sids", builder.substring(1));
            JSONnodes.put(n);
        }
        obj.put("nodes", JSONnodes);
        for (Entry<Pair<Integer, Integer>, HashIntSet> entry : edges.entrySet()) {
            JSONObject e = new JSONObject();
            e.put("source", entry.getKey().getA());
            e.put("target", entry.getKey().getB());
            StringBuilder builder = new StringBuilder();
            entry.getValue().stream().forEach(sub -> builder.append(",").append(sub));
            e.put("sids", builder.substring(1));
            JSONedges.put(e);
        }
        obj.put("edges", JSONedges);
        for (int id = index; id > 0; id --) {
            JSONObject d = new JSONObject();
            d.put("id", id);
            d.put("value", densities.get(id));
            JSONdensities.put(d);
        }
        obj.put("densities", JSONdensities);
        return obj.toString();
    }
    
    public static String graph2JSON(DynamicGraph graph, List<Pair<HashIntSet, Double>> result) throws JSONException {
        JSONObject obj = new JSONObject();
        JSONArray JSONnodes = new JSONArray();
        JSONArray JSONedges = new JSONArray();
        JSONArray JSONdensities = new JSONArray();

        if (result.isEmpty()) {
            return "";
        }
        
        HashIntObjMap<HashIntSet> nodes = HashIntObjMaps.newMutableMap();
        HashIntObjMap<HashIntSet> edges = HashIntObjMaps.newMutableMap();
        Collections.sort(result, (Pair<HashIntSet, Double> p1, Pair<HashIntSet, Double> p2) -> - Double.compare(p1.getB(), p2.getB()));
        HashIntDoubleMap densities = HashIntDoubleMaps.newMutableMap();
        DecimalFormat df = new DecimalFormat("#.###");
        double previousDensity = Double.parseDouble(df.format(result.get(0).getB()));
        int index = 1;
        densities.put(index, previousDensity);
        for (int s = 0; s < result.size(); s++) {
            for (int edge : result.get(s).getA()) {
                int src = graph.getSrc(edge);
                int dst = graph.getDst(edge);
                double thisDensity = Double.parseDouble(df.format(result.get(s).getB()));
                if (thisDensity < previousDensity && index < 6) {
                    index ++;
                    previousDensity = thisDensity;
                    densities.put(index, previousDensity);
                }
                HashIntSet srcA = nodes.getOrDefault(src, HashIntSets.newMutableSet());
                srcA.add(Integer.parseInt(String.format("%d%d", index, s)));
                nodes.put(src, srcA);
                HashIntSet dstA = nodes.getOrDefault(dst, HashIntSets.newMutableSet());
                dstA.add(Integer.parseInt(String.format("%d%d", index, s)));
                nodes.put(dst, dstA);
                HashIntSet apps = edges.getOrDefault(edge, HashIntSets.newMutableSet());
                apps.add(Integer.parseInt(String.format("%d%d", index, s)));
                edges.put(edge, apps);
            }
        }
        
        for (int n = 0; n < graph.getNumNodes(); n ++) {
            JSONObject node = new JSONObject();
            node.put("name", n);
            if (nodes.containsKey(n)) {
                StringBuilder builder = new StringBuilder();
                nodes.get(n).stream().forEach(sub -> builder.append(",").append(sub));
                node.put("sids", builder.substring(1));
            }
            JSONnodes.put(node);
        }
        obj.put("nodes", JSONnodes);
        for (DynamicEdge e : graph.getEdges()) {
            JSONObject edge = new JSONObject();
            edge.put("source", e.getSrc());
            edge.put("target", e.getDst());
            if (edges.containsKey(e.getEdgeID())) {
                StringBuilder builder = new StringBuilder();
                edges.get(e.getEdgeID()).stream().forEach(sub -> builder.append(",").append(sub));
                edge.put("sids", builder.substring(1));
            }
            JSONedges.put(edge);
        }
        obj.put("edges", JSONedges);
        for (int id = index; id > 0; id --) {
            JSONObject d = new JSONObject();
            d.put("id", id);
            d.put("value", densities.get(id));
            JSONdensities.put(d);
        }
        obj.put("densities", JSONdensities);
        obj.put("size", graph.getNumEdges());
        return obj.toString();
    }
    
    public static String views2JSON(DynamicGraph graph, HashIntObjMap<Pair<HashIntSet, Double>> views) throws JSONException {
        if (views.isEmpty()) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("#.###");
        JSONArray snapshots = new JSONArray();
        List<Entry<Integer, Pair<HashIntSet, Double>>> entries = new ArrayList(views.entrySet());
        Collections.sort(entries, (Entry<Integer, Pair<HashIntSet, Double>> e1, Entry<Integer, Pair<HashIntSet, Double>> e2) -> Integer.compare(e1.getKey(), e2.getKey()));
        for (Entry<Integer, Pair<HashIntSet, Double>> entry : entries) {
            HashIntIntMap nodes = HashIntIntMaps.newMutableMap();
            HashObjSet<Pair<Integer, Integer>> edges = HashObjSets.newMutableSet();
            int nodeCount = 0;
            for (int edge : entry.getValue().getA()) {
                int src = graph.getSrc(edge);
                int dst = graph.getDst(edge);
                int srcId;
                int dstId;
                if (nodes.containsKey(src)) {
                    srcId = nodes.get(src);
                } else {
                    srcId = nodeCount;
                    nodes.put(src, nodeCount);
                    nodeCount++;

                }
                if (nodes.containsKey(dst)) {
                    dstId = nodes.get(dst);
                } else {
                    dstId = nodeCount;
                    nodes.put(dst, nodeCount);
                    nodeCount++;
                }
                edges.add(new Pair<>(srcId, dstId));
            }
            JSONObject obj = new JSONObject();
            JSONArray JSONnodes = new JSONArray();
            JSONArray JSONedges = new JSONArray();
            List<Entry<Integer, Integer>> nodeList = new ArrayList(nodes.entrySet());
            Collections.sort(nodeList, (Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) -> Integer.compare(o1.getValue(), o2.getValue()));
            for (Entry<Integer, Integer> node : nodeList) {
                JSONObject n = new JSONObject();
                n.put("name", node.getKey());
                JSONnodes.put(n);
            }
            obj.put("nodes", JSONnodes);

            for (Pair<Integer, Integer> edge : edges) {
                JSONObject e = new JSONObject();
                e.put("source", edge.getA());
                e.put("target", edge.getB());
                JSONedges.put(e);
            }
            obj.put("edges", JSONedges);
            obj.put("density", Double.parseDouble(df.format(entry.getValue().getB())));
            JSONObject view = new JSONObject();
            view.put("number", entry.getKey());
            view.put("subgraph", obj);
            snapshots.put(view);
        }
        return snapshots.toString();
    }
    
    public static String labeled_views2JSON(DynamicGraph graph, HashIntObjMap<Pair<HashIntSet, Double>> views, HashIntObjMap<String> mapping, HashIntObjMap<String> snapMapping) throws JSONException {
        if (views.isEmpty()) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("#.###");
        JSONArray snapshots = new JSONArray();
        List<Entry<Integer, Pair<HashIntSet, Double>>> entries = new ArrayList(views.entrySet());
        Collections.sort(entries, (Entry<Integer, Pair<HashIntSet, Double>> e1, Entry<Integer, Pair<HashIntSet, Double>> e2) -> Integer.compare(e1.getKey(), e2.getKey()));
        for (Entry<Integer, Pair<HashIntSet, Double>> entry : entries) {
            HashIntObjMap<Pair<Integer, String>> nodes = HashIntObjMaps.newMutableMap();
            HashObjSet<Pair<Pair<Integer, String>, Pair<Integer, String>>> edges = HashObjSets.newMutableSet();
            int nodeCount = 0;
            for (int edge : entry.getValue().getA()) {
                int src = graph.getSrc(edge);
                int dst = graph.getDst(edge);
                String srcName;
                String dstName;
                String label = mapping.getOrDefault(edge, "none");
                if (label.equals("none")) {
                    srcName = "none";
                    dstName = "none";
                } else {
                    String[] elems = label.split("\\+\\+");
                    srcName = elems[0];
                    dstName = elems[1];
                }
                if (!nodes.containsKey(src)) {
                    nodes.put(src, new Pair<>(nodeCount, srcName));
                    nodeCount++;
                }
                if (!nodes.containsKey(dst)) {
                    nodes.put(dst, new Pair<>(nodeCount, dstName));
                    nodeCount++;
                }
                edges.add(new Pair<>(nodes.get(src), nodes.get(dst)));
            }
            JSONObject obj = new JSONObject();
            JSONArray JSONnodes = new JSONArray();
            JSONArray JSONedges = new JSONArray();
            List<Entry<Integer, Pair<Integer, String>>> nodeList = new ArrayList(nodes.entrySet());
            Collections.sort(nodeList, 
                    (Entry<Integer, Pair<Integer, String>> o1, Entry<Integer, Pair<Integer, String>> o2) -> Integer.compare(o1.getValue().getA(), o2.getValue().getA()));
            for (Entry<Integer, Pair<Integer, String>> node : nodeList) {
                JSONObject n = new JSONObject();
                n.put("name", node.getKey());
                n.put("label", node.getValue().getB());
                JSONnodes.put(n);
            }
            obj.put("nodes", JSONnodes);

            for (Pair<Pair<Integer, String>, Pair<Integer, String>> edge : edges) {
                JSONObject e = new JSONObject();
                e.put("source", edge.getA().getA());
                e.put("sourceLabel", edge.getA().getB());
                e.put("target", edge.getB().getA());
                e.put("targetLabel", edge.getB().getB());
                JSONedges.put(e);
            }
            obj.put("edges", JSONedges);
            obj.put("density", Double.parseDouble(df.format(entry.getValue().getB())));
            JSONObject view = new JSONObject();
            view.put("number", entry.getKey());
            view.put("label", snapMapping.getOrDefault(entry.getKey(), entry.getKey().toString()));
            view.put("subgraph", obj);
            snapshots.put(view);
        }
        return snapshots.toString();
    }

}
