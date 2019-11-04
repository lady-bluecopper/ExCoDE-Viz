package eu.unitn.disi.db.dence.maxclique;

import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import eu.unitn.disi.db.dence.graph.GPCorrelationGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 *
 * @author bluecopper
 */
public class MultiThreadCliqueFinder {

    private final GPCorrelationGraph graph;
    private final int maxSize;
    private final ForkJoinPool pool;
    private final List<HashIntSet> cliques;

    public MultiThreadCliqueFinder(GPCorrelationGraph graph, int maxSize) {
        this.graph = graph;
        this.maxSize = maxSize;
        this.pool = ForkJoinPool.commonPool();
        this.cliques = new ArrayList<HashIntSet>();
    }

    public List<HashIntSet> findMaxCliques() {
        // Initialize data structures
        HashIntSet anchor = HashIntSets.newMutableSet();
        HashIntSet not = HashIntSets.newMutableSet();
        HashIntSet cand = HashIntSets.newMutableSet(graph.getNodes());
        DegreeMap degreeMap = new DegreeMap(graph, cand);

        pool.invoke(new ExplorationTask(anchor, cand, not, degreeMap));
        pool.shutdownNow();
        return cliques;
    }

    private class ExplorationTask extends RecursiveTask {

        private HashIntSet anchor;
        private HashIntSet cand;
        private HashIntSet not;
        private DegreeMap degreeMap;

        public ExplorationTask(
                HashIntSet anchor,
                HashIntSet cand,
                HashIntSet not,
                DegreeMap degreeMap) {

            this.anchor = anchor;
            this.cand = cand;
            this.not = not;
            this.degreeMap = degreeMap;
        }

        private void enumerateClique(HashIntSet anchor, HashIntSet cand, HashIntSet not, DegreeMap degreeMap) {
            if (isAClique(cand, degreeMap)) {
                HashIntSet clique = HashIntSets.newMutableSet(anchor);
                clique.addAll(cand);
                cliques.add(clique);
                return;
            }
            while (!isAClique(cand, degreeMap)) {
                int next = degreeMap.getLowDegNode();
                HashIntSet nextCand = HashIntSets.newMutableSet(cand);
                nextCand.retainAll(graph.getNeighbors(next));
                HashIntSet nextNot = HashIntSets.newMutableSet(not);
                nextNot.retainAll(graph.getNeighbors(next));
                boolean doRecursion = true;
                for (int u : nextNot) {
                    if (graph.getDegreeInSet(u, nextCand) == nextCand.size()) {
                        doRecursion = false;
                        break;
                    }
                }
                if (doRecursion) {
                    HashIntSet nextAnchor = HashIntSets.newMutableSet(anchor);
                    nextAnchor.add(next);
                    DegreeMap nextDegreeMap = new DegreeMap(graph, nextCand);
                    if (nextCand.size() > maxSize) {
                        pool.invoke(createTask(nextAnchor, nextCand, nextNot, nextDegreeMap));
                    } else {
                        enumerateClique(nextAnchor, nextCand, nextNot, nextDegreeMap);
                    }
                }
                cand.remove(next);
                not.add(next);
                HashIntSet neighInCand = graph.getNeighborsInSet(next, cand);
                degreeMap.updateMap(neighInCand);
            }
            boolean isMaximal = true;
            for (int u : not) {
                if (graph.getDegreeInSet(u, cand) == cand.size()) {
                    isMaximal = false;
                    break;
                }
            }
            if (isMaximal) {
                HashIntSet clique = HashIntSets.newMutableSet(anchor);
                clique.addAll(cand);
                cliques.add(clique);
            }
        }
        
        private boolean isAClique(HashIntSet nodes, DegreeMap degreeMap) {
            HashIntSet cands = degreeMap.getNodesWithDeg(nodes.size() - 1);
            if (cands.size() < nodes.size()) {
                return false;
            }
            return nodes.stream().noneMatch((n) -> (!cands.contains(n)));
        }

        protected Object compute() {
            enumerateClique(anchor, cand, not, degreeMap);
            return 0;
        }

        private ExplorationTask createTask(HashIntSet anchor, HashIntSet cand, HashIntSet not, DegreeMap degreeMap) {
            return new ExplorationTask(anchor, cand, not, degreeMap);
        }

    }

}
