"""Knuth-Morris-Pratt substring search in O(n + m)."""


def prefix_function(s: str) -> list[int]:
    """pi[i] = length of the longest proper prefix of s[:i+1] that is also its suffix."""
    pi = [0] * len(s)
    k = 0
    for i in range(1, len(s)):
        # Fall back through shorter borders until one can be extended.
        while k > 0 and s[i] != s[k]:
            k = pi[k - 1]
        if s[i] == s[k]:
            k += 1
        pi[i] = k
    return pi


def find_all(text: str, pattern: str) -> list[int]:
    """Start indices of every (possibly overlapping) occurrence of pattern."""
    if not pattern:
        return list(range(len(text) + 1))
    pi = prefix_function(pattern)
    out, k = [], 0
    for i, ch in enumerate(text):
        while k > 0 and ch != pattern[k]:
            k = pi[k - 1]
        if ch == pattern[k]:
            k += 1
        if k == len(pattern):
            out.append(i - k + 1)
            k = pi[k - 1]  # keep going to catch overlaps
    return out


def find_first(text: str, pattern: str) -> int:
    hits = find_all(text, pattern) if pattern else [0]
    return hits[0] if hits else -1


def shortest_period(s: str) -> int:
    """Length of the smallest p such that s is a prefix of (s[:p]) repeated."""
    return len(s) - prefix_function(s)[-1] if s else 0
