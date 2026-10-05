# Huffman coding (encode and decode)

**Problem:** Given symbol frequencies, build a prefix-free binary code that minimizes the total encoded length, then encode and decode text with it.

## Approach
- Put every symbol in a min-heap keyed by frequency. Repeatedly pop the two lightest trees and push a new node whose weight is their sum, until one tree remains.
- Walk the tree: left edge = `0`, right edge = `1`. Each leaf's path is its code. Because symbols sit only at leaves, no code is a prefix of another.
- A counter is the heap tiebreaker so equal weights never compare trees, and the output is deterministic.
- Edge cases: empty input gives an empty table; a single distinct symbol gets code `"0"`.
- **Decode:** rebuild a trie from the code table, walk it bit by bit, and emit a symbol at each leaf. Invalid or incomplete bit strings raise `ValueError`.

## Complexity
| Step | Time | Space |
|------|------|-------|
| build_codes | O(k log k) for k distinct symbols | O(k) |
| encode / decode | O(output bits) | O(k) trie |

## Interview talking points
- Why greedy works: the two least frequent symbols can always be deepest siblings in some optimal tree (exchange argument), then recurse.
- Optimal among *symbol-by-symbol* prefix codes: within 1 bit/symbol of entropy. Arithmetic coding / ANS get closer.
- Practical formats (DEFLATE, JPEG) use *canonical* Huffman codes: only code lengths are transmitted and codes are reassigned in sorted order.
- Length-limited Huffman (package-merge) when codes must fit in, e.g., 15 bits.
- The cost equals the sum of all merge weights, a handy invariant for testing.

## Run
From this folder: `python -m pytest -q`
