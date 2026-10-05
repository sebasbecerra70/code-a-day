# Sudoku solver (backtracking + MRV)

**Problem:** Fill a 9×9 grid so every row, column, and 3×3 box contains 1–9 exactly once, given some pre-filled cells (LeetCode 37). Detect unsolvable puzzles.

## Approach
- Keep three arrays of 9-bit masks (`rows`, `cols`, `boxes`) marking digits already used. A cell's candidates are `ALL & ~(rows[r] | cols[c] | boxes[b])`, an O(1) computation.
- Validate the givens first; a duplicate means no solution.
- **Backtracking with MRV:** at each step pick the empty cell with the *fewest* candidates (minimum remaining values). Cells with 0 candidates fail immediately; cells with 1 are forced moves. This prunes the search dramatically compared with left-to-right order.
- Try each candidate bit (`cand & -cand`), set the masks, recurse, and undo on failure.
- The solver works on a copy, so the caller's grid is untouched.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | Exponential worst case (Sudoku generalizes to NP-complete), milliseconds in practice with MRV |
| Space | O(81) for masks, empties, and recursion |

## Interview talking points
- MRV ("fail-first") is the key heuristic from constraint satisfaction; add constraint propagation (naked singles, hidden singles) to solve most puzzles without guessing.
- Bitmasks vs. sets: same logic, much faster, and popcount gives candidate counts.
- Knuth's Algorithm X / Dancing Links models Sudoku as exact cover.
- Uniqueness check: keep searching after the first solution and stop at the second.
- Generalizes to n²×n² boards; that's where NP-completeness applies.

## Run
From this folder: `python -m pytest -q`
