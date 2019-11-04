package eu.unitn.disi.db.dence.main;

import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import eu.unitn.disi.db.dence.bucketization.MinHashBucketization;
import eu.unitn.disi.db.dence.densesub.DenseSubgraphsFinder;
import eu.unitn.disi.db.dence.graph.BinaryDynamicEdge;
import eu.unitn.disi.db.dence.graph.BinaryDynamicGraph;
import eu.unitn.disi.db.dence.graph.DynamicEdge;
import eu.unitn.disi.db.dence.graph.DynamicGraph;
import eu.unitn.disi.db.dence.graph.GPCorrelationGraph;
import eu.unitn.disi.db.dence.graph.WeightedDynamicEdge;
import eu.unitn.disi.db.dence.graph.WeightedDynamicGraph;
import static eu.unitn.disi.db.dence.main.Main.loadBGPGraph;
import static eu.unitn.disi.db.dence.main.Main.loadEdges;
import static eu.unitn.disi.db.dence.main.Main.watch;
import eu.unitn.disi.db.dence.maxclique.GPCliqueFinder;
import eu.unitn.disi.db.dence.utils.CommandLineParser;
import eu.unitn.disi.db.dence.utils.Pair;
import eu.unitn.disi.db.dence.utils.Settings;
import eu.unitn.disi.db.dence.utils.StopWatch;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 *
 * @author bluecopper
 */
public class Test {
    
    public static void main(String[] args) throws IOException {
        CommandLineParser.parse(args);
        test();
//        createASMapping(Settings.dataFolder + "node_labels.txt", Settings.dataFolder + "ids.txt", Settings.dataFolder + "bgp.csv", Settings.dataFolder + "regions.txt");
//        createBGPMapping(Settings.dataFolder + "bgp.csv", Settings.dataFolder + "as_mapping.txt");
    }
    
    public static void createBGPMapping(String graph, String nodeMapping) throws IOException {
        HashIntObjMap<String> nodeMap = HashIntObjMaps.newMutableMap();
        FileWriter fw = new FileWriter(Settings.dataFolder + "bgp.csv_mapping");
        try (Stream<String> stream = Files.lines(Paths.get(nodeMapping))) {
            stream.forEach(line -> {
                String[] lst = line.split("\t");
                nodeMap.put(Integer.parseInt(lst[0]), lst[1] + "+" + lst[2]);
            });
        }
        try (final BufferedReader rows = new BufferedReader(new FileReader(graph))) {
            String line;
            int edge_count = 0;
            while ((line = rows.readLine()) != null) {
                String[] lst = line.split(" ");
                fw.write(edge_count + "\t" + 
                        nodeMap.getOrDefault(Integer.parseInt(lst[0]), "none") + "++" +
                        nodeMap.getOrDefault(Integer.parseInt(lst[1]), "none") + "\n");
                edge_count++;
            }
            fw.flush();
        }
    }

