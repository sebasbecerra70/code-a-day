# Floyd-Warshall all-pairs shortest paths

**Problem:** Compute shortest distances between **every** pair of nodes in a weighted directed graph (negative edges allowed), reconstruct actual paths, and detect negative cycles.

## Approach
- Start with `dist[i][j]` = direct edge weight (cheapest parallel edge), `0` on the diagonal, and ∞ elsewhere.
- DP over allowed intermediate nodes: after round `k`, `dist[i][j]` is the shortest path whose intermediate nodes all come from `{0..k}`. The transition is `dist[i][j] = min(dist[i][j], dist[i][k] + dist[k][j])`. The 3D table collapses to 2D because row and column `k` don't change during round `k`.
- **Path reconstruction:** `nxt[i][j]` stores the first hop. When going through `k` improves `i -> j`, the first hop becomes `nxt[i][k]`. Walk `nxt` until you reach `j`.
- **Negative cycles:** after the run, any `dist[i][i] < 0` means node `i` sits on a negative cycle.
- Small speedups: skip rows where `dist[i][k]` is ∞, and hoist row lookups out of the inner loop.
- Bonus: **Warshall's transitive closure** (same triple loop over booleans) and **graph center** (min eccentricity).

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(V³) |
| Space | O(V²) |
| Path query | O(path length) |

## Interview talking points
- When to use it: dense graphs, small V (≤ ~500 in Python), or when you need all pairs anyway. For sparse graphs, V × Dijkstra is O(V·E log V). Johnson's algorithm handles negative edges by reweighting with Bellman-Ford first.
- Loop order matters: `k` **must** be the outermost loop. Putting it inside is a classic bug.
- It works with negative edges but not with negative cycles. Distances through a cycle are -∞, so propagate that if you need per-pair answers.
- The same skeleton works over other semirings: (min,+) for shortest paths, (or,and) for reachability, (max,min) for widest/bottleneck paths.
- Tests cross-check against Dijkstra from every source on random non-negative graphs.

## Run
From this folder: `python -m pytest -q`
