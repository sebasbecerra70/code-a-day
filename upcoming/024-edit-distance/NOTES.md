# Edit distance (Levenshtein) with alignment

**Problem:** Find the minimum number of single-character insertions, deletions, and substitutions needed to turn string `a` into string `b` (LeetCode 72), and produce one optimal sequence of edits.

## Approach
- `dp[i][j]` = distance between `a[:i]` and `b[:j]`. Base cases: `dp[i][0] = i`, `dp[0][j] = j`.
- `dp[i][j] = min(dp[i-1][j] + 1` (delete), `dp[i][j-1] + 1` (insert), `dp[i-1][j-1] + (a[i-1] != b[j-1])` (substitute/keep)`)`.
- **Distance only:** each row depends only on the previous one, so keep two rows, sized by the shorter string.
- **Edit script:** keep the full table and walk back from `dp[n][m]`, choosing any move consistent with the recurrence.
- `apply_script` replays the ops to verify them in tests.

## Complexity
| Version | Time | Space |
|---------|------|-------|
| distance | O(n·m) | O(min(n, m)) |
| with script | O(n·m) | O(n·m) |

## Interview talking points
- It's a metric (non-negative, symmetric, triangle inequality), which enables BK-trees for fuzzy search.
- Variants: Damerau (adds transposition), LCS distance (no substitution), weighted costs, Needleman-Wunsch/Smith-Waterman in bioinformatics.
- Hirschberg's algorithm recovers the alignment in O(min(n, m)) space via divide and conquer.
- Bounded check "is distance ≤ k?" only needs a diagonal band: O(k·n).
- Uses: spell checkers, diff tools, DNA alignment, fuzzy matching.

## Run
From this folder: `python -m pytest -q`
