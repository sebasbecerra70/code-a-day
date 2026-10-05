"""3Sum family solved with sorting + two pointers."""

from __future__ import annotations


def three_sum(nums: list[int], target: int = 0) -> list[list[int]]:
    """All unique triplets (as sorted lists) summing to target."""
    a = sorted(nums)
    n = len(a)
    out = []
    for i in range(n - 2):
        if i > 0 and a[i] == a[i - 1]:
            continue  # same first value => same triplets
        if a[i] + a[i + 1] + a[i + 2] > target:
            break  # smallest possible sum already too big
        if a[i] + a[n - 2] + a[n - 1] < target:
            continue  # largest possible sum with a[i] too small
        lo, hi = i + 1, n - 1
        while lo < hi:
            s = a[i] + a[lo] + a[hi]
            if s < target:
                lo += 1
            elif s > target:
                hi -= 1
            else:
                out.append([a[i], a[lo], a[hi]])
                lo += 1
                hi -= 1
                while lo < hi and a[lo] == a[lo - 1]:
                    lo += 1  # skip duplicate second values
    return out


def three_sum_closest(nums: list[int], target: int) -> int:
    """Sum of the triplet closest to target (LeetCode 16)."""
    if len(nums) < 3:
        raise ValueError("need at least three numbers")
    a = sorted(nums)
    best = a[0] + a[1] + a[2]
    for i in range(len(a) - 2):
        lo, hi = i + 1, len(a) - 1
        while lo < hi:
            s = a[i] + a[lo] + a[hi]
            if abs(s - target) < abs(best - target):
                best = s
            if s < target:
                lo += 1
            elif s > target:
                hi -= 1
            else:
                return s
    return best


def two_sum_sorted(a: list[int], target: int) -> tuple[int, int] | None:
    """Indices (i < j) in a sorted list with a[i] + a[j] == target."""
    lo, hi = 0, len(a) - 1
    while lo < hi:
        s = a[lo] + a[hi]
        if s == target:
            return lo, hi
        if s < target:
            lo += 1
        else:
            hi -= 1
    return None
