# Kadane's algorithm (maximum subarray sum)

**Problem:** Find the contiguous non-empty subarray with the largest sum, and report where it is. Variants: the array is circular (a subarray may wrap from the end to the start), and the maximum *product* subarray.

## Approach
- **Kadane:** `best_ending_here = max(a[i], best_ending_here + a[i])`. Equivalently, when the running sum drops to ≤ 0, restart at `i`, because a non-positive prefix can't help any later subarray. Track the start index when restarting to recover the range.
- **Circular:** a wrapping subarray is the whole array minus a middle block, so its best sum is `total − minSubarray`. Run Kadane for max and min together. If every element is negative, `total − min` would pick the empty subarray, so return the plain max.
- **Max product:** keep both the largest and smallest product ending at `i`. A negative number turns the smallest into the largest, so swap them before updating. Zeros reset both naturally.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| maxSubarray | O(n) | O(1) |
| maxSubarrayCircular | O(n) | O(1) |
| maxProductSubarray | O(n) | O(1) |

## Interview talking points
- Kadane is a DP with state "best sum of a subarray ending at i", compressed to one variable.
- Initialize `best` with `a[0]`, not 0, or all-negative inputs return 0 (unless empty subarrays are allowed).
- Divide and conquer also works in O(n log n), and its "combine" step leads to segment trees that answer max-subarray queries on ranges with updates.
- 2D extension: fix a pair of rows, compress columns to a 1D array, and run Kadane: O(R²·C).
- Use 64-bit sums and products to avoid overflow.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
