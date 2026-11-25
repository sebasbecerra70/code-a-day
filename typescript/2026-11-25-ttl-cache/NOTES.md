# TTL cache with LRU bound

**Problem:** Build an in-memory cache where each entry expires after a time-to-live, optionally capped at `maxSize` entries with least-recently-used eviction. Time must be testable without sleeping.

## Approach
- Store `{ value, expiresAt }` in a JS `Map`. A `Map` iterates in insertion order, so deleting and re-inserting a key on access keeps the LRU entry first. No linked list needed.
- **Lazy expiry:** `get`/`has`/`peek` check `expiresAt <= now()` and delete stale entries on the spot. `prune()` sweeps everything when you want to reclaim memory.
- On `set` past `maxSize`, prune expired entries first, then evict the first (LRU) key if still over.
- `peek` reads without touching recency; `getOrSet` is the read-through pattern.
- The clock is injected, so tests advance time deterministically.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| get / set / has / delete | O(1) average (set is O(n) only when it has to prune) | O(n) |
| prune | O(n) | — |

## Interview talking points
- Lazy vs. active expiry: Redis does both (lazy on access + periodic random sampling). Timers per key don't scale.
- For efficient proactive expiry, keep a min-heap or timing wheel keyed by `expiresAt`.
- Cache stampede: many callers miss at once and all recompute. Fix with request coalescing (store the in-flight Promise), jittered TTLs, or stale-while-revalidate.
- Use a monotonic clock (`performance.now()`) so wall-clock changes don't expire everything.
- TTL vs. LRU answer different questions: freshness vs. memory bound. Real caches usually need both.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
