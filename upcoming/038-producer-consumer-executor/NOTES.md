# Producer-consumer executor (fixed worker pool)

**Problem:** Build a minimal `ExecutorService`: a fixed number of worker threads consume tasks from a bounded queue that callers fill. Support `execute`, `submit` returning a future, graceful `shutdown`, `shutdownNow` and `awaitTermination`.

## Approach
- **Producer-consumer:** callers `put` tasks into an `ArrayBlockingQueue` and N long-lived workers loop on `take()`. A full queue blocks submitters, which gives natural backpressure.
- **submit:** wraps the `Callable` in a `Runnable` that completes a `CompletableFuture` with the result or the exception.
- **Graceful shutdown with poison pills:** set the `shutdown` flag, then enqueue one sentinel per worker. Pills sit *behind* every real task, so queued work still finishes. The flag check and enqueue in `execute` share a lock, so no task can slip in behind the pills.
- **shutdownNow:** drain the queue (returning the discarded tasks), interrupt the workers so blocking tasks wake up, then send the pills.
- **Fault isolation:** each task runs in `try/catch (Throwable)`, so a throwing task doesn't kill its worker. The interrupt flag is cleared after each task so it doesn't leak into the next one.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| execute / submit | O(1) plus blocking when the queue is full | O(queue capacity) |
| shutdown | O(threads) | — |
| Per worker | — | one thread stack |

## Interview talking points
- Why bound the queue? `Executors.newFixedThreadPool` uses an *unbounded* `LinkedBlockingQueue`, so a slow consumer leads to unbounded memory growth. With a bounded queue you must pick a policy: block (as here), reject, run on the caller's thread (`CallerRunsPolicy`) or drop.
- Poison pills vs flag polling: pills give clean ordering ("finish what's queued") with no busy waiting. With multiple consumers you need one pill per consumer.
- Interruption is cooperative: `shutdownNow` only works if tasks respond to interrupts (blocking calls or checking `Thread.interrupted()`).
- Sizing: around the number of cores for CPU-bound work, more for I/O-bound work (cores × (1 + wait/compute)). Java 21 virtual threads change the I/O-bound case: one virtual thread per task, with no pool needed.
- Exceptions thrown by `execute`d tasks are swallowed by default in many executors, which is a classic debugging trap. Here they're counted, and `submit` surfaces them through the future.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
