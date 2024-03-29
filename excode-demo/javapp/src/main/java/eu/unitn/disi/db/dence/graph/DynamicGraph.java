package eu.unitn.disi.db.dence.graph;

import com.google.common.collect.Lists;
import com.koloboke.collect.map.hash.HashIntDoubleMap;
import com.koloboke.collect.map.hash.HashIntDoubleMaps;
import com.koloboke.collect.map.hash.HashIntIntMap;
import com.koloboke.collect.map.hash.HashIntIntMaps;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashObjObjMap;
import com.koloboke.collect.map.hash.HashObjObjMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import com.koloboke.collect.set.hash.HashObjSet;
import com.koloboke.collect.set.hash.HashObjSets;
import eu.unitn.disi.db.dence.correlation.CorrelationMeasure;
import eu.unitn.disi.db.dence.correlation.PearsonCorrelation;
import eu.unitn.disi.db.dence.densesub.MetaData;
import eu.unitn.disi.db.dence.utils.Pair;
import eu.unitn.disi.db.dence.webserver.utils.Configuration.Subgraph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.apache.commons.collections4.collection.SynchronizedCollection;

/**
 *
 * @author bluecopper
 */
public abstract class DynamicGraph extends Graph<DynamicEdge> {
    
    private final int numSnapshots;
    private final int numEdgesPerSnap;

    public DynamicGraph(int numNodes, int numEdges, int numSnapshots, int numEdgesPerSnap) {
        super(numNodes, numEdges);
        this.numSnapshots = numSnapshots;
        this.numEdgesPerSnap = numEdgesPerSnap;
    }
    
    public int getNumSnaps() {
        return numSnapshots;
    }
    
    public void addEdge(DynamicEdge edge) {
        edges.add(edge);
        nodes[edge.getSrc()].addReachableNode(edge.getDst());
        nodeEdges[edge.getSrc()][edge.getDst()] = edge.getEdgeID();
        nodes[edge.getDst()].addReachableNode(edge.getSrc());
        nodeEdges[edge.getDst()][edge.getSrc()] = edge.getEdgeID();
    }
    
    public Collection<Pair<Integer, Integer>> findCorrelatedPairs(double minCor) {
        HashIntObjMap<double[]> series = HashIntObjMaps.newMutableMap();
        edges.stream().forEach(edge -> series.put(edge.getEdgeID(), createSeriesVector(edge)));
        Collection<Pair<Integer, Integer>> pairs = SynchronizedCollection.synchronizedCollection(Lists.newArrayList());
        final CorrelationMeasure measure = new PearsonCorrelation();
        edges.parallelStream().forEach(e1 -> {
            edges.parallelStream()
                    .filter(e2 -> e1.getEdgeID() < e2.getEdgeID())
                    .filter(e2 -> measure.computeCorrelation(series.get(e1.getEdgeID()), series.get(e2.getEdgeID())) >= minCor)
                    .forEach(e2 -> pairs.add(new Pair<Integer, Integer>(e1.getEdgeID(), e2.getEdgeID())));
        });
        return pairs;
    }
    
    public Collection<Pair<Integer, Integer>> findCorrelatedPairs(Collection<Pair<Integer, Integer>> candPair, double minCor) {
        HashIntObjMap<double[]> series = HashIntObjMaps.newMutableMap();
        edges.stream().forEach(edge -> series.put(edge.getEdgeID(), createSeriesVector(edge)));
        Collection<Pair<Integer, Integer>> corrPairs = SynchronizedCollection.synchronizedCollection(Lists.newArrayList());
        final CorrelationMeasure measure = new PearsonCorrelation();
        candPair.stream()
                .distinct()
                .parallel()
                .forEach(pair -> { 
                    if (measure.computeCorrelation(series.get(pair.getA()), series.get(pair.getB())) >= minCor) {
                        corrPairs.add(pair);
                    }
                });
        return corrPairs;
    }
    
    private double[] createSeriesVector(DynamicEdge e1) {
        double[] s1 = new double[numSnapshots];
        if (e1 instanceof BinaryDynamicEdge) {
            BinaryDynamicEdge edge1 = (BinaryDynamicEdge) e1;
            for (int i = 0; i < numSnapshots; i++) {
                s1[i] = (edge1.getEdgeSeries().contains(i)) ? 1 : 0;
            }
        } else {
            WeightedDynamicEdge edge1 = (WeightedDynamicEdge) e1;
            for (int i = 0; i < numSnapshots; i++) {
                s1[i] = edge1.getEdgeSeries().getOrDefault(i, 0.0);
            }
        }
        return s1;
    }
    
