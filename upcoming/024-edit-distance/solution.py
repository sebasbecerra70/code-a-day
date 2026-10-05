"""Levenshtein edit distance: min insertions, deletions, substitutions to turn a into b."""


def edit_distance(a: str, b: str) -> int:
    """Two-row DP. O(len(a)*len(b)) time, O(min(len)) space."""
    if len(a) < len(b):
        a, b = b, a  # keep the row as short as possible
    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        cur = [i] + [0] * len(b)
        for j, cb in enumerate(b, 1):
            cur[j] = min(
                prev[j] + 1,  # delete ca
                cur[j - 1] + 1,  # insert cb
                prev[j - 1] + (ca != cb),  # substitute (free if equal)
            )
        prev = cur
    return prev[-1]


def edit_script(a: str, b: str) -> list[tuple[str, str, str]]:
    """Full table + backtrack. Returns ops as (op, from_char, to_char) where
    op is 'keep', 'sub', 'del', or 'ins'."""
    n, m = len(a), len(b)
    dp = [[0] * (m + 1) for _ in range(n + 1)]
    for i in range(n + 1):
        dp[i][0] = i
    for j in range(m + 1):
        dp[0][j] = j
    for i in range(1, n + 1):
        for j in range(1, m + 1):
            dp[i][j] = min(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + (a[i - 1] != b[j - 1]),
            )
    ops = []
    i, j = n, m
    while i > 0 or j > 0:
        if i > 0 and j > 0 and dp[i][j] == dp[i - 1][j - 1] + (a[i - 1] != b[j - 1]):
            ops.append(("keep" if a[i - 1] == b[j - 1] else "sub", a[i - 1], b[j - 1]))
            i, j = i - 1, j - 1
        elif i > 0 and dp[i][j] == dp[i - 1][j] + 1:
            ops.append(("del", a[i - 1], ""))
            i -= 1
        else:
            ops.append(("ins", "", b[j - 1]))
            j -= 1
    return ops[::-1]


def apply_script(a: str, ops: list[tuple[str, str, str]]) -> str:
    """Replays an edit script against `a` (used to verify scripts)."""
    out, i = [], 0
    for op, frm, to in ops:
        if op in ("keep", "sub", "del"):
            assert a[i] == frm
            i += 1
        if op in ("keep", "sub", "ins"):
            out.append(to)
    assert i == len(a)
    return "".join(out)
