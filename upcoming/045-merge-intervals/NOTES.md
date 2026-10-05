# Merge, insert, and intersect intervals

**Problem:** A family of interval problems on closed intervals `[start, end]`: merge overlapping intervals (LeetCode 56), insert a new interval into a sorted list (57), intersect two interval lists (986), and find the minimum removals to make intervals non-overlapping (435).

## Approach
- **Merge:** sort by start; sweep and either extend the last output interval (`start <= last.end`) or start a new one.
- **Insert:** the list is already sorted and disjoint, so do it in one O(n) pass with three phases: copy intervals that end before the new one, absorb all that overlap it, copy the rest.
- **Intersect:** two pointers. The overlap of the current pair is `[max(starts), min(ends)]` if non-empty; then advance whichever interval ends first.
- **Min removals:** classic greedy. Sort by end and keep each interval that starts after the last kept one ends (same as max non-overlapping activities).
- Tests cross-check against a brute-force "set of covered points" model on doubled coordinates.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| merge | O(n log n) | O(n) |
| insert | O(n) | O(n) |
| intersect | O(n + m) | O(n + m) |
| minRemovalsToNonOverlap | O(n log n) | O(n) |

## Interview talking points
- Closed vs. half-open intervals changes whether touching intervals merge (`<=` vs. `<`). Clarify early.
- Why sort by end for the greedy? Exchange argument: the earliest-ending interval never hurts the remaining choices.
- For many dynamic inserts and queries, use a balanced BST / sorted map keyed by start, or an interval tree.
- Related: meeting rooms (sweep line with a heap), calendar booking, range coverage.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