    public int getDegreeInT(int nodeId, int t) {
        return (int) IntStream.range(0, nodeEdges.length)
                .unordered()
                .parallel()
                .filter(i -> (nodeEdges[nodeId][i] != -1) && (edges.get(nodeEdges[nodeId][i]).existsInT(t)))
                .count();
    }
    
    public int getDegreeInT(int nodeId, int t, Set<Integer> cands) {
        return (int) IntStream.range(0, nodeEdges.length)
                .unordered()
                .parallel()
                .filter(i -> (cands.contains(nodeEdges[nodeId][i]) && (edges.get(nodeEdges[nodeId][i]).existsInT(t))))
                .count();
    }
    
    public double getAVGDegreeinT(Set<Integer> nodes, Set<Integer> edges, int t) {
        return (nodes.isEmpty()) ? 0 : 
                nodes.parallelStream()
                    .mapToInt(n -> getDegreeInT(n, t, edges))
                    .average()
                    .getAsDouble();
    }
    
    public Pair<Double, Integer> getAVGDegreeinTWithReturn(Set<Integer> nodes, Set<Integer> edges, int t) {
        int minDeg = nodes.size();
        int minNode = -1;
        double sumDeg = 0;
        for (int n : nodes) {
            int deg = getDegreeInT(n, t, edges);
            if (deg < minDeg) {
                minDeg = deg;
                minNode = n;
            }
            sumDeg += deg;
        }
        double avgDeg = (nodes.isEmpty()) ? 0 : sumDeg / nodes.size();
        return new Pair<Double, Integer>(avgDeg, minNode);
    }
    
    public int getMinDegNodeInT(Set<Integer> nodes, Set<Integer> edges, int t) {
        int minDeg = nodes.size();
        int minNode = -1;
        for (int n : nodes) {
            int deg = getDegreeInT(n, t, edges);
            if (deg < minDeg) {
                minDeg = deg;
                minNode = n;
            }
        }
        return minNode;
    }
    
    public Pair<HashIntSet, Integer> getKEdgeSnaps_old(HashIntSet comp) {
        HashIntIntMap existCount = HashIntIntMaps.newMutableMap();
        getEdgeList(comp).stream().forEach(edge -> {
            IntStream.range(0, numSnapshots).forEach(t -> {
                if (edge.existsInT(t)) {
                    existCount.put(t, existCount.getOrDefault(t, 0) + 1);
                }
            });
        });
        HashIntSet existing = existCount.entrySet().stream()
                .filter(e -> e.getValue() >= numEdgesPerSnap)
                .collect(() -> HashIntSets.newMutableSet(), 
                        (HashIntSet t, Map.Entry<Integer, Integer> u) -> t.add(u.getKey()), 
                        (HashIntSet t, HashIntSet u) -> t.addAll(u));
        int skipped = 0;
        if (numEdgesPerSnap > 0 && existing.size() < numSnapshots) {
            skipped = existCount.entrySet().stream()
                .filter(e -> e.getValue() < numEdgesPerSnap)
                .mapToInt(e -> e.getValue())
                .sum();
        }
        return new Pair<HashIntSet, Integer>(existing, skipped);
    }
    
    public Pair<HashIntSet, Integer> getKEdgeSnaps(HashIntSet comp, int edgesInSnap) {
        HashIntIntMap existCount = HashIntIntMaps.newMutableMap();
        getEdgeList(comp).stream().forEach(edge -> {
            IntStream.range(0, numSnapshots).forEach(t -> {
                if (edge.existsInT(t)) {
                    existCount.put(t, existCount.getOrDefault(t, 0) + 1);
                }
            });
        });
        HashIntSet existing = existCount.entrySet().stream()
                .filter(e -> e.getValue() >= comp.size() * edgesInSnap / 100.)
                .collect(() -> HashIntSets.newMutableSet(), 
                        (HashIntSet t, Map.Entry<Integer, Integer> u) -> t.add(u.getKey()), 
                        (HashIntSet t, HashIntSet u) -> t.addAll(u));
        int skipped = 0;
        if (edgesInSnap > 0 && existing.size() < numSnapshots) {
            skipped = existCount.entrySet().stream()
                .filter(e -> e.getValue() < comp.size() * edgesInSnap / 100.)
                .mapToInt(e -> e.getValue())
                .sum();
        }
        return new Pair<HashIntSet, Integer>(existing, skipped);
    }
    
