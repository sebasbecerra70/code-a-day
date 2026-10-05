# Line diff via longest common subsequence

**Problem:** Given two versions of a file as arrays of lines, produce a minimal list of `equal` / `delete` / `insert` operations that turns the old version into the new one, and render it like `diff`.

## Approach
- The lines kept unchanged form a **longest common subsequence**; everything else is a deletion (from `a`) or insertion (from `b`). Maximizing the LCS minimizes the edits.
- Trim the common prefix and suffix first. Real diffs are usually small edits to big files, so this shrinks the DP drastically.
- Build the LCS table on *suffixes* (`t[i][j]` = LCS of `a[i:]`, `b[j:]`) so the reconstruction walks forward and emits ops in order: equal lines match diagonally; otherwise go in the direction that keeps the larger LCS, preferring deletes before inserts (as `diff` prints them).
- `formatDiff` prints ` `/`-`/`+` lines; `applyDiff` replays a diff against the old text and verifies it.

## Complexity
| Step | Time | Space |
|------|------|-------|
| diffLines | O(n·m) on the untrimmed middle | O(n·m) |
| applyDiff | O(n + m) | O(m) |

## Interview talking points
- Git/GNU diff use **Myers' algorithm**: O((n+m)·D) where D is the edit distance. Fast when files are similar, and O(n+m) space with the linear-space refinement.
- Patience diff and histogram diff anchor on unique lines to produce more human-friendly hunks.
- Hash lines to integers first so comparisons are O(1).
- Unified diff output groups changes into hunks with context lines (`@@ -l,s +l,s @@`).
- Same DP solves edit distance without substitution, and LCS-based merging (diff3) for three-way merges.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