    public static void createASMapping(String file1, String file2, String graph, String mapping) throws IOException {
        HashObjObjMap<Pair<Integer, Integer>, String> regions = HashObjObjMaps.newMutableMap();
        HashIntSet ases = HashIntSets.newMutableSet();
        FileWriter fw = new FileWriter(Settings.dataFolder + "as_mapping.txt");
        try (Stream<String> stream = Files.lines(Paths.get(mapping))) {
            stream.forEach(line -> {
                String[] lst = line.split("\t");
                String[] pairs = lst[0].split(",");
                for (String pair : pairs) {
                    String[] elems = pair.split("-");
                    if (elems.length == 1) {
                        regions.put(new Pair<>(Integer.parseInt(pair), Integer.parseInt(pair)), lst[1]);
                    } else {
                        regions.put(new Pair<>(Integer.parseInt(elems[0]), Integer.parseInt(elems[1])), lst[1]);
                    }
                }
            });
        }
        try (Stream<String> stream = Files.lines(Paths.get(file1), Charset.forName("ISO-8859-1"))) {
            stream.forEach(line -> {
                try {
                    fw.write(line + "\n");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                ases.add(Integer.parseInt(line.split("\t")[0]));
            });
        }
        try (Stream<String> stream = Files.lines(Paths.get(file2), Charset.forName("ISO-8859-1"))) {
            stream.forEach(line -> {
                String[] lst = line.split("\t");
                int id = Integer.parseInt(lst[0]);
                if (!ases.contains(id)) {
                    ases.add(id);
                    String region = "none";
                    for (Entry<Pair<Integer, Integer>, String> e : regions.entrySet()) {
                        int src = e.getKey().getA();
                        int dst = e.getKey().getB();
                        if (id >= src && id <= dst) {
                            region = e.getValue();
                            break;
                        }
                    }
                    try {
                        fw.write(lst[0] + "\t" + region + "\t" + lst[1] + "\n");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
        try (Stream<String> stream = Files.lines(Paths.get(graph), Charset.forName("ISO-8859-1"))) {
            stream.forEach(line -> {
                String[] ids = line.trim().split(" ");
                int id1 = Integer.parseInt(ids[0]);
                int id2 = Integer.parseInt(ids[1]);
                if (!ases.contains(id1)) {
                    ases.add(id1);
                    String region = "none";
                    for (Entry<Pair<Integer, Integer>, String> e : regions.entrySet()) {
                        int src = e.getKey().getA();
                        int dst = e.getKey().getB();
                        if (id1 >= src && id1 <= dst) {
                            region = e.getValue();
                            break;
                        }
                    }
                    try {
                        fw.write(id1 + "\t" + region + "\tno name\n");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                if (!ases.contains(id2)) {
                    ases.add(id2);
                    String region = "none";
                    for (Entry<Pair<Integer, Integer>, String> e : regions.entrySet()) {
                        int src = e.getKey().getA();
                        int dst = e.getKey().getB();
                        if (id2 >= src && id2 <= dst) {
                            region = e.getValue();
                            break;
                        }
                    }
                    try {
                        fw.write(id2 + "\t" + region + "\tno name\n");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            fw.flush();
        }
    }
    
    public static void test() throws IOException {
        DynamicGraph graph = loadEdges(Settings.dataFolder + Settings.edgeFile, Settings.minEdgesInSnap);
        List<HashIntSet> ccs = graph.findCCs();
        Collections.sort(ccs, (HashIntSet o1, HashIntSet o2) -> - Integer.compare(o1.size(), o2.size()));
        HashIntSet retainedEdges = ccs.get(0);
        List<Integer> seeds = new ArrayList<>(retainedEdges);
        Collections.sort(seeds, (Integer o1, Integer o2) -> {
            DynamicEdge e1 = graph.getEdge(o1);
            DynamicEdge e2 = graph.getEdge(o2);
            int max1 = graph.getReachableNodes(e1.getSrc()).size();
            max1 = Math.max(max1, graph.getReachableNodes(e1.getDst()).size());
            int max2 = graph.getReachableNodes(e2.getSrc()).size();
            max2 = Math.max(max2, graph.getReachableNodes(e2.getDst()).size());
            return - Integer.compare(max1, max2);
        });
        int maxD = 3;
        double density = 0;
        int seed = -1;
        HashIntSet refinedSeeds;
        for (int i = 0; i < seeds.size(); i++) {
            refinedSeeds = graph.bfsMaxD(graph.getEdge(seeds.get(i)).getSrc(), maxD);
            double thisD = refinedSeeds.size() * 1.0 / graph.getNodesOf(refinedSeeds).size();
            if (thisD > density && graph.getNodesOf(refinedSeeds).size() < 1000) {
                seed = i;
                density = thisD;
            }
        }
        refinedSeeds = graph.bfsMaxD(graph.getEdge(seeds.get(seed)).getSrc(), maxD);
        final BufferedReader rows = new BufferedReader(new FileReader(Settings.dataFolder + Settings.edgeFile));
        final FileWriter fw = new FileWriter(Settings.dataFolder + "pruned");
        String line;
        int edge_counter = 0;
        while ((line = rows.readLine()) != null) {
            if (refinedSeeds.contains(edge_counter)) {
                fw.write(line + "\n");
            }
            edge_counter ++;
        }
        fw.flush();
    }
        
}    
