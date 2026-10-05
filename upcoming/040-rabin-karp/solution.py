"""Rabin-Karp substring search with a polynomial rolling hash."""

BASE = 256
MOD = (1 << 61) - 1  # Mersenne prime: tiny collision probability


def find_all(text: str, pattern: str) -> list[int]:
    """All start indices of pattern in text (overlapping allowed)."""
    n, m = len(text), len(pattern)
    if m == 0:
        return list(range(n + 1))
    if m > n:
        return []
    high = pow(BASE, m - 1, MOD)  # weight of the character leaving the window
    ph = th = 0
    for i in range(m):
        ph = (ph * BASE + ord(pattern[i])) % MOD
        th = (th * BASE + ord(text[i])) % MOD
    out = []
    for i in range(n - m + 1):
        # Hashes match => verify, so a collision can never cause a wrong answer.
        if ph == th and text[i : i + m] == pattern:
            out.append(i)
        if i + m < n:
            th = ((th - ord(text[i]) * high) * BASE + ord(text[i + m])) % MOD
    return out


def find_any(text: str, patterns: list[str]) -> dict[str, list[int]]:
    """Multi-pattern search: one pass per distinct pattern length, set lookup per window."""
    result: dict[str, list[int]] = {p: [] for p in patterns}
    by_len: dict[int, dict[int, list[str]]] = {}
    for p in set(patterns):
        if not p:
            result[p] = list(range(len(text) + 1))
            continue
        by_len.setdefault(len(p), {}).setdefault(_hash(p), []).append(p)
    for m, table in by_len.items():
        if m > len(text):
            continue
        high = pow(BASE, m - 1, MOD)
        th = _hash(text[:m])
        for i in range(len(text) - m + 1):
            for p in table.get(th, ()):
                if text[i : i + m] == p:
                    result[p].append(i)
            if i + m < len(text):
                th = ((th - ord(text[i]) * high) * BASE + ord(text[i + m])) % MOD
    return result


def longest_repeated_substring(s: str) -> str:
    """Binary search on length + rolling hash to test 'some substring of length L repeats'."""

    def repeated(length: int) -> int:
        if length == 0:
            return 0
        high = pow(BASE, length - 1, MOD)
        h = _hash(s[:length])
        seen = {h: [0]}
        for i in range(1, len(s) - length + 1):
            h = ((h - ord(s[i - 1]) * high) * BASE + ord(s[i + length - 1])) % MOD
            for j in seen.get(h, ()):
                if s[j : j + length] == s[i : i + length]:
                    return i
            seen.setdefault(h, []).append(i)
        return -1

    lo, hi, start = 0, len(s) - 1, 0
    while lo < hi:
        mid = (lo + hi + 1) // 2
        i = repeated(mid)
        if i >= 0:
            lo, start = mid, i
        else:
            hi = mid - 1
    return s[start : start + lo] if lo > 0 else ""


def _hash(s: str) -> int:
    h = 0
    for ch in s:
        h = (h * BASE + ord(ch)) % MOD
    return h
