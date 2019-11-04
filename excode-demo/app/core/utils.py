import io
import os
from core.app import app


def read_graph(graph_f):
    nodeDegs = {}
    nodeDegXS = {}
    edgeApps = {}
    edgesXS = {}
    N = 0
    E = 0
    T = 0

    with graph_f:
        line_iterator = iter(graph_f)
        for line in line_iterator:
            line = line.rstrip()
            E += 1
            lst = line.split(" ")
            src = int(lst[0])
            dst = int(lst[1])
            i_s = nodeDegs.get(src, 0)
            i_d = nodeDegs.get(dst, 0)
            nodeDegs[src] = i_s + 1
            nodeDegs[dst] = i_d + 1

            stamps = lst[2].split(",")
            T = len(stamps)
            c = 0
            for t in range(T):
                if float(stamps[t]) > 0:
                    c += 1
                    edgesXS[t] = edgesXS.get(t, 0) + 1
                    srcDegMap = nodeDegXS.get(src, {})
                    srcDegMap[t] = srcDegMap.get(t, 0) + 1
                    nodeDegXS[src] = srcDegMap
                    dstDegMap = nodeDegXS.get(dst, {})
                    dstDegMap[t] = dstDegMap.get(t, 0) + 1
                    nodeDegXS[dst] = dstDegMap
            edgeApps[E] = c
        N = len(nodeDegs)

        return (nodeDegs, edgeApps, edgesXS, nodeDegXS, N, E, T)


def compute_stats(graph):
    filename = os.path.basename(graph)
    graph_f = io.open(graph)
    (nD, eA, eS, sD, N, E, T) = read_graph(graph_f)
    # TABLE
    avg_d = 0
    max_d = 0
    min_d = N
    l_d = len(nD.keys())
    for i in nD.values():
        avg_d += i
        max_d = max(max_d, i)
        min_d = min(min_d, i)
    avg_d /= l_d
    with io.open(os.path.join(app.config['UPLOAD_FOLDER'], filename + "_general_stats.csv"), "w") as out_f:
        out_f.write("Nodes,Edges,Snapshots,Min Deg,Avg Deg,Max Deg\n")
        out_f.write("{},{},{},{},{},{}".format(N, E, T, min_d, round(avg_d, 3), max_d))
    # SNAP TABLE
    avg_s = 0
    max_s = 0
    min_s = T
    for t in eA.values():
        avg_s += t
        max_s = max(max_s, t)
        min_s = min(min_s, t)
    avg_s /= len(eA.keys())

    avg_e = 0
    max_e = 0
    min_e = E
    for t in eS.values():
        avg_e += t
        max_e = max(max_e, t)
        min_e = min(min_e, t)
    avg_e /= len(eS.keys())

    with io.open(os.path.join(app.config['UPLOAD_FOLDER'], filename + "_snap_stats.csv"), "w") as out_f:
        out_f.write(",Min,Avg,Max\n")
        out_f.write("edges per snapshot,{},{},{}\n".format(min_e, round(avg_e, 3), max_e))
        out_f.write("snapshots per edge,{},{},{}".format(min_s, round(avg_s, 3), max_s))
    # DEG BAR CHART
    degNodes = {}
    for n, d in nD.items():
        degNodes[d] = degNodes.get(d, 0) + 1
    with io.open(os.path.join(app.config['UPLOAD_FOLDER'], filename + "_degrees.csv"), "w") as out_f:
        out_f.write("Degree,Count\n")
        degrees = sorted(degNodes.keys())
        for d in degrees:
            out_f.write("{},{}\n".format(d, degNodes[d]))
    # MIN,AVG,MAX DEG PER SNAP MULTI CHART
    minDegXS = [E] * T
    avgDegXS = [0] * T
    maxDegXS = [0] * T
    for degMap in sD.values():
        for t in range(T):
            minDegXS[t] = min(minDegXS[t], degMap.get(t, 0))
            maxDegXS[t] = max(maxDegXS[t], degMap.get(t, 0))
            avgDegXS[t] = avgDegXS[t] + degMap.get(t, 0)
    for t in range(T):
        avgDegXS[t] /= len(sD.keys())

    with io.open(os.path.join(app.config['UPLOAD_FOLDER'], filename + "_degrees_per_snapshot.csv"), "w") as out_f:
        out_f.write("Snapshot,Max,Avg,Min\n")
        for t in range(T):
            out_f.write("{},{},{},{}\n".format(t, maxDegXS[t], round(avgDegXS[t], 3), minDegXS[t]))
    # SNAP PER EDGE BAR CHART
    snapEdges = {}
    for e, c in eA.items():
        snapEdges[c] = snapEdges.get(c, 0) + 1
    with io.open(os.path.join(app.config['UPLOAD_FOLDER'], filename + "_num_snap_count_edges.csv"), "w") as out_f:
        out_f.write("Existence,Count\n")
        existence = sorted(snapEdges.keys())
        for e in existence:
            out_f.write("{},{}\n".format(e, snapEdges[e]))
    # EDGES PER SNAP LINE CHART
    with io.open(os.path.join(app.config['UPLOAD_FOLDER'], filename + "_edges_per_snapshot.csv"), "w") as out_f:
        out_f.write("Snapshot,Count\n")
        for t in range(T):
            out_f.write("{},{}\n".format(t, eS.get(t, 0)))


def write_results_on_disk(name, string):
    with io.open(os.path.join(app.config['UPLOAD_FOLDER'], name), "w") as out_f:
        out_f.write(string)


def create_JSON_graph_string(dataset):
    path = os.path.join(app.config['UPLOAD_FOLDER'], dataset)
    JSONGraph = {}
    JSONNodes = []
    JSONEdges = []
    with io.open(path) as dataset_f:
        nodes = set()
        for line in dataset_f.readlines():
            lst = line.split(" ")
            nodes.add(lst[0])
            nodes.add(lst[1])
            edge = {}
            edge['source'] = lst[0]
            edge['target'] = lst[1]
            JSONEdges.append(edge)
        JSONGraph['edges'] = JSONEdges
        for n in range(len(nodes)):
            node = {}
            node['name'] = n
            JSONNodes.append(node)
        JSONGraph['nodes'] = JSONNodes
    return JSONGraph


def get_graph_size(dataset):
    path = os.path.join(app.config['UPLOAD_FOLDER'], dataset)
    with open(path) as f:
        return sum(1 for _ in f)
