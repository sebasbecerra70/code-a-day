# Kruskal's minimum spanning tree (union-find)

**Problem:** Given an undirected weighted graph, find a set of edges that connects all nodes with minimum total weight (a minimum spanning tree), or a minimum spanning forest if the graph is disconnected.

## Approach
- Sort edges by weight. Walk them in order and keep an edge iff its endpoints are in different components. A **union-find** answers "same component?" and merges components in near-constant time.
- Why it's correct (**cut property**): for any cut of the graph, the lightest crossing edge belongs to some MST. When Kruskal accepts an edge, it's the lightest edge crossing the cut between its endpoint's component and the rest.
- Stop early once `n - 1` edges are chosen.
- DSU uses path halving + union by size, so each operation is O(α(n)).
- Applications included:
  - **Single-linkage clustering:** stop when `k` components remain. The removed heaviest MST edges are the cluster gaps.
  - **Min cost to connect points** (Manhattan distance, complete graph).
- Tests cross-check against brute force over all (n-1)-edge subsets.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(E log E) (sorting dominates) |
| Space | O(V + E) |
| DSU op | O(α(V)) amortized |

## Interview talking points
- Kruskal vs Prim: Kruskal is edge-centric and suits sparse graphs or edges that arrive pre-sorted. Prim (heap) is O(E log V) and better on dense graphs. On complete graphs, O(V²) array-based Prim beats both.
- Negative weights are fine. Unlike shortest paths, MST only cares about relative order.
- If all weights are distinct, the MST is unique.
- Maximum spanning tree: sort descending (or negate the weights).
- Follow-ups: second-best MST, "critical and pseudo-critical edges" (LeetCode 1489), Borůvka's algorithm for parallel MST.

## Run
From this folder: `python -m pytest -q`
