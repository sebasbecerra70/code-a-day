# Generic doubly linked list with iterator

**Problem:** Implement a generic doubly linked list in Java: add/remove at both ends and by index, `get`/`set`/`indexOf`, `removeIf`, in-place `reverse`, and an `Iterator` that supports `remove()` and fails fast if the list is modified behind its back.

## Approach
- Two **sentinel** nodes, `head` and `tail`, bracket the real elements. Every insert is "link after some node" and every delete is "unlink this node", with no special cases for empty lists or ends.
- `nodeAt(i)` walks from whichever end is closer, halving the worst case.
- **reverse:** swap `prev`/`next` in every node (sentinels included), then restore the sentinels' roles and reconnect them to the new first and last nodes.
- **Fail-fast iterator:** the list bumps `modCount` on every structural change. The iterator remembers the value it expects and throws `ConcurrentModificationException` on mismatch. `Iterator.remove()` goes through the list but then resyncs its expected count.
- `removeIf` saves `next` before unlinking, so removal during traversal is safe.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| addFirst / addLast / removeFirst / removeLast | O(1) | O(1) |
| get / set / add(i) / remove(i) | O(min(i, n − i)) | O(1) |
| indexOf / removeIf / reverse | O(n) | O(1) |
| iterator next / remove | O(1) | O(1) |

## Interview talking points
- Sentinels trade two extra nodes for much simpler and less bug-prone code.
- `java.util.LinkedList` is almost always slower than `ArrayList` in practice: each node is a separate object (about 24–40 bytes overhead) and traversal misses the cache.
- Fail-fast is best-effort, not a thread-safety guarantee; concurrent code needs `ConcurrentLinkedDeque` or external locking.
- Nulling `prev`/`next` on unlink helps the GC and turns bugs with stale nodes into immediate `NullPointerException`s.
- Classic follow-ups: reverse a singly linked list iteratively and recursively, detect a cycle (Floyd), find the middle with slow/fast pointers.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
