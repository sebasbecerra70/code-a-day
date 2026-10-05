# Dutch national flag (three-way partition)

**Problem:** Given an array of 0s, 1s and 2s, sort it in one pass with O(1) extra space ("Sort Colors"). Generalize to partitioning any range around a pivot into `< | == | >`, and use that to build a quicksort that stays fast with many duplicate keys.

## Approach
Dijkstra's three-pointer partition keeps four regions:

```
[lo, lt)   < pivot
[lt, i)    == pivot
[i, gt)    not yet examined
[gt, hi)   > pivot
```
- `a[i] < pivot`: swap into the `<` region, advance `lt` and `i`.
- `a[i] > pivot`: swap with `a[gt−1]`, shrink `gt`, and **don't** advance `i`, because the element swapped in hasn't been examined.
- Equal: just advance `i`.

The loop ends when `i` meets `gt`. Sort Colors is the special case pivot = 1. The 3-way quicksort picks a random pivot, partitions, and recurses only on the `<` and `>` parts (the smaller one first, looping on the larger to bound stack depth).

## Complexity
| Function | Time | Space |
|----------|------|-------|
| threeWayPartition | O(n), one pass | O(1) |
| sortColors | O(n) | O(1) |
| quicksort3Way | O(n log n) expected; O(n · distinct keys) with few distinct keys | O(log n) stack |

## Interview talking points
- Counting sort (count 0s/1s/2s and rewrite) is also O(n) but takes two passes and doesn't work when elements carry satellite data.
- Classic Lomuto/Hoare quicksort goes quadratic on all-equal input; 3-way partitioning makes it linear.
- Not stable: equal elements can be reordered.
- Same pattern: move zeros to the end, partition by sign, segregate even/odd, and the partition step in quickselect.

## Run
From this folder: `g++ -std=c++17 -Wall -Wextra -o test test.cpp && ./test`
