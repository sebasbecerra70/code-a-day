# Matrix spiral order and in-place rotation

**Problem:** Three matrix classics: read a rectangular matrix in clockwise spiral order (LeetCode 54), generate an `n × n` spiral matrix (59), and rotate a square matrix 90° clockwise in place (48).

## Approach
- **Spiral order:** keep four boundaries (`top`, `bottom`, `left`, `right`). Walk the top row, right column, bottom row (reversed), left column (upward), shrinking a boundary after each. The two inner checks stop single remaining rows/columns from being read twice.
- **Spiral generation:** walk with a direction vector and turn right (`(dr, dc) → (dc, -dr)`) whenever the next cell is off the grid or already filled.
- **Rotate (transpose + reverse):** transposing swaps across the main diagonal; reversing each row then gives the clockwise rotation.
- **Rotate (layers):** for each ring, rotate four cells at a time with one temp: left→top, bottom→left, right→bottom, top→right.

## Complexity
| Function | Time | Space |
|----------|------|-------|
| spiral_order | O(r·c) | O(1) extra (besides output) |
| spiral_matrix | O(n²) | O(n²) output |
| rotate_* | O(n²) | O(1) |

## Interview talking points
- Counter-clockwise rotation: transpose then reverse columns (or reverse rows first, then transpose). 180° = reverse rows and each row.
- Python one-liner reference: `list(zip(*m[::-1]))`, but it allocates a new matrix.
- Off-by-one traps in spiral order: single row, single column, and odd-sized centers. Test them explicitly.
- Non-square rotation can't be in place without index cycling; it changes shape.
- Image processing and game boards (Tetris piece rotation) are real uses.

## Run
From this folder: `python -m pytest -q`
