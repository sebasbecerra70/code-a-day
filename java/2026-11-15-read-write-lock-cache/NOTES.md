# Read-write lock and a read-mostly cache

**Problem:** Implement a read-write lock (many concurrent readers or one exclusive writer) that doesn't starve writers, and use it to build a thread-safe cache whose `getOrCompute` loads each key at most once.

## Approach
- **Lock state:** `activeReaders`, `writerActive` and `waitingWriters`, guarded by the object's monitor (`synchronized` + `wait`/`notifyAll`).
- **Readers** wait while a writer is active **or waiting**. That makes the lock writer-preferring, so a continuous stream of readers can't starve a writer.
- **Writers** register as waiting, then wait until there's no writer and no readers.
- **Unlock:** the last reader out calls `notifyAll()`, and the writer's unlock does too. Every waiter re-checks its condition in a `while` loop.
- **Cache:** `get` takes the read lock (parallel); `put` and `remove` take the write lock. `getOrCompute` checks under the read lock, and on a miss it **releases** that lock, takes the write lock and **re-checks** before loading. A read lock can't be upgraded in place: two readers trying to upgrade would deadlock waiting for each other.

## Complexity
| Operation | Time | Notes |
|-----------|------|-------|
| get (hit) | O(1) | concurrent with other readers |
| put / remove / miss | O(1) plus loader time | exclusive |
| Lock acquire/release | O(1) | `notifyAll` wakes all waiters (thundering herd) |

## Interview talking points
- Read-write locks only pay off when reads dominate and the critical section is long enough to outweigh the extra bookkeeping. For a tiny `HashMap` lookup, `ConcurrentHashMap` (lock striping and lock-free reads) wins easily.
- Reader vs writer preference: reader preference maximizes throughput but can starve writers; writer preference (as here) can starve readers under heavy writes. A fair lock serves waiters in FIFO order.
- `ReentrantReadWriteLock` allows **downgrading** (take the write lock, take the read lock, release the write lock) but not upgrading. `StampedLock` adds optimistic reads: read without locking, then validate the stamp.
- Doing the load under the write lock blocks all readers while it runs. Production caches (Caffeine, `ConcurrentHashMap.computeIfAbsent`) lock per key or store a future per key, so other keys stay readable.
- The double-checked re-read after taking the write lock is what guarantees "load once".

## Run
From this folder: `javac *.java && java -ea SolutionTest`
