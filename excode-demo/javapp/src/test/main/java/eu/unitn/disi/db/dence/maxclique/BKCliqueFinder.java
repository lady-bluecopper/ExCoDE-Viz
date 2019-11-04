package eu.unitn.disi.db.dence.maxclique;

import com.google.common.collect.Lists;
import com.koloboke.collect.IntCursor;
import com.koloboke.collect.set.hash.HashIntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import eu.unitn.disi.db.dence.graph.GPCorrelationGraph;
import eu.unitn.disi.db.dence.utils.Utilities;
import java.util.List;

/**
 *
 * @author bluecopper
 */
public class BKCliqueFinder {

    GPCorrelationGraph graph;
    List<HashIntSet> cliques;
    
    public BKCliqueFinder(GPCorrelationGraph graph) {
        this.graph = graph;
        this.cliques = Lists.newArrayList();
    }
    
    private void BronKerbosch(HashIntSet potentialClique, HashIntSet candidates, HashIntSet alreadyFound) {
        if (isAnyConnectedToAllCandidates(alreadyFound, candidates)) {
            return;
        }
        IntCursor it = candidates.cursor();
        while (it.moveNext()) {
            Integer candidate = it.elem();
            it.remove();
            // create newCandidates and newAlreadyFound 
            HashIntSet neis = graph.getNeighbors(candidate);
            HashIntSet newCandidates = HashIntSets.newMutableSet(candidates);
            newCandidates.retainAll(neis);
            HashIntSet newAlreadyFound = HashIntSets.newMutableSet(alreadyFound);
            newAlreadyFound.retainAll(neis);
            // move candidate node to potentialClique 
            potentialClique.add(candidate);
            if (newCandidates.isEmpty() && newAlreadyFound.isEmpty()) {
                // this is a maximal clique 
                cliques.add(HashIntSets.newMutableSet(potentialClique));
            } else {
                // recursive call 
                BronKerbosch(potentialClique, newCandidates, newAlreadyFound);
            }
            alreadyFound.add(candidate);
            potentialClique.remove(candidate);
        }
    }
    
    public List<HashIntSet> findMaxCliques() {
        long start = System.currentTimeMillis();
        HashIntSet potentialClique = HashIntSets.newMutableSet();
        HashIntSet candidates = HashIntSets.newMutableSet();
        HashIntSet alreadyFound = HashIntSets.newMutableSet();

        candidates.addAll(graph.getNodes());
        BronKerbosch(potentialClique, candidates, alreadyFound);
        System.out.println(".......................Found " + cliques.size() + " Cliques in (ms) " + (System.currentTimeMillis() - start));
        return cliques;
    }
    
    protected boolean isAnyConnectedToAllCandidates(HashIntSet nodes, HashIntSet candidates) {
        return nodes.parallelStream()
                .filter(node -> graph.getNeighbors(node).size() >= candidates.size())
                .anyMatch(node -> Utilities.intersectionSize(graph.getNeighbors(node), candidates) == candidates.size());
    }

}   