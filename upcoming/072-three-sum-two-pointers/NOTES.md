# 3Sum with two pointers

**Problem:** Find all unique triplets in an array that sum to zero (LeetCode 15), plus the closest-sum variant (16) and the sorted two-sum building block (167).

## Approach
- Sort. Fix the first element `a[i]`, then solve two-sum on `a[i+1:]` with two pointers: if the sum is too small move `lo` right, too big move `hi` left.
- **Deduplication** without a set: skip `a[i]` equal to `a[i-1]`, and after recording a triplet skip repeated `a[lo]` values. Output comes out sorted and unique.
- **Pruning:** if the three smallest candidates already exceed the target, stop; if `a[i]` with the two largest is still too small, skip this `i`.
- **Closest:** same scan, tracking the sum with the smallest `|s - target|`, returning early on an exact hit.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| three_sum / three_sum_closest | O(n²) | O(n) for the sorted copy (O(1) if sorting in place) |
| two_sum_sorted | O(n) | O(1) |

## Interview talking points
- Why two pointers work: in a sorted array, moving `lo` right only increases the sum and moving `hi` left only decreases it, so no pair is skipped.
- Hash-set alternative is also O(n²) but deduplication is messier.
- Generalizes to k-sum: recurse down to two-sum, O(n^{k-1}).
- 3SUM is a famous problem in complexity theory: no strongly subquadratic algorithm is known, and many geometry problems reduce to it.

## Run
From this folder: `python -m pytest -q`
