# Circular buffer (ring buffer)

**Problem:** Implement a fixed-capacity FIFO buffer that never reallocates: O(1) push, shift, pop, and random access, with a choice between rejecting writes when full and overwriting the oldest element.

## Approach
- A fixed array plus `head` (index of the oldest element) and `size`. The back is at `(head + size) % capacity`. Indices wrap with modulo, so nothing ever moves.
- **Full buffer:** throw, or (overwrite mode) replace the element at `head` and advance `head`, returning the evicted value.
- Removed slots are set to `undefined` so the GC can reclaim objects.
- Storing `size` instead of a `tail` pointer avoids the classic "empty vs. full both have head == tail" ambiguity.
- Iterable, with `at(i)` supporting negative indices.
- `MovingAverage` shows a typical use: keep a running sum and subtract whatever the buffer evicts.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| push / shift / pop / at | O(1) | O(capacity) total |
| iterate / toArray | O(n) | O(n) for the array |

## Interview talking points
- Empty-vs-full disambiguation: a size counter, a "full" flag, or always leaving one slot empty.
- Power-of-two capacity lets you replace `% capacity` with `& (capacity - 1)`.
- Lock-free single-producer/single-consumer ring buffers (LMAX Disruptor, audio drivers, kernel io_uring) use separate read/write indices with memory barriers.
- Uses: logs of the last N events, network packet buffers, streaming audio, rolling metrics, undo history with a cap.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
