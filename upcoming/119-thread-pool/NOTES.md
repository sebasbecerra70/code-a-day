# Thread pool with futures

**Problem:** Creating a thread per task is expensive. Build a pool of `N` worker threads that pull tasks from a shared queue. `submit(f, args...)` should return a `std::future` with the result (or the exception the task threw), and destroying the pool should finish queued work and join all threads.

## Approach
- **Shared state:** a `std::queue<std::function<void()>>`, a mutex, a condition variable, and a `stopping` flag.
- **Workers** loop: wait on the condition variable until there's work or the pool is stopping, pop one task under the lock, then **run it outside the lock**. They exit only when stopping *and* the queue is empty, so queued work is drained.
- **submit:** wrap the call in a `std::packaged_task<R()>`, which connects the result or exception to a `std::future`. `packaged_task` is move-only and `std::function` requires copyable callables, so it's held in a `shared_ptr`. Arguments are captured into a tuple and applied with `std::apply`, which also supports move-only arguments.
- **Shutdown:** set `stopping` under the lock, `notify_all`, then `join` every worker.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| submit | O(1) plus lock contention | O(1) per queued task |
| worker dequeue | O(1) | — |
| construction / destruction | O(N) threads | O(N) |

## Interview talking points
- Always wait with a **predicate**: condition variables can wake spuriously, and the predicate also catches notifications sent before the worker started waiting.
- Never hold the lock while running a task, or the pool degrades to one thread at a time (and can deadlock if a task submits more work).
- Waiting on a future from *inside* a task can deadlock a full pool: every worker waits on work that no free worker can run. Work-stealing pools (e.g. TBB, Java's ForkJoinPool) help with that.
- Improvements: per-worker queues with work stealing to cut contention, bounded queues for backpressure, task priorities, and C++20 `std::jthread` with `stop_token`.
- Sizing: about the core count for CPU-bound work; more threads for I/O-bound work.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
