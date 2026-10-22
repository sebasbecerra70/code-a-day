# BST iterators (in-order, reverse, pre/post-order)

**Problem:** Implement lazy iterators over a binary search tree for in-order (ascending and descending), pre-order and post-order traversals, without recursion and without materializing the whole traversal. Use them for two-sum on a BST and k-th smallest.

## Approach
- **In-order:** keep a stack of the current "left spine". `next()` pops a node, then pushes the left spine of its right subtree. Reverse in-order is the mirror (right spine, then the left child's right spine).
- **Pre-order:** a stack seeded with the root; pop, emit, push right then left so left comes out first.
- **Post-order (one stack):** descend from a node to its first post-order node by always going left if possible, else right, pushing the path. After popping a node, if it was the left child of the new stack top, descend into the top's right subtree next.
- **Two-sum:** a forward and a reverse in-order iterator act as the two pointers of the sorted-array two-sum, using O(h) memory instead of O(n).

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| Construct | O(h) | O(h) |
| next() | amortized O(1), worst O(h) | O(h) total |
| Full traversal | O(n) | O(h) |
| Two-sum / k-th smallest | O(n) / O(h + k) | O(h) |

## Interview talking points
- Amortized O(1): every node is pushed and popped exactly once across the whole traversal.
- The iterator is lazy, so you can stop early (k-th smallest) without walking the whole tree.
- Morris traversal gets O(1) extra space by threading temporary right pointers, but it mutates the tree during the walk and isn't safe with concurrent readers.
- If the tree is modified mid-iteration, the stack can go stale; `TreeMap` iterators detect this with a modCount and throw `ConcurrentModificationException`.
- Pre-order and post-order iterators are used for serialization and safe deletion (children before parent), respectively.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
