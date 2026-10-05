# Maximum flow (Edmonds-Karp) and minimum cut

**Problem:** Given a directed graph with edge capacities, a source `s` and a sink `t`, find the maximum amount of flow that can be sent from `s` to `t`. Also return a minimum `s-t` cut: the cheapest set of edges whose removal disconnects `t` from `s`.

## Approach
- **Residual graph:** every edge `u → v` with capacity `c` gets a partner `v → u` with capacity 0. Sending `f` along `u → v` lowers its residual capacity by `f` and raises the partner's by `f`. The partner lets a later path *undo* earlier flow, which is what makes greedy augmentation correct.
- **Ford-Fulkerson:** while there's a path from `s` to `t` with positive residual capacity, push the path's bottleneck capacity along it.
- **Edmonds-Karp:** find each augmenting path with **BFS**, so it's always a shortest path. That bounds the number of augmentations by O(V·E), independent of capacities.
- **Min cut:** after the flow is maximal, the vertices reachable from `s` in the residual graph form one side `S`. Every original edge from `S` to the rest is saturated, and their capacities sum to the max flow.

## Complexity
| Step | Time | Space |
|------|------|-------|
| one BFS augmentation | O(V + E) | O(V) |
| Edmonds-Karp total | O(V · E²) | O(V + E) |
| min cut extraction | O(V + E) | O(V) |

## Interview talking points
- **Max-flow min-cut theorem:** the maximum flow value equals the minimum cut capacity. The test suite checks it by brute force over all partitions.
- With DFS instead of BFS, Ford-Fulkerson can take O(E · maxflow) steps, and may not terminate with irrational capacities.
- Dinic's algorithm (BFS level graph + blocking flows with DFS) runs in O(V²E), and O(E√V) on unit-capacity bipartite graphs.
- Reductions: bipartite matching (unit capacities), edge-disjoint paths, project selection, image segmentation, baseball elimination.
- Undirected edges: add both directions with capacity `c` each.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
