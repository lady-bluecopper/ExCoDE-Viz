package eu.unitn.disi.db.dence.data;

import com.koloboke.collect.map.hash.HashObjIntMap;
import com.koloboke.collect.map.hash.HashObjIntMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import eu.unitn.disi.db.dence.graph.GPCorrelationGraph;
import eu.unitn.disi.db.dence.utils.CommandLineParser;
import eu.unitn.disi.db.dence.utils.Pair;
import eu.unitn.disi.db.dence.utils.Settings;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CCFinder {
    
    public static void main(String[] args) throws IOException {
        // Getting Parameters 
        CommandLineParser.parse(args);
        Pair<GPCorrelationGraph, HashObjIntMap<String>> p = loadGraph(Settings.dataFolder + Settings.edgeFile);
        GPCorrelationGraph g = p.getA();
        List<HashIntSet> ccs = g.findCCs();
        if (ccs.size() > 1) {
            writeCCs(Settings.dataFolder + Settings.edgeFile, ccs, p.getB());
        }
        System.out.println("FOUND: " + g.findCCs().size());
    }
    
    public static Pair<GPCorrelationGraph, HashObjIntMap<String>> loadGraph(String edgePath) throws IOException {
        HashIntSet nodes = HashIntSets.newMutableSet();
        HashObjIntMap<String> tmpMap = HashObjIntMaps.newMutableMap();
        List<Pair<Integer, Integer>> edges = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(edgePath), Charset.forName("ISO-8859-1"))) {
            stream.forEach(line -> {
                String[] parts = line.split(" ");
                int srcId = Integer.parseInt(parts[0]);
                String dstName = parts[1];
                nodes.add(srcId);
                tmpMap.putIfAbsent(dstName, - tmpMap.size());
                edges.add(new Pair(srcId, tmpMap.get(dstName)));
            });
            GPCorrelationGraph g = new GPCorrelationGraph(nodes.size() + tmpMap.size(), edges);
            return new Pair<GPCorrelationGraph, HashObjIntMap<String>>(g, tmpMap);
        }
    }
    
    public static void writeCCs(String inputPath, List<HashIntSet> ccs, HashObjIntMap<String> map) throws IOException {
        List<String>[] files = new ArrayList[ccs.size()];
        for (int i = 0; i < files.length; i++) {
            files[i] = new ArrayList<>();
        }
        try (Stream<String> stream = Files.lines(Paths.get(inputPath), Charset.forName("ISO-8859-1"))) {
            stream.forEach(line -> {
                String[] parts = line.split(" ");
                int srcId = Integer.parseInt(parts[0]);
                for (int i = 0; i < ccs.size(); i ++) {
                    if (ccs.get(i).contains(srcId)) {
                        files[i].add(line);
                        break;
                    }
                }
            });
        }
        for (int i = 0; i < files.length; i++) {
            writeGraph(files[i], Settings.outputFolder + "out_" + i);
        }
    }
    
    public static void writeGraph(List<String> file, String path) throws IOException {
        FileWriter fw = new FileWriter(path);
        for (String s : file) {
            fw.write(s + "\n");
        }
        fw.close();
    }
    
}