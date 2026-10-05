# Number of islands (BFS flood fill + online union-find)

**Problem:** Given a grid of `'1'` (land) and `'0'` (water), count the 4-connected islands and report their sizes. Follow-up: land is added one cell at a time, and you report the count after each addition.

## Approach
- **Static grid, BFS flood fill:** scan every cell. Each unvisited land cell starts a BFS that marks its whole island, and the number of BFS starts is the island count. Cells are marked **when enqueued**, so none is queued twice. A separate `seen` array keeps the input unmodified.
- An explicit queue instead of recursion: a 1000×1000 all-land grid would overflow the stack with recursive DFS. The tests include this case.
- **Dynamic version (union-find):** each new land cell starts as its own island (`count++`). Every successful union with a neighboring land cell merges two islands (`count--`). Path halving plus union by rank keeps each step nearly constant.
- Tests compare the dynamic count after *every* addition with a recursive DFS on a copy of the grid.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| Static count / sizes | O(R·C) | O(R·C) visited, plus the queue |
| addLand | O(α(R·C)) amortized | O(R·C) total |
| k additions with recount each time | O(k·R·C) | — |

## Interview talking points
- Mutating the grid (sinking land) saves memory but surprises callers. Ask whether it's allowed, or use a visited array.
- BFS vs DFS: both are O(R·C). Recursive DFS is shortest to write but risks a stack overflow; BFS or an iterative DFS is safer.
- Why union-find for the online version? Re-running BFS after each addition costs O(R·C) per step. Union-find handles merges incrementally, but it **can't handle deletions** (land turning back to water). For that, process offline in reverse or use more advanced structures.
- Variants: 8-connectivity (add diagonals), counting distinct island shapes (normalize the BFS path or relative coordinates), closed islands (not touching the border), and max area.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
