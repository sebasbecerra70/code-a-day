"""Longest strictly increasing subsequence in O(n log n) with reconstruction."""

from bisect import bisect_left


def lis_length(nums: list[int]) -> int:
    # tails[k] = smallest possible tail of an increasing subsequence of length k+1.
    tails: list[int] = []
    for x in nums:
        i = bisect_left(tails, x)  # bisect_left => strictly increasing
        if i == len(tails):
            tails.append(x)
        else:
            tails[i] = x
    return len(tails)


def lis(nums: list[int]) -> list[int]:
    """Returns one longest strictly increasing subsequence."""
    tails: list[int] = []  # values, kept sorted for bisect
    tail_idx: list[int] = []  # index in nums of each tails entry
    parent = [-1] * len(nums)  # predecessor index in the best chain ending here
    for i, x in enumerate(nums):
        k = bisect_left(tails, x)
        if k > 0:
            parent[i] = tail_idx[k - 1]
        if k == len(tails):
            tails.append(x)
            tail_idx.append(i)
        else:
            tails[k] = x
            tail_idx[k] = i
    out = []
    i = tail_idx[-1] if tail_idx else -1
    while i != -1:
        out.append(nums[i])
        i = parent[i]
    return out[::-1]


def lis_quadratic(nums: list[int]) -> int:
    """O(n^2) DP: dp[i] = LIS ending at i. Simple reference implementation."""
    dp = [1] * len(nums)
    for i in range(len(nums)):
        for j in range(i):
            if nums[j] < nums[i]:
                dp[i] = max(dp[i], dp[j] + 1)
    return max(dp, default=0)
