package eu.unitn.disi.db.dence.utils;

import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashObjIntMap;
import com.koloboke.collect.map.hash.HashObjIntMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author bluecopper
 */
public class Statistics {

    public static void main(String[] args) throws IOException {
        CommandLineParser.parse(args);

        switch (Settings.statNo) {
            case 0:
                randAccuracyTests();
                break;
            case 1:
                AXAccuracyTests();
                break;
            case 2:
                areMaximal();
                break;
            case 3:
                subgraphStats();
                break;
            case 4:
                redundancyScore();
                break;
            default:
                break;
        }

    }

    public static void randAccuracyTests() throws IOException {
        String[] ls = new String[]{"1-7-1", "2-7-1", "3-7-1", "1-7-3", "2-7-3", "3-7-3"};
        for (String l : ls) {
            String axFile = "ciForager_gaussian-" + l /*+ "_R"*/ + ".csv";
            String exactFile = "gaussian-" + l + ".csv.clusters";
            String edgeFile = "gaussian-" + l /*+ "_R"*/ + ".csv";
            HashObjSet<HashIntSet> clustering = loadClustering(Settings.dataFolder + "groundtruth/" + exactFile, Settings.dataFolder + edgeFile);
            System.out.println("Clustering loaded.");
            File directory = new File(Settings.outputFolder);
            for (File file : directory.listFiles()) {
                if (file.getName().startsWith(axFile)) {
                    System.out.println("Processing file: " + file.getName());
                    HashObjSet<HashIntSet> denseSubs = loadDenseSubs(file.getAbsolutePath());
                    computeAccuracy(clustering, denseSubs, file.getName().substring(6), Settings.outputFolder + "accuracy_rand.txt");
                }
            }
        }
    }

    public static void AXAccuracyTests() throws IOException {
        File directory = new File(Settings.outputFolder + "ax/");
        for (File file : directory.listFiles()) {
            if (file.getName().startsWith(Settings.axFile)) {
                System.out.println("Processing file: " + file.getName());
                HashObjSet<HashIntSet> ax_denseSubs = loadDenseSubs(file.getAbsolutePath());
                HashObjSet<HashIntSet> ex_denseSubs = loadDenseSubs(Settings.outputFolder + "exact/" + file.getName().substring(0, file.getName().length() - 5));
                if (ex_denseSubs != null) {
                    System.out.println(ex_denseSubs.size() + " " + ax_denseSubs.size());
                    computeAccuracy(ex_denseSubs, ax_denseSubs, file.getName().substring(6), Settings.outputFolder + "accuracy_ax.txt");
                }
            }
        }
    }

    public static void areMaximal() {
        File directory = new File(Settings.outputFolder);
        for (File file : directory.listFiles()) {
            if (file.getName().startsWith(Settings.exactFile)) {
                System.out.println("Processing file: " + file.getName());
                HashObjSet<HashIntSet> exact = loadDenseSubs(file.getAbsolutePath());
                areMaximal(exact);
            }
        }
    }

    public static void subgraphStats() {
        File directory = new File(Settings.outputFolder);
        for (File file : directory.listFiles()) {
            if (file.getName().startsWith(Settings.exactFile)) {
                System.out.println("Processing file: " + file.getName());
                HashObjSet<HashIntSet> exact = loadDenseSubs(file.getAbsolutePath());
                String fileName = file.getName().replaceAll(".csv", "").replaceAll("DenCE_", "");
                parseSet(exact, fileName, Settings.outputFolder + "subgraphs.stats");
            }
        }
    }

    public static void redundancyScore() {
        File directory = new File(Settings.outputFolder);
        for (File file : directory.listFiles()) {
            if (file.getName().startsWith(Settings.exactFile)) {
                System.out.println("Processing file: " + file.getName());
                HashObjSet<HashIntSet> result = loadDenseSubs(file.getAbsolutePath());
                String fileName = file.getName().replaceAll(".csv", "").replaceAll("DenCE_", "");
                computeRedundancy(result, fileName, Settings.outputFolder + "redundancy.stats");
            }
        }
    }

    public static HashObjSet<HashIntSet> loadDenseSubs(String fileName) {
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        try (BufferedReader rows = new BufferedReader(new FileReader(Paths.get(fileName).toFile()))) {
            String line = rows.readLine();
            while (line != null) {
                String[] edges = line.split("\t");
                HashIntSet current = HashIntSets.newMutableSet();
                for (String e : edges) {
                    current.add(Integer.parseInt(e));
                }
                denseSubs.add(current);
                line = rows.readLine();
            }
        } catch (Exception e) {
            return null;
        }
        return denseSubs;
    }

