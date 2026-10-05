# Largest rectangle in a histogram (and maximal rectangle)

**Problem:** Given bar heights of width 1, find the area of the largest rectangle that fits under the histogram. Extension: find the largest rectangle containing only `'1'`s in a binary matrix.

## Approach
- The best rectangle's height equals some bar `h[k]`, and it extends from the nearest shorter bar on the left to the nearest shorter bar on the right. So we need "previous smaller" and "next smaller" for every bar.
- Keep a **stack of indices with increasing heights**. When bar `i` is not taller than the top, the top bar has found its right boundary (`i`). Its left boundary is the new stack top after popping. Area = `height × (i − left)`.
- Append a virtual bar of height 0 at the end so every remaining bar gets popped.
- **Maximal rectangle:** for each row, `heights[c]` = number of consecutive `'1'`s ending at that row in column `c`. Run the histogram algorithm on each row.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| largestRectangleArea | O(n) | O(n) |
| maximalRectangle (R × C) | O(R·C) | O(C) |

## Interview talking points
- Each index is pushed and popped once, so the inner `while` is amortized O(1).
- Popping on `>=` (not just `>`) handles equal heights correctly: the last equal bar still computes the full width.
- Alternatives: divide and conquer on the minimum bar (O(n log n) with a segment tree, O(n²) worst case naively).
- Use 64-bit area: 10⁵ bars of height 10⁹ overflow 32 bits.
- Related: trapping rain water, sum of subarray minimums, maximal square (simpler DP).

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
