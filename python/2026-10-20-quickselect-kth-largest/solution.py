"""k-th largest element via randomized quickselect (average O(n))."""

from __future__ import annotations

import heapq
import random


def kth_largest(nums: list[int], k: int, rng: random.Random | None = None) -> int:
    """Return the k-th largest value (1-based). Does not modify `nums`."""
    if not 1 <= k <= len(nums):
        raise ValueError("k out of range")
    rng = rng or random.Random()
    a = list(nums)
    target = len(a) - k  # index of the answer in ascending order
    lo, hi = 0, len(a) - 1
    while True:
        # Three-way partition around a random pivot: [< p | == p | > p].
        # Handling equals separately keeps many-duplicate inputs O(n).
        pivot = a[rng.randint(lo, hi)]
        lt, i, gt = lo, lo, hi
        while i <= gt:
            if a[i] < pivot:
                a[lt], a[i] = a[i], a[lt]
                lt += 1
                i += 1
            elif a[i] > pivot:
                a[i], a[gt] = a[gt], a[i]
                gt -= 1
            else:
                i += 1
        if target < lt:
            hi = lt - 1
        elif target > gt:
            lo = gt + 1
        else:
            return pivot


def kth_largest_heap(nums: list[int], k: int) -> int:
    """Alternative: min-heap of size k, O(n log k). Good for streams."""
    if not 1 <= k <= len(nums):
        raise ValueError("k out of range")
    heap = nums[:k]
    heapq.heapify(heap)
    for x in nums[k:]:
        if x > heap[0]:
            heapq.heapreplace(heap, x)
    return heap[0]
