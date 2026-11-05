# Coin change (min coins and number of ways)

**Problem:** Given coin denominations (unlimited supply) and a target amount, find (1) the fewest coins that sum to it (LeetCode 322) and (2) the number of distinct combinations that sum to it (LeetCode 518).

## Approach
- **Min coins:** `dp[a]` = fewest coins for amount `a`. `dp[0] = 0`, and `dp[a] = 1 + min(dp[a - c])` over coins `c ≤ a`. Use `amount + 1` as "infinity". Track the last coin used per amount to reconstruct a combination.
- **Number of ways:** `ways[0] = 1`; for each coin (outer loop), for each amount from `c` upward, `ways[a] += ways[a - c]`. Putting coins in the outer loop counts combinations, not ordered sequences. Swapping the loops counts permutations (LeetCode 377).
- Duplicate denominations are deduplicated for counting.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| min_coins / min_coins_combo | O(amount · #coins) | O(amount) |
| count_ways | O(amount · #coins) | O(amount) |

## Interview talking points
- Greedy (take the biggest coin) works for "canonical" systems like US coins but fails for `[1, 3, 4]` with amount 6.
- Loop order decides combinations vs. permutations. This is a classic follow-up.
- Upward inner loop = unlimited coins (unbounded knapsack); downward = each coin at most once (0/1).
- BFS over amounts also finds min coins and can stop early.
- Pseudo-polynomial in `amount`.

## Run
From this folder: `python -m pytest -q`
