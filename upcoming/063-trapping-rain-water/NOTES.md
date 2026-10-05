# Trapping rain water (two pointers, stack, and 2D heap)

**Problem:** Given bar heights, how many units of rain water are trapped after it rains? Extension: the same question on a 2D height map, where water can escape over any border cell.

## Approach
- Water above bar `i` is `min(maxLeft(i), maxRight(i)) − h[i]`.
- **Two pointers:** move inward from both ends. If `h[l] < h[r]`, the right side has a wall at least `h[r]` tall, so the water at `l` is decided by `leftMax` alone. Process `l` and advance; otherwise mirror. O(1) extra space.
- **Monotonic stack:** keep decreasing heights. When a taller bar arrives, pop the floor; the new top is the left wall, the current bar the right wall. Add one horizontal layer: `width × (min(walls) − floor)`.
- **2D:** the border is the initial "wall". Repeatedly take the **lowest** boundary cell from a min-heap (that's the spill height), visit its unseen neighbors, add `max(0, level − height)` water, and push them with level `max(level, height)`. It's Dijkstra-like: the water level of a cell is the minimum over escape paths of the maximum height on the path.

## Complexity
| Method | Time | Space |
|--------|------|-------|
| two pointers | O(n) | O(1) |
| monotonic stack | O(n) | O(n) |
| 2D heap (R × C) | O(RC log(RC)) | O(RC) |

## Interview talking points
- The prefix-max/suffix-max arrays version is the easiest to explain; two pointers is the space optimization of it.
- The stack version counts water in horizontal layers; the others count vertical columns. Both sum to the same total.
- Why doesn't the two-pointer trick work in 2D? There are many escape paths, not just left and right, so you need the priority queue.
- Edge cases: fewer than 3 bars, monotonic arrays, flat plateaus, and the 2D border leaking.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
