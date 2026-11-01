# Longest increasing subsequence (patience sorting)

**Problem:** Given an integer array, find the length of the longest strictly increasing subsequence (LeetCode 300), and return one such subsequence.

## Approach
- **O(n²) DP:** `dp[i]` = LIS ending at `i` = 1 + max `dp[j]` over `j < i` with `nums[j] < nums[i]`. Kept as a reference.
- **O(n log n) patience sorting:** maintain `tails`, where `tails[k]` is the smallest tail of any increasing subsequence of length `k+1`. `tails` is always sorted, so for each `x` binary-search the first tail `>= x` and replace it (or append). The answer is `len(tails)`.
- `bisect_left` gives *strictly* increasing; `bisect_right` would give non-decreasing.
- **Reconstruction:** also store which index sits at each `tails` slot and, for every element, a `parent` pointer to the index at slot `k-1` when it was placed. Walk parents back from the last slot.

## Complexity
| Method | Time | Space |
|--------|------|-------|
| DP | O(n²) | O(n) |
| Patience sorting | O(n log n) | O(n) |

## Interview talking points
- `tails` is **not** itself an LIS; it only has the right length. That's why reconstruction needs parent pointers.
- Strict vs. non-strict: swap `bisect_left` for `bisect_right`.
- Related problems: Russian doll envelopes (sort by width asc, height desc, then LIS on heights), box stacking, minimum deletions to make sorted (n - LIS).
- Dilworth's theorem: the minimum number of non-increasing sequences covering the array equals the LIS length (that's the number of patience piles).

## Run
From this folder: `python -m pytest -q`
