# Consistent hashing ring (virtual nodes)

**Problem:** Map keys to a changing set of servers so that adding or removing a server only remaps about `1/N` of the keys, instead of nearly all of them as with `hash(key) % N`.

## Approach
- Hash every server onto a 64-bit circle many times (`replicas` "virtual nodes" per server: `node#0`, `node#1`, ...).
- Keep the virtual-node hashes in a sorted list, plus a dict from hash to physical node.
- To place a key: hash it, `bisect_right` into the sorted list, and take the next point clockwise (wrapping to index 0).
- Adding a node inserts its points; only keys falling just before those points move, and they all move *to* the new node. Removing does the reverse.

## Complexity
| Operation   | Time | Space |
|-------------|------|-------|
| get_node    | O(log(N·R)) | — |
| add_node    | O(R · N·R) with `insort` (O(R log(N·R)) with a balanced tree) | O(R) per node |
| remove_node | O(R · N·R) | — |

N = physical nodes, R = replicas.

## Interview talking points
- Why virtual nodes? With one point per server the arcs are very uneven; many points smooth the load and spread a removed node's keys across all survivors.
- Weighted capacity: give bigger servers more replicas.
- The hash only needs to be stable and uniform (md5/murmur/xxhash); it is not a security boundary.
- Used in Dynamo, Cassandra, memcached clients (ketama), CDNs. Alternatives: rendezvous (HRW) hashing, jump consistent hash.
- Replication: walk clockwise and take the next K *distinct* physical nodes.

## Run
From this folder: `python -m pytest -q`
