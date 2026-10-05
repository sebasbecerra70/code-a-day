# K-way merge (min-heap over sorted sources)

**Problem:** Merge k sorted sequences (arrays, generators, file streams) into a single sorted output. Bonus: find the smallest range that contains at least one element from each of k sorted lists.

## Approach
- Put the first element of every non-empty source into a min-heap, tagged with its source index and iterator.
- Repeatedly emit the heap's minimum, then replace it with the next element from the same source (or drop it if that source is exhausted). A single "replace top + sift down" is cheaper than pop then push.
- Ties are broken by source index, which makes the merge **stable**.
- It's a generator over iterables, so it is lazy: it can merge infinite streams or files too big for memory, holding only k items at a time.
- **Smallest range:** the heap holds one pointer per list; the current window is `[heap min, running max]`. Advance the list that owns the minimum, since that's the only move that can shrink the window. Stop when any list runs out.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(N log k) for N total elements |
| Space | O(k) extra (plus output if materialized) |
| Smallest range | O(N log k) time, O(k) space |

## Interview talking points
- Alternatives: concat + sort is O(N log N); pairwise divide-and-conquer merging is also O(N log k) but not streaming-friendly.
- This is the merge phase of **external sort**: sort chunks that fit in RAM, write them out, then k-way merge the runs.
- LSM-tree databases (LevelDB, RocksDB) k-way merge SSTables during compaction.
- "Merge k sorted linked lists" is the same algorithm with list nodes as heap entries.
- With k ≤ ~8 a linear scan for the minimum can beat a heap due to constants. A tournament (loser) tree reduces comparisons further.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
