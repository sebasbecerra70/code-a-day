# Open addressing hash map (linear probing, backward-shift delete)

**Problem:** Implement a hash map without chaining: all entries live in one array, and collisions are resolved by probing other slots. Support `put`, `get`, `containsKey`, `remove`, and automatic resizing.

## Approach
- Parallel `keys`/`values` arrays with a **power-of-two capacity**; a key's home slot is `mix(hash) & (capacity − 1)`. `mix` XORs the high 16 bits into the low ones (as `java.util.HashMap` does), since the mask throws away the high bits.
- **Linear probing:** from the home slot, step `+1` (wrapping) until finding the key or an empty slot.
- Keep the **load factor ≤ 0.5** by doubling and re-inserting. Linear probing degrades fast as the table fills.
- **Deletion without tombstones (backward shift):** after emptying slot `i`, walk the rest of the probe run. An entry at `j` may move into the hole only if its home slot is *not* cyclically between the hole and `j`; otherwise moving it would put it before its home, where lookups would never find it. Each moved entry leaves a new hole. Stop at the first empty slot.

## Complexity
| Operation | Time (expected, load ≤ 0.5) | Worst case |
|-----------|-----------------------------|------------|
| put / get / containsKey | O(1) | O(n) |
| remove (with backward shift) | O(1) | O(n) |
| resize | O(n), amortized O(1) per put | — |
| Space | O(capacity) = O(n) | — |

## Interview talking points
- Chaining vs open addressing: open addressing has no per-entry node allocation and better cache locality; chaining tolerates high load factors and bad hashes better. Java's `HashMap` chains (and turns long chains into trees); `IdentityHashMap` and many C++/Rust maps use open addressing.
- Tombstones are simpler to delete with, but they pile up and slow lookups until the next rehash. Backward shift keeps runs tight.
- **Primary clustering:** runs merge and grow under linear probing. Quadratic probing and double hashing reduce it; Robin Hood hashing evens out probe lengths.
- Expected probes for a successful search with linear probing are about `½(1 + 1/(1−α))`.
- `equals` and `hashCode` must agree, and mutating a key after insertion breaks the map.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
