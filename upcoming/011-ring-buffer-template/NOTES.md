# Ring buffer (fixed-capacity circular queue template)

**Problem:** Build a FIFO queue with a fixed capacity `N` (a template parameter) that never allocates. Support `push` (fails when full), `push_overwrite` (drops the oldest when full), `pop`, `front`, `back`, and indexed access from oldest to newest.

## Approach
- Store elements in a `std::array<T, N>`, so the buffer lives inline with no heap allocation.
- Track `head` (oldest element) and `size`. The write slot is `(head + size) % N`.
- `pop` advances `head` modulo `N`. `push_overwrite` on a full buffer writes over the oldest slot and advances `head`.
- Keeping `size` explicitly avoids the classic ambiguity where `head == tail` could mean empty *or* full.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| push / push_overwrite / pop | O(1) | O(N) total, fixed |
| front / back / operator[] | O(1) | — |

## Interview talking points
- Two ways to tell full from empty: keep a `size` counter, or leave one slot unused (`(tail + 1) % N == head` means full).
- If `N` is a power of two, `% N` becomes `& (N - 1)`, which is cheaper. Lock-free SPSC queues often use this plus monotonically increasing indices.
- Use cases: audio/network buffers, logging the last N events, rate limiters, producer/consumer pipes.
- A single-producer/single-consumer version can be made lock-free with `std::atomic` head/tail and acquire/release ordering.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