    public boolean containsDenseSubgraph(HashIntSet comp, double minDen) {
        HashIntSet temp = HashIntSets.newMutableSet(comp);
        HashIntObjMap<HashIntSet> nodeEdgeMap = HashIntObjMaps.newMutableMap();
        int min;
        int max;
        int minIndex;
        comp.stream().forEach(e -> {
            DynamicEdge edge = edges.get(e);
            HashIntSet srcList = nodeEdgeMap.getOrDefault(edge.getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = nodeEdgeMap.getOrDefault(edge.getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            nodeEdgeMap.put(edge.getSrc(), srcList);
            nodeEdgeMap.put(edge.getDst(), dstList);
        });
        int numNodes = nodeEdgeMap.size();
        while (!isMINDense(nodeEdgeMap.keySet(), temp, minDen)) {
            if (numNodes < 2 && minDen > 0) {
                return false;
            }
            min = Integer.MAX_VALUE;
            max = 0;
            minIndex = -1;
            for (Map.Entry<Integer, HashIntSet> e : nodeEdgeMap.entrySet()) {
                int curr = e.getValue().size();
                if (e.getValue().size() < min) {
                    minIndex = e.getKey();
                    min = e.getValue().size();
                }
                max = Math.max(curr, max);
            }
            if (max < minDen) {
                return false;
            }
            HashIntSet toRemove = HashIntSets.newMutableSet(nodeEdgeMap.getOrDefault(minIndex, HashIntSets.newMutableSet()));
            temp.removeAll(toRemove);
            nodeEdgeMap.values().stream().forEach(edgeList -> edgeList.removeAll(toRemove));
            nodeEdgeMap.remove(minIndex);
            numNodes--;
        }
        return true;
    }
    
    public double getMINDensity(HashIntSet comp, HashIntSet existing) {
        HashIntSet compNodes = getNodesOf(comp);
        return IntStream.range(0, numSnapshots)
                .parallel()
                .mapToObj(t -> {
                    HashIntIntMap nodeDegs = HashIntIntMaps.newMutableMap();
                    final int s = t;
                    comp.stream()
                            .filter(id -> edges.get(id).existsInT(s))
                            .forEach(id -> {
                                DynamicEdge e = edges.get(id);
                                nodeDegs.put(e.getSrc(), nodeDegs.getOrDefault(e.getSrc(), 0) + 1);
                                nodeDegs.put(e.getDst(), nodeDegs.getOrDefault(e.getDst(), 0) + 1);
                            });
                    return nodeDegs;
                })
                .mapToDouble(nodeDegs -> (nodeDegs.size() < 2) ? 0 : compNodes.stream().mapToInt(x -> nodeDegs.getOrDefault((int) x, 0)).average().getAsDouble())
                .min()
                .getAsDouble();
    }
    
    public boolean isMINDense(HashIntSet comp, double minDen) {
        HashIntSet compNodes = getNodesOf(comp);
        return IntStream.range(0, numSnapshots)
                .parallel()
                .mapToObj(t -> {
                    HashIntIntMap nodeDegs = HashIntIntMaps.newMutableMap();
                    final int s = t;
                    comp.stream()
                            .filter(id -> edges.get(id).existsInT(s))
                            .forEach(id -> {
                                DynamicEdge e = edges.get(id);
                                nodeDegs.put(e.getSrc(), nodeDegs.getOrDefault(e.getSrc(), 0) + 1);
                                nodeDegs.put(e.getDst(), nodeDegs.getOrDefault(e.getDst(), 0) + 1);
                            });
                    return nodeDegs;
                })
                .map(nodeDegs -> (nodeDegs.size() < 2) ? 0 : compNodes.stream().mapToInt(x -> nodeDegs.getOrDefault((int) x, 0)).average().getAsDouble())
                .noneMatch(avgDeg -> avgDeg < minDen);
    }
    
    public boolean isMINDense(HashIntSet comp, HashIntSet existing, double minDen) {
        if (existing.isEmpty()) {
            return false;
        }
        HashIntSet compNodes = getNodesOf(comp);
        return existing.parallelStream()
                .map(t -> {
                    HashIntDoubleMap nodeDegs = HashIntDoubleMaps.newMutableMap();
                    final int s = t;
                    comp.stream()
                            .filter(id -> edges.get(id).existsInT(s))
                            .forEach(id -> {
                                if (edges.get(id) instanceof BinaryDynamicEdge) {
                                    BinaryDynamicEdge we = (BinaryDynamicEdge) edges.get(id);
                                    nodeDegs.put(we.getSrc(), nodeDegs.getOrDefault(we.getSrc(), 0.) + 1);
                                    nodeDegs.put(we.getDst(), nodeDegs.getOrDefault(we.getDst(), 0.) + 1);
                                } else {
                                    WeightedDynamicEdge we = (WeightedDynamicEdge) edges.get(id);
                                    nodeDegs.put(we.getSrc(), nodeDegs.getOrDefault(we.getSrc(), 0.) + we.getEdgeWeight(s));
                                    nodeDegs.put(we.getDst(), nodeDegs.getOrDefault(we.getDst(), 0.) + we.getEdgeWeight(s));
                                }
                            });
                    return nodeDegs;
                })
                .map(nodeDegs -> (nodeDegs.size() < 2) ? 0. : compNodes.stream().mapToDouble(x -> nodeDegs.getOrDefault((int) x, 0.)).average().getAsDouble())
                .noneMatch(avgDeg -> avgDeg < minDen);
    }
    
    public boolean containsDenseSubgraph(HashIntSet comp, HashIntSet existing, double minDen) {
        HashIntSet temp = HashIntSets.newMutableSet(comp);
        HashIntObjMap<HashIntSet> nodeEdgeMap = HashIntObjMaps.newMutableMap();
        int min;
        int max;
        int minIndex;
        comp.stream().forEach(e -> {
            DynamicEdge edge = edges.get(e);
            HashIntSet srcList = nodeEdgeMap.getOrDefault(edge.getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = nodeEdgeMap.getOrDefault(edge.getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            nodeEdgeMap.put(edge.getSrc(), srcList);
            nodeEdgeMap.put(edge.getDst(), dstList);
        });
        int numNodes = nodeEdgeMap.size();
        while (!isMINDense(temp, existing, minDen)) {
            if (numNodes < 2 && minDen > 0) {
                return false;
            }
//            if (existing.size() == 0) {
//                return false;
//            }
            min = Integer.MAX_VALUE;
            max = 0;
            minIndex = -1;
            for (Map.Entry<Integer, HashIntSet> e : nodeEdgeMap.entrySet()) {
                int curr = e.getValue().size();
                if (curr < min) {
                    minIndex = e.getKey();
                    min = curr;
                }
                max = Math.max(curr, max);
            }
            if (max < minDen) {
                return false;
            }
            HashIntSet toRemove = HashIntSets.newMutableSet(nodeEdgeMap.getOrDefault(minIndex, HashIntSets.newMutableSet()));
            temp.removeAll(toRemove);
            nodeEdgeMap.values().stream().forEach(edgeList -> edgeList.removeAll(toRemove));
            nodeEdgeMap.remove(minIndex);
            numNodes--;
            Pair<HashIntSet, Integer> p = getKEdgeSnaps(temp, numEdgesPerSnap);
            existing = p.getA();
        }
        return true;
    }
    
    public HashObjSet<HashIntSet> extractDenseSubgraphs(HashIntSet comp, double minDen) {
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        HashIntObjMap<HashObjSet<HashIntSet>> queue = HashIntObjMaps.newMutableMap();
        HashObjObjMap<HashIntSet, HashIntObjMap<HashIntSet>> nodeEdgeMap = HashObjObjMaps.newMutableMap();
        HashIntObjMap<HashIntSet> thisNodeEdgeMap = HashIntObjMaps.newMutableMap();
        int min;
        int max;
        comp.stream().forEach(e -> {
            HashIntSet srcList = thisNodeEdgeMap.getOrDefault(edges.get(e).getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = thisNodeEdgeMap.getOrDefault(edges.get(e).getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            thisNodeEdgeMap.put(edges.get(e).getSrc(), srcList);
            thisNodeEdgeMap.put(edges.get(e).getDst(), dstList);
        });
        int size = thisNodeEdgeMap.size();
        HashObjSet<HashIntSet> firstSet = HashObjSets.newMutableSet();
        firstSet.add(comp);
        queue.put(size, firstSet);
        nodeEdgeMap.put(comp, thisNodeEdgeMap);
        while (size > 1 && !queue.isEmpty()) {
            HashObjSet<HashIntSet> candidates = queue.getOrDefault(size, HashObjSets.newMutableSet()); 
            for (HashIntSet candidate : candidates) {
                HashIntObjMap<HashIntSet> nodeEdgeCand = nodeEdgeMap.get(candidate);
                if (isMINDense(candidate, minDen)) {
                    denseSubs.add(candidate);
                    continue;
                } 
                List<Integer> minIndices = Lists.newArrayList();
                min = Integer.MAX_VALUE;
                max = 0;
                for (Map.Entry<Integer, HashIntSet> e : nodeEdgeCand.entrySet()) {
                    int curr = e.getValue().size();
                    min = Math.min(curr, min);
                    max = Math.max(curr, max);
                }
                final int minDeg = min;
                if (max < minDen) {
                    continue;
                }
                nodeEdgeCand.entrySet().stream().filter(e -> e.getValue().size() == minDeg).forEach(e -> minIndices.add(e.getKey()));
                if (minDeg == 0) {
                    minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                    int newSize = size - minIndices.size();
                    HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                    currSet.add(candidate);
                    queue.put(newSize, currSet);
                    nodeEdgeMap.replace(candidate, nodeEdgeCand);
                    continue;
                } 
                if (minDeg == 1) {
                    HashIntSet tempCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet();
                    nodeEdgeCand.entrySet().stream()
                            .filter(e -> minIndices.contains(e.getKey()))
                            .forEach(e -> toRemove.addAll(e.getValue()));
                    tempCand.removeAll(toRemove);
                    if (tempCand.size() > 0 && !isMINDense(tempCand, minDen) && !nodeEdgeMap.containsKey(tempCand) && !nodeEdgeMap.containsKey(tempCand)) {
                        candidate.removeAll(toRemove);
                        nodeEdgeCand.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                        minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                        int newSize = size - minIndices.size();
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                        currSet.add(candidate);
                        queue.put(newSize, currSet);
                        nodeEdgeMap.put(candidate, nodeEdgeCand);
                        continue;
                    }
                }
                for (int node : minIndices) {
                    HashIntObjMap<HashIntSet> newMap = HashIntObjMaps.newMutableMap();
                    nodeEdgeCand.entrySet().forEach(entry -> newMap.put(entry.getKey(), HashIntSets.newMutableSet(entry.getValue())));
                    HashIntSet newCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet(newMap.getOrDefault(node, HashIntSets.newMutableSet()));
                    newCand.removeAll(toRemove);
                    newMap.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                    newMap.remove(node);
                    if (newCand.size() > 0 && !nodeEdgeMap.containsKey(newCand) && !nodeEdgeMap.containsKey(newCand)) {
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(size - 1, HashObjSets.newMutableSet());
                        currSet.add(newCand);
                        queue.put(size - 1, currSet);
                        nodeEdgeMap.put(newCand, newMap);
                    }
                }
            }
            queue.remove(size);
            size--;
        }
        return denseSubs;
    }
    
    public HashObjSet<HashIntSet> extractDenseSubgraphs(HashIntSet comp, HashIntSet existing, double minDen) {
        HashObjSet<HashIntSet> denseSubs = HashObjSets.newMutableSet();
        HashIntObjMap<HashObjSet<HashIntSet>> queue = HashIntObjMaps.newMutableMap();
        HashObjObjMap<HashIntSet, MetaData> candsData = HashObjObjMaps.newMutableMap();
        HashIntObjMap<HashIntSet> thisNodeEdgeMap = HashIntObjMaps.newMutableMap();
        int min;
        int max;
        comp.stream().forEach(e -> {
            HashIntSet srcList = thisNodeEdgeMap.getOrDefault(edges.get(e).getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = thisNodeEdgeMap.getOrDefault(edges.get(e).getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            thisNodeEdgeMap.put(edges.get(e).getSrc(), srcList);
            thisNodeEdgeMap.put(edges.get(e).getDst(), dstList);
        });
        int size = thisNodeEdgeMap.size();
        HashObjSet<HashIntSet> firstSet = HashObjSets.newMutableSet();
        firstSet.add(comp);
        queue.put(size, firstSet);
        candsData.putIfAbsent(comp, new MetaData<HashIntSet>(thisNodeEdgeMap, existing));
        while (size > 1 && !queue.isEmpty()) {
            HashObjSet<HashIntSet> candidates = queue.getOrDefault(size, HashObjSets.newMutableSet()); 
            for (HashIntSet candidate : candidates) {
                MetaData<HashIntSet> candData = candsData.get(candidate);
                HashIntObjMap<HashIntSet> nodeEdgeCand = candData.nodeEdgeMap;
                HashIntSet candExisting = candData.snapInfo;
                if (isMINDense(candidate, candExisting, minDen)) {
                    denseSubs.add(candidate);
                } else if (!candExisting.isEmpty()) {
                    min = nodeEdgeCand.values().stream().mapToInt(s -> s.size()).min().getAsInt();
                    max = nodeEdgeCand.values().stream().mapToInt(s -> s.size()).max().getAsInt();
                    if (max < minDen) {
                        continue;
                    }
                    List<Integer> minIndices = Lists.newArrayList();
                    final int minDeg = min;
                    nodeEdgeCand.entrySet().stream().filter(e -> e.getValue().size() == minDeg).forEach(e -> minIndices.add(e.getKey()));
                    if (minDeg == 0) {
                        minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                        int newSize = size - minIndices.size();
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                        currSet.add(candidate);
                        queue.put(newSize, currSet);
                        candsData.replace(candidate, new MetaData<HashIntSet>(nodeEdgeCand, candExisting));
                        continue;
                    } 
                    if (minDeg == 1) {
                        HashIntSet tempCand = HashIntSets.newMutableSet(candidate);
                        HashIntSet toRemove = HashIntSets.newMutableSet();
                        nodeEdgeCand.entrySet().stream()
                                .filter(e -> minIndices.contains(e.getKey()))
                                .forEach(e -> toRemove.addAll(e.getValue()));
                        tempCand.removeAll(toRemove);
                        Pair<HashIntSet, Integer> p = getKEdgeSnaps(tempCand, numEdgesPerSnap);
                        if (tempCand.size() > 0 && !isMINDense(tempCand, p.getA(), minDen) && !candsData.containsKey(tempCand)) {
                            candidate.removeAll(toRemove);
                            nodeEdgeCand.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                            minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                            int newSize = size - minIndices.size();
                            HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                            currSet.add(candidate);
                            queue.put(newSize, currSet);
                            candsData.put(candidate, new MetaData<HashIntSet>(nodeEdgeCand, p.getA()));
                            continue;
                        }
                    }
                    for (int node : minIndices) {
                        HashIntObjMap<HashIntSet> newMap = HashIntObjMaps.newMutableMap();
                        nodeEdgeCand.entrySet().forEach(entry -> newMap.put(entry.getKey(), HashIntSets.newMutableSet(entry.getValue())));
                        HashIntSet newCand = HashIntSets.newMutableSet(candidate);
                        HashIntSet toRemove = HashIntSets.newMutableSet(newMap.getOrDefault(node, HashIntSets.newMutableSet()));
                        int old = newCand.size();
                        newCand.removeAll(toRemove);
                        newMap.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                        newMap.remove(node);
                        if (newCand.size() > 0 && !candsData.containsKey(newCand)) {
                            HashObjSet<HashIntSet> currSet = queue.getOrDefault(size - 1, HashObjSets.newMutableSet());
                            currSet.add(newCand);
                            queue.put(size - 1, currSet);
                            Pair<HashIntSet, Integer> p = getKEdgeSnaps(newCand, numEdgesPerSnap);
                            candsData.put(newCand, new MetaData<HashIntSet>(newMap, p.getA()));
                        }
                    }
                }
            }
            queue.remove(size);
            size--;
        }
        return denseSubs;
    }
    
    public HashObjSet<Pair<HashIntSet, Double>> extractDenseSubgraphsWithScore(HashIntSet comp, HashIntSet existing, double minDen) {
        HashObjSet<Pair<HashIntSet, Double>> denseSubs = HashObjSets.newMutableSet();
        HashIntObjMap<HashObjSet<HashIntSet>> queue = HashIntObjMaps.newMutableMap();
        HashObjObjMap<HashIntSet, MetaData> candsData = HashObjObjMaps.newMutableMap();
        HashIntObjMap<HashIntSet> thisNodeEdgeMap = HashIntObjMaps.newMutableMap();
        int min;
        int max;
        comp.stream().forEach(e -> {
            HashIntSet srcList = thisNodeEdgeMap.getOrDefault(edges.get(e).getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = thisNodeEdgeMap.getOrDefault(edges.get(e).getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            thisNodeEdgeMap.put(edges.get(e).getSrc(), srcList);
            thisNodeEdgeMap.put(edges.get(e).getDst(), dstList);
        });
        int size = thisNodeEdgeMap.size();
        HashObjSet<HashIntSet> firstSet = HashObjSets.newMutableSet();
        firstSet.add(comp);
        queue.put(size, firstSet);
        candsData.putIfAbsent(comp, new MetaData<HashIntSet>(thisNodeEdgeMap, existing));
        while (size > 1 && !queue.isEmpty()) {
            HashObjSet<HashIntSet> candidates = queue.getOrDefault(size, HashObjSets.newMutableSet()); 
            for (HashIntSet candidate : candidates) {
                MetaData<HashIntSet> candData = candsData.get(candidate);
                HashIntObjMap<HashIntSet> nodeEdgeCand = candData.nodeEdgeMap;
                HashIntSet candExisting = candData.snapInfo;
                if (isMINDense(candidate, candExisting, minDen)) {
                    denseSubs.add(new Pair<>(candidate, getMINDensity(candidate, candExisting)));
                } else {
//                    if (!candExisting.isEmpty()) {
                    min = nodeEdgeCand.values().stream().mapToInt(s -> s.size()).min().getAsInt();
                    max = nodeEdgeCand.values().stream().mapToInt(s -> s.size()).max().getAsInt();
                    if (max < minDen) {
                        continue;
                    }
                    List<Integer> minIndices = Lists.newArrayList();
                    final int minDeg = min;
                    nodeEdgeCand.entrySet().stream().filter(e -> e.getValue().size() == minDeg).forEach(e -> minIndices.add(e.getKey()));
                    if (minDeg == 0) {
                        minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                        int newSize = size - minIndices.size();
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                        currSet.add(candidate);
                        queue.put(newSize, currSet);
                        candsData.replace(candidate, new MetaData<HashIntSet>(nodeEdgeCand, candExisting));
                        continue;
                    } 
                    if (minDeg == 1) {
                        HashIntSet tempCand = HashIntSets.newMutableSet(candidate);
                        HashIntSet toRemove = HashIntSets.newMutableSet();
                        nodeEdgeCand.entrySet().stream()
                                .filter(e -> minIndices.contains(e.getKey()))
                                .forEach(e -> toRemove.addAll(e.getValue()));
                        tempCand.removeAll(toRemove);
                        Pair<HashIntSet, Integer> p = getKEdgeSnaps(tempCand, numEdgesPerSnap);
                        if (tempCand.size() > 0 && !isMINDense(tempCand, p.getA(), minDen) && !candsData.containsKey(tempCand)) {
                            candidate.removeAll(toRemove);
                            nodeEdgeCand.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                            minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                            int newSize = size - minIndices.size();
                            HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                            currSet.add(candidate);
                            queue.put(newSize, currSet);
                            candsData.put(candidate, new MetaData<HashIntSet>(nodeEdgeCand, p.getA()));
                            continue;
                        } 
                    }
                    for (int node : minIndices) {
                        HashIntObjMap<HashIntSet> newMap = HashIntObjMaps.newMutableMap();
                        nodeEdgeCand.entrySet().forEach(entry -> newMap.put(entry.getKey(), HashIntSets.newMutableSet(entry.getValue())));
                        HashIntSet newCand = HashIntSets.newMutableSet(candidate);
                        HashIntSet toRemove = HashIntSets.newMutableSet(newMap.getOrDefault(node, HashIntSets.newMutableSet()));
                        newCand.removeAll(toRemove);
                        newMap.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                        newMap.remove(node);
                        if (newCand.size() > 0 && !candsData.containsKey(newCand)) {
                            HashObjSet<HashIntSet> currSet = queue.getOrDefault(size - 1, HashObjSets.newMutableSet());
                            currSet.add(newCand);
                            queue.put(size - 1, currSet);
                            Pair<HashIntSet, Integer> p = getKEdgeSnaps(newCand, numEdgesPerSnap);
                            candsData.put(newCand, new MetaData<HashIntSet>(newMap, p.getA()));
                        }
                    }
                }
            }
            queue.remove(size);
            size--;
        }
        return denseSubs;
    }
    
    public HashObjSet<HashIntSet> extractDenseSubgraph(HashIntSet comp, HashIntSet existing, double minDen) {
        HashIntObjMap<HashObjSet<HashIntSet>> queue = HashIntObjMaps.newMutableMap();
        HashObjObjMap<HashIntSet, MetaData> candsData = HashObjObjMaps.newMutableMap();
        HashIntObjMap<HashIntSet> thisNodeEdgeMap = HashIntObjMaps.newMutableMap();
        int min;
        int max;
        comp.stream().forEach(e -> {
            HashIntSet srcList = thisNodeEdgeMap.getOrDefault(edges.get(e).getSrc(), HashIntSets.newMutableSet());
            srcList.add(e);
            HashIntSet dstList = thisNodeEdgeMap.getOrDefault(edges.get(e).getDst(), HashIntSets.newMutableSet());
            dstList.add(e);
            thisNodeEdgeMap.put(edges.get(e).getSrc(), srcList);
            thisNodeEdgeMap.put(edges.get(e).getDst(), dstList);
        });
        int size = thisNodeEdgeMap.size();
        HashObjSet<HashIntSet> firstSet = HashObjSets.newMutableSet();
        firstSet.add(comp);
        queue.put(size, firstSet);
        candsData.putIfAbsent(comp, new MetaData<HashIntSet>(thisNodeEdgeMap, existing));
        while (size > 1 && !queue.isEmpty()) {
            HashObjSet<HashIntSet> candidates = queue.getOrDefault(size, HashObjSets.newMutableSet()); 
            for (HashIntSet candidate : candidates) {
                MetaData<HashIntSet> candData = candsData.get(candidate);
                HashIntObjMap<HashIntSet> nodeEdgeCand = candData.nodeEdgeMap;
                HashIntSet candExisting = candData.snapInfo;
                if (isMINDense(candidate, candExisting, minDen)) {
                    HashObjSet<HashIntSet> output = HashObjSets.newMutableSet();
                    output.add(candidate);
                    return output;
                }
//                if (candExisting.isEmpty()) {
//                    continue;
//                }
                List<Integer> minIndices = Lists.newArrayList();
                min = Integer.MAX_VALUE;
                max = 0;
                for (Map.Entry<Integer, HashIntSet> e : nodeEdgeCand.entrySet()) {
                    int curr = e.getValue().size();
                    min = Math.min(curr, min);
                    max = Math.max(curr, max);
                }
                if (max < minDen) {
                    continue;
                }
                final int minDeg = min;
                nodeEdgeCand.entrySet().stream().filter(e -> e.getValue().size() == minDeg).forEach(e -> minIndices.add(e.getKey()));
                if (minDeg == 0) {
                    minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                    int newSize = size - minIndices.size();
                    HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                    currSet.add(candidate);
                    queue.put(newSize, currSet);
                    candsData.replace(candidate, new MetaData<HashIntSet>(nodeEdgeCand, candExisting));
                    continue;
                } 
                if (minDeg == 1) {
                    HashIntSet tempCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet();
                    nodeEdgeCand.entrySet().stream()
                            .filter(e -> minIndices.contains(e.getKey()))
                            .forEach(e -> toRemove.addAll(e.getValue()));
                    tempCand.removeAll(toRemove);
                    Pair<HashIntSet, Integer> p = getKEdgeSnaps(tempCand, numEdgesPerSnap);
                    if (tempCand.size() > 0 && !isMINDense(tempCand, p.getA(), minDen) && !candsData.containsKey(tempCand)) {
                        candidate.removeAll(toRemove);
                        nodeEdgeCand.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                        minIndices.stream().forEach(id -> nodeEdgeCand.remove(id));
                        int newSize = size - minIndices.size();
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(newSize, HashObjSets.newMutableSet());
                        currSet.add(candidate);
                        queue.put(newSize, currSet);
                        candsData.put(candidate, new MetaData<HashIntSet>(nodeEdgeCand, p.getA()));
                        continue;
                    } 
                }
                for (int node : minIndices) {
                    HashIntObjMap<HashIntSet> newMap = HashIntObjMaps.newMutableMap();
                    nodeEdgeCand.entrySet().forEach(entry -> newMap.put(entry.getKey(), HashIntSets.newMutableSet(entry.getValue())));
                    HashIntSet newCand = HashIntSets.newMutableSet(candidate);
                    HashIntSet toRemove = HashIntSets.newMutableSet(newMap.getOrDefault(node, HashIntSets.newMutableSet()));
                    newCand.removeAll(toRemove);
                    newMap.values().forEach(edgeList -> edgeList.removeAll(toRemove));
                    newMap.remove(node);
                    if (newCand.size() > 0 && !candsData.containsKey(newCand)) {
                        HashObjSet<HashIntSet> currSet = queue.getOrDefault(size - 1, HashObjSets.newMutableSet());
                        currSet.add(newCand);
                        queue.put(size - 1, currSet);
                        Pair<HashIntSet, Integer> p = getKEdgeSnaps(newCand, numEdgesPerSnap);
                        candsData.put(newCand, new MetaData<HashIntSet>(newMap, p.getA()));
                    }
                }
            }
            queue.remove(size);
            size--;
        }
        return HashObjSets.newMutableSet();
    }
    
    public HashIntObjMap<Pair<HashIntSet, Double>> generateExplodeViewWithDensity(Subgraph subgraph, Integer edgesPerSnapshot) {
        HashIntObjMap views = HashIntObjMaps.newMutableMap();
        HashIntSet edgeSet = HashIntSets.newMutableSet();
        subgraph.edges.stream().forEach(edge -> edgeSet.add(getEdgeId(edge.source, edge.target)));
        Pair<HashIntSet, Integer> p = getKEdgeSnaps(edgeSet, edgesPerSnapshot);
        for (int snap : p.getA()) {
            views.put(snap, retainInSnapWithDensity(edgeSet, snap));
        }
        return views;
    }
    
    private Pair<HashIntSet, Double> retainInSnapWithDensity(HashIntSet edgeSet, int snap) {
        HashIntSet output = HashIntSets.newMutableSet();
        edgeSet.stream()
                .filter(e -> getEdge(e).existsInT(snap))
                .forEach(e -> output.add(e));
        int numNodes = getNodesOf(output).size();
        return new Pair<HashIntSet, Double>(output, 2. * output.size() / numNodes);
    }
    
    public void printComponentLabels(HashIntSet comp) {
        StringBuilder builder = new StringBuilder().append("{ ");
        comp.stream().forEach((e) -> {
            Node src = nodes[edges.get(e).getSrc()];
            Node dst = nodes[edges.get(e).getDst()];
            builder.append("[")
                    .append(src.getNodeLabel() + " ++ ")
                    .append(edges.get(e).getEdgeLabel() + " ++ ")
                    .append(dst.getNodeLabel())
                    .append("] ");
        });
        builder.append("}");
        System.out.println(builder.toString());
    }
}
