# Monotonic stack (next greater element)

**Problem:** For each element of an array, find the first element to its right that is strictly greater (or −1). Variants: the array is circular; return the *distance* instead (daily temperatures); and the online stock span problem (how many consecutive days up to today had a price ≤ today's).

## Approach
- Scan left to right and keep a **stack of indices still waiting for an answer**. Their values are non-increasing from bottom to top.
- When `a[i]` arrives, it is the answer for every waiting index with a smaller value: pop them and record `a[i]`. Then push `i`.
- **Circular:** iterate `2n` times using `i % n`, but only push during the first pass. The second pass just resolves leftovers.
- **Stock span:** the stack stores `(price, span)` pairs. A new price absorbs (pops and adds) the spans of all earlier prices ≤ it.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| nextGreater / dailyTemperatures | O(n) | O(n) |
| nextGreaterCircular | O(n) | O(n) |
| StockSpanner::next | O(1) amortized | O(n) total |

## Interview talking points
- Why O(n) despite the nested loop? Each index is pushed once and popped at most once, so total work is at most 2n.
- Recognize the pattern: "nearest element to the left/right that is greater/smaller". Flip the comparison for next smaller, scan right to left for previous greater.
- Strict vs non-strict comparison decides how duplicates are handled; this matters for problems like "sum of subarray minimums".
- The same idea powers largest rectangle in a histogram, trapping rain water, and remove-k-digits.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
