# LFU cache (O(1) with frequency buckets)

**Problem:** Design a fixed-capacity cache that evicts the **least frequently used** key, breaking ties by **least recently used**, with O(1) `get` and `put`.

## Approach
- Three maps:
  - `values[key]` holds the value, and `freq[key]` holds the use count.
  - `buckets[f]` is an `OrderedDict` of keys with frequency `f`, in recency order (oldest first).
- `min_freq` is the smallest frequency with a non-empty bucket.
- **Touch** (on get, or put of an existing key): move the key from bucket `f` to the end of bucket `f+1`. If bucket `f` empties and was `min_freq`, then `min_freq` becomes `f+1`. That's the only way the minimum can change during a touch.
- **Insert new key:** if full, evict the *first* key of `buckets[min_freq]`. Then add the key with frequency 1 and reset `min_freq = 1`.
- `pop` (explicit delete) is the one operation that can't update `min_freq` in O(1) in general. It rescans bucket keys, which is acceptable because deletes are rare. It's documented in the code.
- A randomized test compares against a naive O(n) LFU that tracks `(freq, last_use)`.

## Complexity
| Operation | Cost |
|--------|------|
| get / put | O(1) average |
| pop | O(1) usually, O(distinct frequencies) when the min bucket empties |
| Space | O(capacity) |

## Interview talking points
- Why not a heap? A heap gives O(log n) per access, because every `get` changes a key's priority.
- The classic paper solution (Shah, Mitra & Matani 2010) uses a doubly linked list of frequency nodes, each with a list of keys. `OrderedDict` gives the same structure with much less code.
- LFU weaknesses: "cache pollution" from items that were hot long ago. Fixes include frequency decay/aging, W-TinyLFU (Caffeine), and windowed counts.
- LRU vs LFU: LRU adapts to shifting workloads, while LFU protects stable hot sets from scans. ARC and 2Q blend the two.
- Thread safety: wrap operations in a lock, or shard the cache by key hash.

## Run
From this folder: `python -m pytest -q`
