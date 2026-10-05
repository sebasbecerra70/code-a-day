# LRU Cache

**Problem:** Design a cache with a fixed capacity that evicts the *least recently used* entry when full. `get` and `put` must both be O(1).

## Approach
- **Hash map** `key -> node` gives O(1) lookup.
- **Doubly linked list** keeps usage order: the most recent entry sits right after `head`, the least recent right before `tail`.
- Sentinel `head`/`tail` nodes remove the null edge cases when inserting or removing.
- On `get`/`put` of an existing key, unlink the node and move it to the front.
- On insert when full, evict `tail.prev` and delete its key from the map. That's why each node stores its key.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| get       | O(1) | —     |
| put       | O(1) | O(capacity) total |

## Interview talking points
- Why not a singly linked list? Removing a node needs its predecessor, which would take O(n) to find.
- Python's `collections.OrderedDict` with `move_to_end` / `popitem(last=False)` does the same in a few lines, but interviewers usually want the manual version.
- Real-world uses: CPU caches, DB buffer pools, CDN edge caches, `functools.lru_cache`.

## Run
```bash
cd python/2026-10-05-lru-cache && python -m pytest -q
```
