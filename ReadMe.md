# ExCoDE

## Overview
ExCoDE is a general framework to mine diverse dense correlated subgraphs from dynamic networks. The correlation of a subgraph is computed in terms of the minimum pairwise Pearson correlation between its edges. The density of a subgraph is computed either as the minimum average degree among the snapshots of the networks where the subgraph is active, or as the average average degree among the snapshots of the networks where the subgraph is active. The similarity between different subgraphs is measured as the Jaccard similarity between the corresponding sets of edges.

## Usage
The framework can be tested at the following link: https://db.disi.unitn.eu/excode.

## Examples

### EX1
Select the *bgp.csv* dataset and click the *LOAD* button. Then insert the following configuration and click the *EXECUTE* button:

    Correlation Threshold: 0.9
    Density Function: Average 
    Density Threshold: 1.5
    Existing Edge Threshold: 15
    Size Threshold: Unbounded
    Similarity Threshold: 1

The *bgp.csv* dataset is a sequence of snapshots of the Internet network topology created with the routing tables used from August 29 to August 31, 2005. In these days, a catastrophic hurricane hit Florida and Louisiana, causing major issues also to the routing topology. The edges in each dense group found by the algorithm are routing paths that changed similarly over time, and are topologically close in the snapshots where the group is active. Therefore, they are likely to represent instability regions originated from the failure of the same router.
By selecting a node of one of the colored subgraph and clicking the *EXAMINE SUBGRAPH* button, a secondary interactive panel appears at the bottom of the page.
The colors used in the secondary interactive panel indicate the regional Internet address registries for the five geographical areas: Asia-pacific, North America, South America, Europe, and Africa. This panel allows us to see which countries were affected by the disaster, which issues were caused by the same root cause, and which routing paths were active in each snapshot of the network.

Note that by using a large correlation threshold we are able to detect only the most correlated routing paths. When increasing the correlation threshold, the number of pairs of correlated edges decreases, as well as their density. Therefore, we need to use a small density threshold. An unbounded size threshold means that we want to find every dense correlated group of edges, with no restriction on their size. A similarity threshold equal to 1 indicates that the groups of edges found do not overlap.
Finally, note that the existing edge threshold indicates the minimum percentage of edges of a group that must exist in a snapshot, in order to consider it in the computation of the aggregated density of the group. Therefore, by increasing the existing edge threshold, the number of snapshots considered is likely to be smaller, but the density of the group is likely to be larger, because the average node degree in the snapshots considered is larger. 

### EX2
Select the *ring.csv* dataset and click the *LOAD* button. Then insert the following configuration and click the *EXECUTE* button:

    Correlation Threshold: 0.8
    Density Function: Minimum 
    Density Threshold: 8
    Existing Edge Threshold: 1
    Size Threshold: Unbounded
    Similarity Threshold: 1
    
The *ring.csv* dataset is a connected graph obtained by building 10 cliques of size 10 and then rewiring one edge in each clique to a node in an adjacent clique. Edges in the same clique are associated with highly correlated existence strings. As a consequence, in the snapshots where a clique is active, almost all of its edges are present, and therefore, the minimum density is always greater than 8. Thus, the algorithm finds 10 groups of dense correlated edges. These groups are illustrated in the interactive panel using different colors: denser subgraphs are colored in darker colors, nodes belonging to multiple subgraphs are indicated in black, and nodes that are not part of any dense subgraph are in white. Further information on the dense subgraphs is provided in a separate hidden panel that can be revealed by clicking the dotted button. Interact with the graph by hovering over a node, clicking and dragging the nodes, and zooming in and out of the graph.

On the other hand, if we use the following configuration

    Correlation Threshold: 0.8
    Density Function: Minimum 
    Density Threshold: 8
    Existing Edge Threshold: 0
    Size Threshold: Unbounded
    Similarity Threshold: 1

all the snapshots are considered in the computation of the minimum density. Since the average degree of a clique is 0 in the snapshots where no edge of the clique exists, the minimum density of the clique is 0 as well. As a consequence, the algorithm finds no solution.

This example shows that the edge existing threshold is pivotal to account for situation where events that take place in the dynamic network exhibit themselves only in a small number of time instances.

Finally, select the *ring_uncor.csv* dataset and the following configuration:

    Correlation Threshold: 0.8
    Density Function: Minimum 
    Density Threshold: 8
    Existing Edge Threshold: 1
    Size Threshold: Unbounded
    Similarity Threshold: 1

The *ring_uncor.csv* dataset is a connected graph created with the same procedure used to create *ring.csv*, but in this case, all the edges are associated with mutually independent existence strings. As a consequence, by using large correlation thresholds, the algorithm finds no solution, even though several highly dense groups of edges are present in the network. This is due to the fact that a dense correlated subgraph must have not only spatial compactness, but also temporal similarity.

On the other hand, by using the following configuration

    Correlation Threshold: 0.3
    Density Function: Minimum 
    Density Threshold: 1
    Existing Edge Threshold: 1
    Size Threshold: Unbounded
    Similarity Threshold: 1

we loosen the temporal requirement, and thus we can find a solution.

Similar results can be obtained by using the *clustered.csv* and *clustered_uncor.csv* datasets, which are connected graphs obtained by partitioning a set of 100 nodes into *k* groups each of size drawn from a normal distribution N(10,0.2), and then adding intra-cluster edges with probability 0.3 and inter-cluster edges with probability 0.2. In the first graphs, edges in the same group are associated with highly correlated existence strings, while in the second graph all the edges are associated with mutually independent existence strings.