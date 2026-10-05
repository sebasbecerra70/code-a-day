# Median of two sorted arrays (binary search on partition)

**Problem:** Given two sorted arrays of sizes `m` and `n`, return the median of their union in O(log(m + n)) time, without merging. Also find the k-th smallest element of the union.

## Approach
- Cut `a` after `i` elements and `b` after `j = half − i` elements, where `half = ⌈(m+n)/2⌉`. The cut is correct when `a[i−1] ≤ b[j]` and `b[j−1] ≤ a[i]`: everything on the left is ≤ everything on the right.
- **Binary search `i` over the shorter array.** If `a[i−1] > b[j]`, we took too many from `a`, so move left; if `b[j−1] > a[i]`, move right.
- Missing neighbors (cut at an end) are treated as −∞ / +∞, stored in `long long` so they never tie with real `INT_MIN`/`INT_MAX` values.
- Median: the max of the left side if the total is odd, else the average of max-left and min-right.
- **k-th smallest:** compare the `k/2`-th remaining element of each array. The smaller one and everything before it are among the first `k − 1`, so discard them and reduce `k`.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| findMedianSortedArrays | O(log min(m, n)) | O(1) |
| kthSmallest | O(log k) | O(1) |
| merge baseline (for comparison) | O(m + n) | O(1) to O(m + n) |

## Interview talking points
- Search the **shorter** array: then `j = half − i` is always within `[0, n]`, and it's faster.
- Using `(m + n + 1) / 2` for the left size handles odd and even totals uniformly.
- Average as `double` to avoid integer overflow on `a + b` and integer-division truncation.
- The k-th approach generalizes (e.g. percentiles); the partition approach is the one interviewers usually expect for the median.
- State the merge solution first (O(m+n)), then improve it.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
