# Generic indexed priority queue (update and remove)

**Problem:** Build a reusable priority queue for any type with a custom comparator, and support the operations plain heaps lack: change an element's priority (decrease/increase key) and remove an arbitrary element, both in O(log n).

## Approach
- An array-backed binary heap driven by `compare(a, b)` (negative means `a` first), so min-heaps, max-heaps, and multi-field orderings are all one class.
- A `Map<T, index>` tracks each value's current heap slot. Every swap updates it, which makes `has` O(1) and lets `update`/`remove` find the element directly.
- **update(x):** after the caller mutates `x`'s priority, sift up then down from its slot. Only one of them will move it.
- **remove(x):** swap with the last element, pop, and re-sift the moved element (it may need to go up *or* down).
- Construction from an iterable uses O(n) bottom-up heapify.
- Values must be unique by identity (objects work naturally), since the Map is keyed by value.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| push / pop / update / remove | O(log n) | — |
| peek / has / size | O(1) | — |
| build from n items | O(n) | O(n) heap + map |

## Interview talking points
- Why decrease-key matters: Dijkstra and Prim with an indexed heap keep the heap at V entries instead of E (lazy deletion).
- Mutating a priority without calling `update` silently breaks the heap invariant. A safer API passes the new priority in (`update(x, p)`) and stores priorities separately.
- With primitive duplicates, wrap values in handles/ids or store `(priority, seq, value)`.
- Alternatives: pairing heaps (fast decrease-key in practice), Fibonacci heaps (O(1) amortized decrease-key), balanced BSTs (also give ordered iteration).

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
