# Retry with exponential backoff and jitter

**Problem:** Wrap a flaky async call (network request, DB connection) so it retries transient failures with growing delays, stops on non-retryable errors, respects cancellation, and doesn't make a thundering herd worse.

## Approach
- Attempt `fn`. On failure, if attempts remain and `shouldRetry(err)` allows it, wait and try again; otherwise throw.
- **Exponential backoff:** retry `k` waits up to `min(maxMs, baseMs · 2^(k-1))`.
- **Full jitter:** wait a uniformly random time in `[0, ceiling]`. Randomizing spreads out clients that failed together, so they don't all retry at the same instant.
- Exhausted retries throw a `RetryError` carrying the attempt count and last error (also as `cause`). Non-retryable errors pass through unchanged.
- `AbortSignal` cancels both before an attempt and during the sleep.
- `sleep` and `random` are injectable, so tests check exact delays without waiting.

## Complexity
| Aspect | Cost |
|--------|------|
| Attempts | ≤ retries + 1 |
| Worst-case total wait | Σ min(maxMs, base·2^k) (halved on average with full jitter) |
| Space | O(1) |

## Interview talking points
- What to retry: timeouts, 429, 502/503/504, connection resets. Not 400/401/404 or validation errors.
- Only retry **idempotent** operations, or make them idempotent with idempotency keys.
- Jitter strategies (AWS Architecture Blog): full, equal, decorrelated. Full jitter usually minimizes total work.
- Honor `Retry-After` headers when present.
- Retries multiply load during outages: pair with a circuit breaker and retry budgets, and retry at one layer only.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
