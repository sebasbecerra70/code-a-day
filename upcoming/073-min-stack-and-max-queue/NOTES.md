# Min stack and max queue (O(1) extremes)

**Problem:** Design a stack that returns its minimum in O(1) (LeetCode 155), and a FIFO queue that returns its maximum in O(1) amortized.

## Approach
- **MinStack:** a parallel stack where `mins[i]` is the minimum of everything at or below position `i`. Push stores `min(x, previous min)`; pop removes both. Duplicates work naturally.
- **MaxQueue (monotonic deque):** alongside the items, keep `maxes` in non-increasing order. On enqueue, pop smaller values off the back of `maxes` (they can never be the max while `x` is in the queue), then append `x`. The front of `maxes` is the max. On dequeue, if the leaving item equals the front of `maxes`, advance it. Head indices avoid O(n) `shift()`, and a periodic compaction keeps memory bounded.
- **TwoStackMaxQueue:** a queue built from two stacks, each storing `(value, running max)`. Dequeue refills the outbox from the inbox only when the outbox is empty. Max is the larger of the two stack tops.

## Complexity
| Structure | push/enqueue | pop/dequeue | min/max | Space |
|-----------|--------------|-------------|---------|-------|
| MinStack | O(1) | O(1) | O(1) | O(n) |
| MaxQueue | O(1) amortized | O(1) amortized | O(1) | O(n) |
| TwoStackMaxQueue | O(1) | O(1) amortized | O(1) | O(n) |

## Interview talking points
- Space-saving MinStack variants: store only when `x <= min`, or store differences from the current min.
- The monotonic deque is the core of sliding window maximum (LeetCode 239) and some DP optimizations.
- Two-stack queues show up in functional programming (persistent queues) and as a general way to make any "stack with aggregate" into a "queue with aggregate" for any associative operation.
- Why amortized: each element enters and leaves `maxes` (or moves between stacks) at most once.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
