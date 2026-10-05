# A* pathfinding on a grid

**Problem:** Find the cheapest path between two cells on a grid with walls, exploring as few cells as possible. Support 4-way moves (cost 1) and 8-way moves (diagonals cost √2).

## Approach
- A* is Dijkstra with a priority of `f = g + h`: `g` is the cost so far, `h` an estimate of the remaining cost.
- With an **admissible** heuristic (never overestimates), the first time the goal is popped its cost is optimal. Manhattan distance for 4-way; **octile** distance (`max + (√2−1)·min`) for 8-way.
- Equal-`f` ties are broken toward smaller `h`, which heads straight for the goal on open maps.
- Lazy deletion: push duplicates on improvement and skip already-closed nodes when popped.
- Diagonal moves are disallowed when either adjacent orthogonal cell is a wall (no corner cutting).
- Flat typed arrays indexed by `r * cols + c` hold `g`, parent pointers, and the closed set.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(V log V) worst case (V = cells); far fewer expansions with a good heuristic |
| Space | O(V) |

## Interview talking points
- `h = 0` gives Dijkstra; an inadmissible `h` (e.g. weighted A*, `g + w·h`) trades optimality for speed.
- Consistent (monotone) heuristics mean each node is expanded at most once; Manhattan and octile are consistent on these grids.
- Variants: bidirectional A*, Jump Point Search (uniform grids), IDA* (low memory), D* Lite (changing maps).
- In games, precomputed navmeshes or hierarchical pathfinding (HPA*) beat raw grids at scale.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
