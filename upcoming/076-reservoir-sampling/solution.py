"""Reservoir sampling: pick k items uniformly from a stream of unknown length
in one pass and O(k) memory."""

from __future__ import annotations

import heapq
import math
import random
from typing import Iterable, TypeVar

T = TypeVar("T")


def reservoir_sample(stream: Iterable[T], k: int, rng: random.Random | None = None) -> list[T]:
    """Algorithm R: every item ends up in the sample with probability k/n."""
    if k < 0:
        raise ValueError("k must be non-negative")
    rng = rng or random.Random()
    sample: list[T] = []
    for i, item in enumerate(stream):
        if i < k:
            sample.append(item)
        else:
            # Keep item i with probability k/(i+1), replacing a random slot.
            j = rng.randrange(i + 1)
            if j < k:
                sample[j] = item
    return sample


def reservoir_sample_skip(stream: Iterable[T], k: int, rng: random.Random | None = None) -> list[T]:
    """Algorithm L: same distribution, but jumps over items it won't keep,
    so it draws O(k log(n/k)) random numbers instead of O(n)."""
    if k < 0:
        raise ValueError("k must be non-negative")
    rng = rng or random.Random()
    it = iter(stream)
    sample = [x for _, x in zip(range(k), it)]
    if len(sample) < k or k == 0:
        return sample
    w = math.exp(math.log(rng.random()) / k)
    while True:
        skip = math.floor(math.log(rng.random()) / math.log(1 - w))
        for _ in range(skip):
            if next(it, _END) is _END:
                return sample
        item = next(it, _END)
        if item is _END:
            return sample
        sample[rng.randrange(k)] = item
        w *= math.exp(math.log(rng.random()) / k)


def weighted_sample(stream: Iterable[tuple[T, float]], k: int, rng: random.Random | None = None) -> list[T]:
    """A-Res (Efraimidis-Spirakis): weighted sampling without replacement.
    Each item gets key u^(1/w); keep the k largest keys in a min-heap."""
    if k < 0:
        raise ValueError("k must be non-negative")
    rng = rng or random.Random()
    heap: list[tuple[float, int, T]] = []
    for i, (item, weight) in enumerate(stream):
        if weight <= 0:
            raise ValueError("weights must be positive")
        key = rng.random() ** (1 / weight)
        if len(heap) < k:
            heapq.heappush(heap, (key, i, item))
        elif key > heap[0][0]:
            heapq.heapreplace(heap, (key, i, item))
    return [item for _, _, item in heap]


_END = object()
