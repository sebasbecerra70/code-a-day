# 2D prefix sums (range sum queries and difference arrays)

**Problem:** Given an immutable grid, answer "sum of the sub-rectangle (r1, c1)..(r2, c2)" in O(1) after preprocessing. Also support the inverse problem: apply many "add v to every cell of a rectangle" updates in O(1) each and rebuild the final grid once.

## Approach
- Build `P[r][c]` = sum of the top-left `r × c` block, with an extra zero row and column so no index goes negative:
  `P[r+1][c+1] = g[r][c] + P[r][c+1] + P[r+1][c] − P[r][c]` (the overlap is added twice, so subtract it once).
- **Query** with inclusion-exclusion:
  `sum = P[r2+1][c2+1] − P[r1][c2+1] − P[r2+1][c1] + P[r1][c1]`.
- **Difference array:** a rectangle add touches four corners: `+v` at (r1, c1), `−v` at (r1, c2+1) and (r2+1, c1), `+v` at (r2+1, c2+1). A 2D prefix sum over these marks spreads `v` over exactly the rectangle.
- Max k×k square sum: slide over all top-left corners, each an O(1) query.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| build PrefixSum2D | O(R·C) | O(R·C) |
| sum query | O(1) | O(1) |
| RectAdder::add | O(1) | — |
| RectAdder::build | O(R·C) | O(R·C) |
| maxSquareSum | O(R·C) | O(R·C) |

## Interview talking points
- Prefix sums and difference arrays are inverses: one turns point values into range sums, the other turns range updates into point marks.
- Padding with a zero row/column is cleaner than special-casing `r == 0` or `c == 0`.
- If the grid changes between queries, switch to a 2D Fenwick tree: O(log R · log C) for both update and query.
- Related problems: count submatrices that sum to a target (prefix sums + hash map over row pairs, O(R²·C)), image integral images (used in Viola-Jones face detection).
- Use `long long`: sums of many `int`s overflow fast.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
