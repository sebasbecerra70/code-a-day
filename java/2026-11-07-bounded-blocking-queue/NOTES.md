# Bounded blocking queue (lock + two conditions)

**Problem:** Build a thread-safe FIFO queue with a fixed capacity: `put` blocks while it's full, `take` blocks while it's empty, and timed `offer` and `poll` give up after a deadline. This is the core of `ArrayBlockingQueue`.

## Approach
- Storage is a **ring buffer** (`head`, `tail`, `count`) in a fixed `Object[]`.
- One `ReentrantLock` guards all state. There are two `Condition`s: `notFull` (producers wait on it) and `notEmpty` (consumers wait on it).
- Every wait is inside a `while` loop that re-checks the predicate. This handles spurious wakeups and the case where another thread grabs the slot first.
- `enqueue` signals `notEmpty` and `dequeue` signals `notFull`. Because the two waiter groups are separate, `signal()` (not `signalAll()`) is enough and never wakes the wrong kind of thread.
- Timed variants use `awaitNanos`, which returns the time remaining, so the total wait is correct across several wakeups.
- `lockInterruptibly` and `await` both respond to interrupts, so blocked threads can be cancelled.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| put / take / offer / poll | O(1) plus waiting | O(capacity) total |
| size | O(1) | — |

## Interview talking points
- Why two conditions? With a single `wait`/`notify` monitor, a producer's `notify()` could wake another producer, so you'd need `notifyAll()` and suffer a thundering herd.
- Why `while` and not `if`? A woken thread must re-acquire the lock, and by then another thread may have consumed the item. Spurious wakeups are also allowed by the spec.
- `LinkedBlockingQueue` uses **two locks** (one for the head, one for the tail) so a producer and a consumer can proceed in parallel, with an atomic count between them.
- Backpressure: a bounded queue makes fast producers slow down instead of exhausting memory. That's why `ThreadPoolExecutor` setups should prefer bounded queues.
- Nulls are rejected because `poll` uses `null` to mean "timed out".

## Run
From this folder: `javac *.java && java -ea SolutionTest`
