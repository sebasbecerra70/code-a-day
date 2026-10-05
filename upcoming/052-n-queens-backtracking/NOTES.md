# N-Queens (backtracking with bitmasks)

**Problem:** Place `n` queens on an `n × n` chessboard so that no two share a row, column, or diagonal. Return all boards (LeetCode 51) and count them fast (LeetCode 52).

## Approach
- Place exactly one queen per row, so rows never conflict. For each row try each column.
- Track attacked columns and both diagonal families in sets: `r - c` is constant on a `\` diagonal and `r + c` on a `/` diagonal. A square is safe if none of the three contain it. That's an O(1) check.
- **Backtracking:** choose, recurse into the next row, then undo (remove from the sets) before trying the next column.
- **Counting with bitmasks:** keep three ints for the columns and diagonals attacked in the *current* row. `free = full & ~(cols | diag | anti)`; iterate set bits with `free & -free`. When moving to the next row, shift the diagonal masks left/right by one. Much faster than sets.

## Complexity
| Version | Time | Space |
|---------|------|-------|
| backtracking | O(n!) upper bound (heavily pruned in practice) | O(n) recursion + output |
| bitmask count | same bound, far smaller constants | O(n) |

## Interview talking points
- Why one queen per row? It turns the search into permutations and removes a whole class of conflicts for free.
- Symmetry: mirror the first row's choices to halve the work (only try columns `< n/2`, double, and handle the middle for odd `n`).
- No closed-form count is known; values grow roughly like (0.143n)^n.
- A single solution exists for all `n ≥ 4` and can be constructed directly in O(n), or found with min-conflicts local search for huge `n`.
- The choose/explore/unchoose template generalizes to Sudoku, permutations, subsets, and word search.

## Run
From this folder: `python -m pytest -q`
