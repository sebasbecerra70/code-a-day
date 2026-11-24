"""N-Queens: place n queens on an n x n board so none attack each other."""


def solve_n_queens(n: int) -> list[list[str]]:
    """All solutions as board drawings ('Q' and '.'), in lexicographic column order."""
    if n < 0:
        raise ValueError("n must be non-negative")
    cols: set[int] = set()
    diag: set[int] = set()  # r - c is constant along a "\" diagonal
    anti: set[int] = set()  # r + c is constant along a "/" diagonal
    placement: list[int] = []  # placement[r] = column of the queen in row r
    out: list[list[str]] = []

    def backtrack(r: int) -> None:
        if r == n:
            out.append(["." * c + "Q" + "." * (n - c - 1) for c in placement])
            return
        for c in range(n):
            if c in cols or (r - c) in diag or (r + c) in anti:
                continue
            cols.add(c)
            diag.add(r - c)
            anti.add(r + c)
            placement.append(c)
            backtrack(r + 1)
            placement.pop()
            cols.remove(c)
            diag.remove(r - c)
            anti.remove(r + c)

    backtrack(0)
    return out


def count_n_queens(n: int) -> int:
    """Counts solutions with bitmasks: each row's free squares in one integer."""
    if n < 0:
        raise ValueError("n must be non-negative")
    full = (1 << n) - 1

    def go(cols: int, diag: int, anti: int) -> int:
        if cols == full:
            return 1
        total = 0
        free = full & ~(cols | diag | anti)
        while free:
            bit = free & -free  # lowest free column
            free ^= bit
            # Diagonal attacks shift one column per row going down.
            total += go(cols | bit, ((diag | bit) << 1) & full, (anti | bit) >> 1)
        return total

    return go(0, 0, 0)
