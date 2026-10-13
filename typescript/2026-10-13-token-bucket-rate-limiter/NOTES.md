# Token bucket rate limiter (injectable clock)

**Problem:** Limit how often a client can act: allow short bursts up to some size, but hold the long-run rate to `r` requests per second.

## Approach
- A bucket holds up to `capacity` tokens and refills at `refillPerSec`.
- Refill lazily: on each call, add `elapsed * rate` tokens (capped) based on the time since the last call. No timers or background threads.
- A request costing `c` succeeds if `tokens >= c` and spends them; otherwise it's rejected.
- `waitTime` tells callers how long to back off (useful for a `Retry-After` header).
- The clock is a constructor parameter, so tests advance a fake clock instead of sleeping.
- `KeyedRateLimiter` keeps one bucket per user/IP.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| tryConsume / waitTime | O(1) | O(1) per bucket |
| KeyedRateLimiter.allow | O(1) average | O(#keys) |

## Interview talking points
- Token bucket vs. leaky bucket: token bucket permits bursts; leaky bucket smooths output to a constant rate.
- Fixed window counters are simple but allow 2x bursts at window edges; sliding-window log is exact but stores timestamps; sliding-window counter approximates it cheaply.
- Distributed limiting: store `(tokens, last)` in Redis and update atomically with a Lua script.
- Memory growth in the keyed version: evict idle buckets (a full bucket is equivalent to no bucket) with a TTL/LRU.
- Use a monotonic clock (`performance.now()`) in production to avoid wall-clock jumps.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
