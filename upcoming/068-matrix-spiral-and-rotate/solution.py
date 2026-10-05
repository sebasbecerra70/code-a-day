"""Matrix traversal and transformation classics."""


def spiral_order(m: list[list[int]]) -> list[int]:
    """Clockwise spiral from the top-left, for any rectangular matrix."""
    out: list[int] = []
    if not m or not m[0]:
        return out
    top, bottom, left, right = 0, len(m) - 1, 0, len(m[0]) - 1
    while top <= bottom and left <= right:
        out.extend(m[top][c] for c in range(left, right + 1))
        top += 1
        out.extend(m[r][right] for r in range(top, bottom + 1))
        right -= 1
        if top <= bottom:  # a remaining row to walk back along
            out.extend(m[bottom][c] for c in range(right, left - 1, -1))
            bottom -= 1
        if left <= right:  # a remaining column to walk up
            out.extend(m[r][left] for r in range(bottom, top - 1, -1))
            left += 1
    return out


def spiral_matrix(n: int) -> list[list[int]]:
    """n x n matrix filled with 1..n^2 in spiral order (LeetCode 59)."""
    m = [[0] * n for _ in range(n)]
    r = c = 0
    dr, dc = 0, 1
    for k in range(1, n * n + 1):
        m[r][c] = k
        nr, nc = r + dr, c + dc
        if not (0 <= nr < n and 0 <= nc < n) or m[nr][nc]:
            dr, dc = dc, -dr  # turn right
            nr, nc = r + dr, c + dc
        r, c = nr, nc
    return m


def rotate_clockwise(m: list[list[int]]) -> None:
    """Rotate a square matrix 90 degrees clockwise in place: transpose, then reverse rows."""
    n = len(m)
    if any(len(row) != n for row in m):
        raise ValueError("matrix must be square")
    for i in range(n):
        for j in range(i + 1, n):
            m[i][j], m[j][i] = m[j][i], m[i][j]
    for row in m:
        row.reverse()


def rotate_layers(m: list[list[int]]) -> None:
    """Same rotation via four-way swaps, one ring at a time."""
    n = len(m)
    if any(len(row) != n for row in m):
        raise ValueError("matrix must be square")
    for layer in range(n // 2):
        first, last = layer, n - 1 - layer
        for i in range(first, last):
            off = i - first
            top = m[first][i]
            m[first][i] = m[last - off][first]  # left -> top
            m[last - off][first] = m[last][last - off]  # bottom -> left
            m[last][last - off] = m[i][last]  # right -> bottom
            m[i][last] = top  # top -> right
