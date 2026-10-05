# Longest common subsequence (DP + reconstruction)

**Problem:** Given two strings, find the length of their longest common subsequence (characters in order, not necessarily contiguous), and one actual LCS. Derive related metrics such as the shortest common supersequence and the insert/delete edit distance.

## Approach
- **State:** `dp[i][j]` is the LCS length of the prefixes `a[0..i)` and `b[0..j)`.
- **Transition:** if `a[i-1] == b[j-1]`, then `dp[i][j] = dp[i-1][j-1] + 1`; otherwise `max(dp[i-1][j], dp[i][j-1])`.
- **Reconstruction:** walk back from `(n, m)`. On a match, take the character and move diagonally; otherwise move toward the larger neighbor. Reverse at the end.
- **Length only:** keep two rows, swapping so the shorter string sets the row width. That uses O(min(n, m)) memory.
- **Derived:** SCS length = `n + m − LCS`; insert/delete distance = `n + m − 2·LCS`.
- Tests compare against brute-force enumeration of subsequences and check that the reconstructed string really is a common subsequence.

## Complexity
| Variant | Time | Space |
|---------|------|-------|
| Length (two rows) | O(n·m) | O(min(n, m)) |
| Full table + reconstruction | O(n·m) | O(n·m) |
| Hirschberg (reconstruction in linear space) | O(n·m) | O(n + m) |

## Interview talking points
- LCS is the basis of `diff` (lines instead of characters) and of DNA sequence alignment.
- Why is the greedy "take the first match" wrong? Matching early can block a longer match later, e.g. `a = "ab"`, `b = "ba…"`.
- Hirschberg's algorithm reconstructs the LCS in linear space with divide and conquer, running the two-row DP forward and backward to find the middle split.
- LCS of two permutations reduces to LIS in O(n log n) (map each element of `b` to its index in `a`), and Hunt–Szymanski does well when matches are sparse.
- Compare with edit distance: LCS allows only insert and delete, and edit distance adds substitution.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
