"""Sudoku solver: backtracking with bitmask candidates and the
minimum-remaining-values (MRV) heuristic."""

from __future__ import annotations

ALL = 0x3FE  # bits 1..9 set


def _box(r: int, c: int) -> int:
    return (r // 3) * 3 + c // 3


def parse(puzzle: str) -> list[list[int]]:
    """81 chars of 1-9 with 0 or '.' for blanks (whitespace ignored)."""
    chars = [ch for ch in puzzle if not ch.isspace()]
    if len(chars) != 81 or any(ch not in "0123456789." for ch in chars):
        raise ValueError("puzzle must have 81 cells of 1-9, 0 or '.'")
    vals = [0 if ch == "." else int(ch) for ch in chars]
    return [vals[r * 9 : r * 9 + 9] for r in range(9)]


def solve(grid: list[list[int]]) -> list[list[int]] | None:
    """Returns a solved copy, or None if unsolvable or the givens conflict."""
    g = [row[:] for row in grid]
    rows, cols, boxes = [0] * 9, [0] * 9, [0] * 9
    empty = []
    for r in range(9):
        for c in range(9):
            v = g[r][c]
            if v == 0:
                empty.append((r, c))
                continue
            bit = 1 << v
            if rows[r] & bit or cols[c] & bit or boxes[_box(r, c)] & bit:
                return None  # givens already conflict
            rows[r] |= bit
            cols[c] |= bit
            boxes[_box(r, c)] |= bit

    def backtrack() -> bool:
        if not empty:
            return True
        # MRV: branch on the empty cell with the fewest candidates.
        best_i, best_cand, best_count = -1, 0, 10
        for i, (r, c) in enumerate(empty):
            cand = ALL & ~(rows[r] | cols[c] | boxes[_box(r, c)])
            n = bin(cand).count("1")
            if n < best_count:
                best_i, best_cand, best_count = i, cand, n
                if n <= 1:
                    break
        if best_count == 0:
            return False  # dead end
        r, c = empty[best_i]
        empty[best_i] = empty[-1]
        empty.pop()
        b = _box(r, c)
        cand = best_cand
        while cand:
            bit = cand & -cand
            cand ^= bit
            rows[r] |= bit
            cols[c] |= bit
            boxes[b] |= bit
            g[r][c] = bit.bit_length() - 1
            if backtrack():
                return True
            rows[r] ^= bit
            cols[c] ^= bit
            boxes[b] ^= bit
        g[r][c] = 0
        empty.append((r, c))
        empty[best_i], empty[-1] = empty[-1], empty[best_i]
        return False

    return g if backtrack() else None


def is_valid_solution(grid: list[list[int]]) -> bool:
    want = set(range(1, 10))
    for i in range(9):
        if set(grid[i]) != want or {grid[r][i] for r in range(9)} != want:
            return False
    for br in range(0, 9, 3):
        for bc in range(0, 9, 3):
            if {grid[br + r][bc + c] for r in range(3) for c in range(3)} != want:
                return False
    return True
