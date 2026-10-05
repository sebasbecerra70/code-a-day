# Sliding window maximum (monotonic deque)

**Problem:** Given an array and a window size `k`, return the maximum (or minimum) of every contiguous window of length `k` in O(n). Follow-ups: a streaming version, and the longest subarray whose max - min stays within a limit.

## Approach
- Keep a deque of **indices** whose values are strictly decreasing from front to back.
- For each new index `i`:
  1. Drop the front if it slid out of the window (`front <= i - k`).
  2. Pop from the back while the back's value is `<=` the new value. Those elements are older *and* no larger, so they can never be a window max again.
  3. Push `i`. Once `i >= k - 1`, the front is the current window's max.
- Each index is pushed and popped at most once, so the whole pass is amortized O(n).
- Min is the same with the comparison flipped. The code shares one routine via a `dominates` predicate.
- **Streaming `MaxWindow`:** the same deque keyed by arrival count, with occasional compaction so memory stays O(k).
- **Longest within limit:** a variable-size window with one max-deque and one min-deque. Shrink from the left while `max - min > limit`.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(n) amortized |
| Space | O(k) deque (+ output) |
| Brute force | O(n·k) |
| Heap alternative | O(n log n) with lazy deletion |

## Interview talking points
- Why store indices, not values? You need positions to know when the front expires, and duplicates stay unambiguous.
- `<=` vs `<` when popping: either is correct for max values. `<=` keeps the deque smaller.
- `Array.shift()` is O(n) in JS. A head pointer (or ring buffer) keeps pops O(1).
- Alternative O(n) trick: block decomposition with prefix and suffix maxima per block of size k.
- The same monotonic-queue idea powers DP optimizations (e.g. "constrained subsequence sum", "jump game VI").

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
