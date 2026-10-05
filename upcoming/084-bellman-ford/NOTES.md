# Bellman-Ford (negative weights and cycle detection)

**Problem:** Find shortest paths from a source in a directed graph that may have **negative** edge weights, and detect (and report) negative cycles reachable from the source.

## Approach
- Start with `dist[source] = 0`, everything else ∞. Relax **every** edge `n - 1` times: `dist[v] = min(dist[v], dist[u] + w)`.
- Why `n - 1`? A shortest simple path has at most `n - 1` edges, and after pass `k` all shortest paths with ≤ `k` edges are correct.
- Stop early if a pass changes nothing.
- One extra pass: if any edge still relaxes, a reachable negative cycle exists. To recover it, walk `prev` back `n` steps from the relaxed node (guaranteed to land inside the cycle), then follow `prev` around until it repeats.
- **Arbitrage** application: exchange rates multiply, and `-log(rate)` turns "product > 1" into "sum < 0". A virtual source with 0-weight edges to every currency finds cycles anywhere.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(V·E) worst case, often much less with early exit |
| Space | O(V) |

## Interview talking points
- Dijkstra is faster (O(E log V)) but wrong with negative edges; Bellman-Ford trades speed for generality.
- SPFA (queue-based Bellman-Ford) is fast on average, same worst case.
- Distance-vector routing (RIP) is distributed Bellman-Ford; "count to infinity" is its failure mode.
- Johnson's algorithm runs Bellman-Ford once to reweight edges, then Dijkstra from every node: all-pairs in O(VE log V) on sparse graphs.
- "Cheapest flights within K stops" is Bellman-Ford limited to K+1 passes (copy `dist` per pass).

## Run
From this folder: `python -m pytest -q`
