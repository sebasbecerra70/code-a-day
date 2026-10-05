# Flatten nested list iterator (lazy, stack of iterators)

**Problem:** Given a nested list of integers like `[1, [4, [6]], []]`, implement an iterator that yields the integers in order (`1, 4, 6`) lazily, without flattening everything up front. Also flatten an iterator of iterators (a jagged 2D array) and support `remove()`.

## Approach
- **Stack of iterators:** push an iterator for the top-level list. To advance, look at the top iterator: if it's exhausted, pop it; if it yields a list, push that list's iterator; if it yields an integer, cache it as the look-ahead.
- **All the work happens in `hasNext()`**, which caches the next integer. `next()` just calls `hasNext()` and returns the cached value. This makes `hasNext()` idempotent and correct for inputs that contain only empty lists (`[[], [[]]]`).
- The node type is a `sealed interface` with `Int` and `Many` records, matched with a pattern `switch`.
- **2D flatten:** skip empty inner iterators in `hasNext()`. For `remove()`, remember which inner iterator produced the last element, because `hasNext()` may already have moved on to the next row.
- The iterative design handles nesting 10,000 levels deep, which would overflow a recursive flatten.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| hasNext / next | amortized O(1) (each list is pushed and popped once) | O(depth) |
| Full iteration | O(total elements + total lists) | O(depth) |
| Eager flatten (alternative) | O(N) up front | O(N) |

## Interview talking points
- Lazy vs eager: flattening in the constructor is simpler, but it uses O(N) memory, can't handle huge or streaming inputs, and does work the caller may never need.
- The classic bug is putting the descent logic in `next()` and having `hasNext()` just return `!stack.isEmpty()`. That breaks on empty sublists.
- Iterator contract: `hasNext()` must not have visible side effects (calling it twice changes nothing), and `next()` must work even if `hasNext()` was never called.
- The `remove()` subtlety in the 2D case is a common follow-up (Google's "Flatten 2D Vector").
- The same stack-of-iterators idea gives lazy tree traversal, directory walking (`Files.walk`) and JSON streaming parsers.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
