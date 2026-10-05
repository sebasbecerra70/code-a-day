# Binary search variants (bounds, rotated, answer search)

**Problem:** Implement the binary search family without off-by-one bugs: lower/upper bound, first/last occurrence, search in a rotated sorted array, find the rotation point, and "binary search on the answer" over a monotone predicate.

## Approach
- Use one invariant everywhere: search the half-open range `[lo, hi)` and keep "answer is in `[lo, hi]`". Loop `while (lo < hi)`; set `hi = mid` when `mid` could be the answer, `lo = mid + 1` when it can't. The loop ends at the answer.
- `lowerBound` = first `a[i] >= x`; `upperBound` = first `a[i] > x`. Everything else builds on them: `searchRange = [lowerBound, upperBound - 1]`.
- **Rotated array:** at each step one half is sorted; check whether `x` lies in that half's range to decide which way to go.
- **Rotation point:** compare `a[mid]` with `a[hi]`; if bigger, the minimum is to the right.
- **firstTrue(lo, hi, pred):** the general pattern. Any "minimum capacity / speed / time such that it's feasible" problem is a monotone predicate.
- `(lo + hi) >>> 1` avoids overflow for array indices.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| all variants | O(log n) (× predicate cost for firstTrue) | O(1) |

## Interview talking points
- Overflow: `(lo + hi) / 2` overflows in fixed-width languages; use `lo + (hi - lo) / 2`.
- With duplicates, rotated search degrades to O(n) worst case (`[1,1,1,0,1]`): you can't tell which half is sorted.
- Recognizing "binary search on the answer": Koko eating bananas, ship within D days, split array largest sum, minimum time to finish jobs.
- Floating-point answers: loop a fixed number of iterations instead of `lo < hi`.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
