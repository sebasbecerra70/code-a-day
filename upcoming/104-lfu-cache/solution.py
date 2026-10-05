"""Least-frequently-used cache with O(1) get/put.

Each frequency has its own bucket of keys in LRU order (an OrderedDict), and
`min_freq` tracks the smallest non-empty bucket, so the eviction victim (lowest
frequency, then least recently used) is found in O(1).
"""

from __future__ import annotations

from collections import OrderedDict, defaultdict
from typing import Generic, Hashable, TypeVar

K = TypeVar("K", bound=Hashable)
V = TypeVar("V")

_MISSING = object()


class LFUCache(Generic[K, V]):
    def __init__(self, capacity: int):
        if capacity < 0:
            raise ValueError("capacity must be >= 0")
        self.capacity = capacity
        self._values: dict[K, V] = {}
        self._freq: dict[K, int] = {}
        self._buckets: defaultdict[int, OrderedDict[K, None]] = defaultdict(OrderedDict)
        self._min_freq = 0

    def __len__(self) -> int:
        return len(self._values)

    def __contains__(self, key: object) -> bool:
        return key in self._values

    def _touch(self, key: K) -> None:
        """Moves key from bucket f to bucket f+1 (most-recent end)."""
        f = self._freq[key]
        bucket = self._buckets[f]
        del bucket[key]
        if not bucket:
            del self._buckets[f]
            if self._min_freq == f:
                self._min_freq = f + 1
        self._freq[key] = f + 1
        self._buckets[f + 1][key] = None

    def get(self, key: K, default: V | None = None) -> V | None:
        if key not in self._values:
            return default
        self._touch(key)
        return self._values[key]

    def put(self, key: K, value: V) -> K | None:
        """Inserts or updates. Returns the evicted key, if any."""
        if self.capacity == 0:
            return None
        if key in self._values:
            self._values[key] = value
            self._touch(key)  # an update counts as a use
            return None
        evicted = None
        if len(self._values) >= self.capacity:
            bucket = self._buckets[self._min_freq]
            evicted, _ = bucket.popitem(last=False)  # LRU among the least frequent
            if not bucket:
                del self._buckets[self._min_freq]
            del self._values[evicted]
            del self._freq[evicted]
        self._values[key] = value
        self._freq[key] = 1
        self._buckets[1][key] = None
        self._min_freq = 1  # a new key always has the minimum frequency
        return evicted

    def pop(self, key: K, default: object = _MISSING) -> V:
        if key not in self._values:
            if default is _MISSING:
                raise KeyError(key)
            return default  # type: ignore[return-value]
        f = self._freq.pop(key)
        bucket = self._buckets[f]
        del bucket[key]
        if not bucket:
            del self._buckets[f]
            if self._min_freq == f:
                # Rare O(F) rescan; only explicit deletes need it.
                self._min_freq = min(self._buckets, default=0)
        return self._values.pop(key)

    def frequency(self, key: K) -> int:
        return self._freq.get(key, 0)
