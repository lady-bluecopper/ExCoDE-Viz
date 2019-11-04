package eu.unitn.disi.db.dence.graph;

import com.google.common.collect.Lists;
import com.koloboke.collect.map.hash.HashIntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import eu.unitn.disi.db.dence.utils.Pair;
import eu.unitn.disi.db.dence.utils.Utilities;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.stream.IntStream;

/**
 *
 * @author bluecopper
 */
public class GPCorrelationGraph {
    
    HashIntSet nodes;
    HashIntObjMap<HashIntSet> adj;
    
    public GPCorrelationGraph(int numNodes, Collection<Pair<Integer, Integer>> correlatedEdges) {
        nodes = HashIntSets.newMutableSet();
        adj = HashIntObjMaps.newMutableMap();
        correlatedEdges.stream().forEach(e -> {
            nodes.add(e.getA());
            nodes.add(e.getB());
            HashIntSet src = adj.getOrDefault(e.getA(), HashIntSets.newMutableSet());
            src.add(e.getB());
            adj.put(e.getA(), src);
            HashIntSet dst = adj.getOrDefault(e.getB(), HashIntSets.newMutableSet());
            dst.add(e.getA());
            adj.put(e.getB(), dst);
        });
    }
    
    public HashIntSet getNeighbors(int index) {
        return adj.get(index);
    }
    
    public HashIntSet getNodes() {
        return nodes;
    }
    
    public int getDegreeInSet(int n, HashIntSet s) {
        return Utilities.intersectionSize(adj.get(n), s);
    }
    
    public double getLargeness(int n, HashIntSet s) {
        return ((double) getDegreeInSet(n, s)) / s.size();
    }
    
    public int getLargestNodeInSet(HashIntSet s) {
        int node = -1;
        double largeness = -1;
        for (int n : s) {
            double currLarg = getLargeness(n, s);
            if (currLarg > largeness) {
                largeness = currLarg;
                node = n;
            }
        }
        return node;
    }
    
    public int getDegree(int n) {
        return adj.get(n).size();
    }
    
    public HashIntSet getNeighborsInSet(int n, HashIntSet s) {
        if (s.isEmpty() || adj.get(n).isEmpty()) {
            return HashIntSets.newMutableSet();
        }
        HashIntSet neighbors = HashIntSets.newMutableSet(adj.get(n));
        neighbors.retainAll(s);
        return neighbors;
    }
    
    public HashIntSet getNotNeighborsInSet(int n, HashIntSet s) {
        if (s.isEmpty()) {
            return HashIntSets.newMutableSet(adj.get(n));
        }
        if (adj.get(n).isEmpty()) {
            return HashIntSets.newMutableSet();
        }
        HashIntSet neighbors = HashIntSets.newMutableSet(s);
        neighbors.removeAll(adj.get(n));
        return neighbors;
    }
    
    public List<HashIntSet> findCCs() {
        HashIntSet visited = HashIntSets.newMutableSet();
        List<HashIntSet> ccs = Lists.newArrayList();
        nodes.stream().forEach(n -> {
            if (!visited.contains(n)) {
                ccs.add(bfs(n, visited));
            }
        });
        return ccs;
    }
    
    private HashIntSet bfs(int n, HashIntSet visited) {
        Queue<Integer> queue = Lists.newLinkedList();
        HashIntSet cc = HashIntSets.newMutableSet();
        
        queue.add(n);
        visited.add(n);
        while(!queue.isEmpty()) {
            int v = queue.poll();
            cc.add(v);
            adj.getOrDefault(v, HashIntSets.newMutableSet()).stream().forEach(neigh -> {
                if (!visited.contains(neigh)) {
                    queue.add(neigh);
                    visited.add(neigh);
                }
            });
        }
        return cc;
    }
    
    public boolean checkResult(HashIntSet clique) {
        return clique.parallelStream().noneMatch((node) -> (getDegreeInSet(node, clique) != clique.size() - 1));
    }
    
}
