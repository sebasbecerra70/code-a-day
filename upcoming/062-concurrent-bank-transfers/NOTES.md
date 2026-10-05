# Concurrent bank transfers (deadlock-free lock ordering)

**Problem:** Many threads transfer money between accounts at once. Each transfer must be atomic, total money must be conserved, accounts must never go negative, unrelated transfers should run in parallel, and the system must never deadlock.

## Approach
- **Per-account `ReentrantLock`:** fine-grained locking, so A→B and C→D don't contend.
- **Global lock ordering:** always lock the lower account id first. A deadlock needs a circular wait, and if every thread acquires locks in the same total order, no cycle can form. A→B and B→A both lock A first.
- Validation (`debit` throws on insufficient funds) runs **before** any mutation while both locks are held, so a failed transfer leaves no partial state. `finally` blocks always release the locks.
- **Alternative (`tryTransfer`):** `tryLock` both, and if the second lock isn't free, release the first and back off for a **random** time before retrying. This needs no global order but has to handle livelock, which the randomized backoff breaks.
- **Consistent total:** lock all accounts in id order, then sum. Summing unlocked balances could observe money "in flight" (debited but not yet credited).

## Complexity
| Operation | Time | Notes |
|-----------|------|-------|
| transfer | O(1) | 2 lock acquisitions |
| tryTransfer | O(1) per attempt | retries until the deadline |
| totalBalance | O(n log n) sort + O(n) | blocks all transfers briefly |

## Interview talking points
- The four Coffman conditions for deadlock are mutual exclusion, hold-and-wait, no preemption and circular wait. Lock ordering removes circular wait; tryLock with backoff removes hold-and-wait.
- If ids weren't unique or comparable, you could order by `System.identityHashCode` with a tie-breaker lock (as in *Java Concurrency in Practice*).
- A single global lock is correct and simple, but it serializes everything. Fine-grained locks scale but need an ordering discipline.
- Real banks use databases: row locks with deadlock detection (the database aborts one transaction), optimistic concurrency with version columns, or a single-writer ledger (append-only double-entry records) instead of mutating balances.
- `synchronized` can't time out or be interrupted while waiting. `ReentrantLock` adds `tryLock`, timeouts and `lockInterruptibly`.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