    public static HashObjSet<HashIntSet> loadClustering(String clustFile, String edgeFile) throws IOException {
        HashObjIntMap<String> edgeIDs = HashObjIntMaps.newMutableMap();
        HashIntObjMap<HashIntSet> clustering = HashIntObjMaps.newMutableMap();

        try (BufferedReader rows = new BufferedReader(new FileReader(Paths.get(edgeFile).toFile()))) {
            String line = rows.readLine();
            int counter = 0;
            while (line != null) {
                String[] list = line.split(" ");
                edgeIDs.put(list[0] + "-" + list[1], counter);
                counter++;
                line = rows.readLine();
            }
            rows.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
        try (BufferedReader rows = new BufferedReader(new FileReader(Paths.get(clustFile).toFile()))) {
            String line = rows.readLine();
            while (line != null) {
                String[] list = line.trim().split(" ");
                String src = list[0].substring(1, list[0].length() - 1);
                String dst = list[1].substring(0, list[1].length() - 1);
                int cluster = Integer.parseInt(list[2]);
                HashIntSet current = clustering.getOrDefault(cluster, HashIntSets.newMutableSet());
                current.add((int) edgeIDs.get(src + "-" + dst));
                clustering.put(cluster, current);
                line = rows.readLine();
            }
            rows.close();
        } catch (IOException ex) {
            System.exit(0);
        }
        return HashObjSets.newMutableSet(clustering.values());
    }

    public static void computeAccuracy(HashObjSet<HashIntSet> exact, HashObjSet<HashIntSet> approx, String nameFile, String outFile) {
        if (!exact.isEmpty()) {
            List<Double> jaccards_p = new ArrayList<Double>();
            approx.stream().forEach(a -> {
                double max = 0;
                for (HashIntSet e : exact) {
                    max = Math.max(max, Utilities.jaccardSimilarity(a, e));
                }
                jaccards_p.add(max);
            });
            Collections.sort(jaccards_p);
            double avg_p;
            if (jaccards_p.isEmpty()) {
                avg_p = 0;
            } else {
                avg_p = jaccards_p.stream().mapToDouble(x -> x).average().getAsDouble();
            }
            List<Double> jaccards_r = new ArrayList<Double>();
            exact.stream().forEach(e -> {
                double max = 0;
                for (HashIntSet a : approx) {
                    max = Math.max(max, Utilities.jaccardSimilarity(a, e));
                }
                jaccards_r.add(max);
            });
            Collections.sort(jaccards_r);
            double avg_r;
            if (jaccards_r.isEmpty()) {
                avg_r = 0;
            } else {
                avg_r = jaccards_r.stream().mapToDouble(x -> x).average().getAsDouble();
            }
            double min_p = (jaccards_p.isEmpty()) ? 0 : jaccards_p.get(0);
            double min_r = (jaccards_r.isEmpty()) ? 0 : jaccards_r.get(0);
            double max_p = (jaccards_p.isEmpty()) ? 0 : jaccards_p.get(jaccards_p.size() - 1);
            double max_r = (jaccards_r.isEmpty()) ? 0 : jaccards_r.get(jaccards_r.size() - 1);
            System.out.println(min_p + " " + min_r + " " + avg_p + " " + avg_r + " " + max_p + " " + max_r);
            double min_f1 = (min_p + min_r == 0) ? 0 : 2 * (min_p * min_r) / (min_p + min_r);
            double avg_f1 = (avg_p + avg_r == 0) ? 0 : 2 * (avg_p * avg_r) / (avg_p + avg_r);
            try {
                FileWriter fw = new FileWriter(outFile, true);
                fw.write(String.format("%s\t%f\t%f\n", nameFile.replace("_", "\t"), min_f1, avg_f1));
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void areMaximal(HashObjSet<HashIntSet> ex) {
        ex.stream().forEach(sub -> {
            if (ex.stream().filter(eSub -> !eSub.equals(sub)).anyMatch(eSub -> eSub.containsAll(sub))) {
                System.out.print("WARNING! ");
                Utilities.printComponent(sub);
            }
        });
    }

    public static void parseSet(HashObjSet<HashIntSet> ex, String fileName, String outFile) {
        if (!ex.isEmpty()) {
            HashIntIntMap counts = HashIntIntMaps.newMutableMap();
            ex.stream().forEach(sub -> {
                counts.put(sub.size(), counts.getOrDefault(sub.size(), 0) + 1);
            });
            int min = 10000;
            int max = 0;
            double avg = 0;
            for (int size : counts.keySet()) {
                avg += size;
                min = Math.min(min, size);
                max = Math.max(max, size);
            }
            avg /= counts.size();
            try {
                FileWriter fw = new FileWriter(outFile, true);
                fw.write(String.format("%s\t%d\t%f\t%d\n", fileName, min, avg, max));
                for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
                    fw.write(String.format("\t\t%d\t%d\n", e.getKey(), e.getValue()));
                }
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void computeRedundancy(HashObjSet<HashIntSet> ex, String fileName, String outFile) {
        List<Double> averages = new ArrayList<Double>();
        if (!ex.isEmpty()) {
            ex.stream().forEach(sub -> {
                int count = 0;
                double avg = 0;
                for (HashIntSet eSub : ex) {
                    if (!eSub.equals(sub)) {
                        avg += Utilities.jaccardSimilarity(eSub, sub);
                        count ++;
                    }
                }
                avg /= count;
                averages.add(avg);
            });
            Collections.sort(averages);
            double mediana;
            double collectiveAvg = averages.stream().mapToDouble(x -> x).average().getAsDouble();
            if (averages.size() % 2 == 1) {
                mediana = averages.get(averages.size() / 2);
            } else {
                mediana = (averages.get((averages.size() / 2) - 1) + averages.get(averages.size() / 2)) / 2;
            }
            int distinct = ex.stream().reduce(HashIntSets.newMutableSet(), (HashIntSet s1, HashIntSet s2) -> {s1.addAll(s2); return s1;}).size();
            try {
                FileWriter fw = new FileWriter(outFile, true);
                fw.write(String.format("%s\t%d\t%f\t%f\n", fileName, distinct, collectiveAvg, mediana));
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
