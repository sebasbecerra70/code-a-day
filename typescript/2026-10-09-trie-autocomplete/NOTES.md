# Trie autocomplete (top-k by frequency)

**Problem:** Store a dictionary of words with usage counts and, given a prefix, return the `k` most frequent words that start with it. Also support lookup and deletion.

## Approach
- Each node has a `Map<char, child>` and a `count` (0 = not a word end).
- `insert` walks/creates nodes per character and adds to the end node's count.
- `autocomplete` walks to the prefix node, then does an iterative DFS of that subtree to collect `(word, count)` pairs, sorts by count desc then lexicographically, and keeps `k`.
- `delete` records the path, clears the count, and prunes childless non-word nodes bottom-up.
- Iterating with `for..of` over strings handles Unicode code points (emoji) correctly.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| insert / has / delete | O(L) | O(L) new nodes on insert |
| autocomplete | O(P + S log S), S = words under prefix | O(S) |

## Interview talking points
- To make autocomplete O(P + k), cache the top-k list at every node and update it on insert (more memory, faster reads).
- Memory: a `Map` per node is flexible; a fixed 26-slot array is faster for lowercase ASCII. Radix/compressed tries merge single-child chains.
- Alternatives: sorted array + binary search on prefix range, or a ternary search tree.
- Real-world: search boxes, IDE completion, IP routing (longest-prefix match on bit tries).

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
