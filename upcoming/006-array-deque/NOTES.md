# Array deque (circular buffer that grows)

**Problem:** Implement a double-ended queue with O(1) amortized `addFirst`, `addLast`, `removeFirst`, `removeLast`, O(1) random access by position, and an iterator. Grow when full and shrink when mostly empty.

## Approach
- Store elements in a **circular array**: `head` is the physical index of the front, and logical index `i` lives at `(head + i) mod capacity`.
- Keep the capacity a **power of two**, so `mod capacity` becomes `& (capacity − 1)`. That also makes `head − 1` wrap correctly from 0 to `capacity − 1` with no branch.
- `addFirst` moves `head` back one slot; `addLast` writes at `head + size`.
- **Grow:** double the array and copy elements in logical order so the front lands at index 0.
- **Shrink:** halve only when the deque is ≤ 1/4 full. Shrinking at 1/2 would let an add/remove pair at the boundary resize every time.
- Removed slots are set to `null` so the deque doesn't keep garbage alive.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| addFirst / addLast | O(1) amortized | O(n) total |
| removeFirst / removeLast | O(1) amortized | — |
| get(i) / peek | O(1) | — |
| resize | O(n) | O(n) |

## Interview talking points
- Why not a linked list? The array version allocates nothing per element and is far more cache friendly. That's why `java.util.ArrayDeque` beats `LinkedList` as both stack and queue.
- Doubling gives amortized O(1): n pushes copy at most about 2n elements in total.
- Generic arrays can't be created in Java (`new T[]` is illegal because of erasure), so store `Object[]` and cast on read.
- Distinguishing full from empty is easy here because we track `size`, not just head/tail indices.
- `java.util.ArrayDeque` forbids `null` elements so `poll()` can use `null` to mean "empty"; this version allows nulls and throws on empty instead.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
