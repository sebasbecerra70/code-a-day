# Dijkstra shortest path (binary min-heap)

**Problem:** Given a directed graph with non-negative edge weights and a source node, find the shortest distance from the source to every node and be able to reconstruct the actual paths.

## Approach
- Keep `dist[]` (best known distance) and `prev[]` (predecessor on the best path).
- A binary min-heap holds `(distance, node)` pairs. Repeatedly pop the closest unsettled node and *relax* its outgoing edges.
- Instead of a decrease-key operation, push a fresh entry whenever a distance improves and skip stale entries on pop (`d > dist[u]`). This "lazy deletion" keeps the heap simple.
- `pathTo` walks `prev[]` backward from the target and reverses.

## Complexity
| Aspect | Cost |
|--------|------|
| Time   | O((V + E) log V) |
| Space  | O(V + E) (heap can hold up to E entries) |

## Interview talking points
- Why non-negative weights? Once a node is popped its distance is final; a negative edge could later make it cheaper. Use Bellman-Ford for negative weights.
- Lazy deletion vs. an indexed heap with decrease-key: same asymptotics, far less code.
- With a Fibonacci heap the bound becomes O(E + V log V), but constants make it rarely worth it.
- Early exit: if you only need one target, stop when it's popped.
- A* is Dijkstra plus an admissible heuristic added to the priority.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
