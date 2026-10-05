# Skip list (ordered map with expected O(log n) ops)

**Problem:** Implement a sorted key-value map with search, insert, delete, range queries, and floor/ceiling in expected O(log n), without the rotations of a balanced BST.

## Approach
- Level 0 is a sorted singly linked list of all nodes. Each node also appears in levels 1, 2, ... with probability `p` per level (a coin flip), so level `i` holds about `n·pⁱ` nodes and acts as an express lane over the level below.
- **Search:** start at the head's highest level and move right while the next key is `< target`, then drop down a level. Record the last node visited on each level in `update[]`. Those are exactly the predecessors to splice around.
- **Insert:** find `update[]`. If the key exists, overwrite the value. Otherwise draw a random height and splice the new node in after `update[i]` on each of its levels.
- **Delete:** unlink from every level the node appears in, then shrink the list's level while the top levels are empty.
- A head sentinel with `MAX_LEVEL` pointers removes edge cases. Range queries walk level 0 from the ceiling of `lo`.
- The RNG is seedable so tests are deterministic. A randomized test checks against a dict plus `bisect`.

## Complexity
| Aspect | Cost |
|--------|------|
| Search / insert / delete | O(log n) expected, O(n) worst |
| Range query | O(log n + k) |
| Space | O(n) expected, n/(1-p) pointers total |

## Interview talking points
- Why choose it over a red-black tree? It's much simpler, with local updates only and no rotations. That makes lock-free concurrent versions practical (Java's `ConcurrentSkipListMap`).
- Real-world uses: Redis sorted sets (ZSET) and LevelDB/RocksDB memtables.
- Choosing `p`: 1/2 gives fewer comparisons, and 1/4 uses fewer pointers (about 1.33 per node vs 2) with similar speed. Redis uses 1/4.
- Expected search cost ≈ (log₁/ₚ n)/p. The analysis traces the search path backwards (Pugh, 1990).
- Indexable skip lists store span widths on each pointer, which gives O(log n) rank and select.

## Run
From this folder: `python -m pytest -q`
