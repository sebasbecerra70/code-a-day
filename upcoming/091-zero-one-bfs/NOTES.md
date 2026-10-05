# 0-1 BFS (shortest paths with 0/1 edge weights)

**Problem:** Find single-source shortest paths in a graph whose edge weights are all 0 or 1, faster than Dijkstra. Application: on a grid of free cells and walls, find the minimum number of walls you must break to get from the top-left to the bottom-right corner.

## Approach
- Use a **deque** instead of a priority queue. When relaxing edge `u → v` improves `dist[v]`:
  - weight 0: `push_front(v)` (same distance as `u`, so it belongs at the front),
  - weight 1: `push_back(v)` (one more than `u`).
- Invariant: the deque only ever holds vertices with distance `d` or `d + 1`, in non-decreasing order. Popping the front therefore always gives a minimum-distance vertex, just like Dijkstra's heap, but each operation is O(1).
- A vertex may be pushed more than once; a stale copy just finds no improving edges.
- **Grid walls:** cells are vertices; moving into a wall costs 1 and into a free cell costs 0. Add 1 if the start itself is a wall.

## Complexity
| Algorithm | Time | Space |
|-----------|------|-------|
| 0-1 BFS | O(V + E) | O(V) |
| Dijkstra (binary heap), for comparison | O((V + E) log V) | O(V) |
| grid walls (R × C) | O(R·C) | O(R·C) |

## Interview talking points
- Plain BFS works only when all weights are equal; 0-1 BFS is the minimal extension.
- Generalization: Dial's algorithm uses buckets for small integer weights 0..k in O(V·k + E).
- Typical problems: minimum cost to make a grid have a valid path (LeetCode 1368), minimum obstacle removal (2290), reversing edges to reach a node (cost 1 per reversed edge).
- Correctness argument is the same as Dijkstra's: vertices leave the front in non-decreasing distance order.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
