# Promise pool with a concurrency limit

**Problem:** Process a list of async jobs (HTTP requests, file reads) without firing them all at once: keep at most `limit` in flight, return results in input order, and fail fast on error.

## Approach
- **Worker pool:** start `min(limit, n)` async workers. Each loops: take the next index (`next++`), await `fn(item)`, store the result at that index. Single-threaded JS makes the shared counter safe.
- Results are written by index, so output order matches input order regardless of finish order.
- On the first rejection, set `failed` so workers stop picking up new items; `Promise.all` over the workers rejects with that error.
- `settleWithLimit` wraps each call so it never rejects (like `Promise.allSettled`).
- `createLimiter` (p-limit style) is a reusable gate: callers `await` a slot via a FIFO queue of resolvers, and `finally` releases it even on errors.

## Complexity
| Aspect | Cost |
|--------|------|
| Scheduling overhead | O(n) total |
| Space | O(n) results + O(limit) workers (limiter queue O(waiting)) |
| Wall time | ≈ total work / limit when jobs are similar |

## Interview talking points
- Why not `Promise.all(items.map(fn))`? It starts everything at once: sockets, memory, and rate limits all blow up.
- Why not fixed batches of `limit`? One slow job stalls each batch; a pool keeps all slots busy.
- Fail-fast doesn't cancel in-flight work; pass an `AbortSignal` to make tasks cancelable.
- Add retries/backoff per task, or a token bucket for rate (not just concurrency) limits.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
