# Word ladder (BFS with wildcard buckets)

**Problem:** Given `begin`, `end`, and a dictionary, find the shortest sequence of words from `begin` to `end` where each step changes exactly one letter and every word after `begin` is in the dictionary. Also return *all* shortest sequences (Word Ladder II).

## Approach
- Model it as an implicit unweighted graph: words are nodes, and words one letter apart share an edge. Shortest path in an unweighted graph means BFS.
- **Wildcard buckets:** index every word under `L` patterns (`hot` goes under `*ot`, `h*t`, `ho*`). Two words are neighbors exactly when they share a bucket, so you never compare all pairs.
- **Bidirectional BFS:** grow frontiers from both ends and always expand the smaller one. When a newly discovered word is already known to the other side, stitch the two parent chains together. On branching graphs this explores roughly `b^(d/2)` nodes on each side instead of `b^d`.
- **All shortest ladders:** run a level-by-level BFS that records *every* parent at the shortest depth (a DAG), stop after the level that reaches `end`, then DFS from `end` back through the parents.

## Complexity
| Aspect | Cost |
|--------|------|
| Build buckets | O(N·L²) (N words, L letters; string slicing is O(L)) |
| BFS | O(N·L²) |
| All ladders | O(N·L²) + O(output size) |
| Space | O(N·L²) for buckets |

## Interview talking points
- Neighbor generation alternative: try all 26 letters at each position, O(26·L²) per word. That wins when the dictionary is huge and words are short.
- Why mark visited when *enqueuing* and not when dequeuing? It avoids duplicate queue entries and keeps BFS linear.
- Word Ladder II pitfall: you must allow multiple parents at the same depth but never from a deeper level. A naive DFS over all paths is exponential.
- Bidirectional search applies to any search with a known goal (social graph "degrees of separation", puzzle solvers).
- Edge cases: `end` not in the dictionary, `begin` == `end`, different lengths.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
