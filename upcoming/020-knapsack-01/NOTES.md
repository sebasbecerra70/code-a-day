# 0/1 knapsack (DP with item reconstruction)

**Problem:** Given items with integer weights and values and a capacity `W`, choose a subset (each item at most once) with total weight ≤ `W` and maximum total value. Also report which items.

## Approach
- State: `dp[i][c]` = best value using the first `i` items with capacity `c`.
- Transition: skip item `i` (`dp[i-1][c]`) or take it if it fits (`dp[i-1][c-w] + v`).
- **Value only:** collapse to a 1-D array and loop `c` *downward*, so `dp[c - w]` still refers to the previous row (each item counted once). Looping upward would solve the *unbounded* knapsack instead.
- **Reconstruction:** keep the 2-D table and walk back from `dp[n][W]`; whenever `dp[i][c] != dp[i-1][c]`, item `i-1` was taken.

## Complexity
| Version | Time | Space |
|---------|------|-------|
| value only | O(n·W) | O(W) |
| with items | O(n·W) | O(n·W) |

## Interview talking points
- O(n·W) is *pseudo-polynomial*: it's polynomial in the numeric value of W, not its bit length. 0/1 knapsack is NP-hard in general.
- Greedy by value/weight ratio is optimal only for the *fractional* knapsack.
- Upward vs. downward inner loop is the classic 0/1 vs. unbounded distinction.
- When W is huge but values are small, flip the DP: min weight to reach each value.
- Meet-in-the-middle handles n ≈ 40 with large weights in O(2^{n/2} n).

## Run
From this folder: `python -m pytest -q`
