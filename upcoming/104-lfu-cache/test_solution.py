import random

import pytest

from solution import LFUCache


def test_leetcode_sequence():
    c = LFUCache(2)
    c.put(1, 1)
    c.put(2, 2)
    assert c.get(1) == 1  # freq(1)=2
    assert c.put(3, 3) == 2  # evicts 2 (lowest freq)
    assert c.get(2) is None
    assert c.get(3) == 3  # freq(3)=2
    assert c.put(4, 4) == 1  # 1 and 3 tie at freq 2 -> evict LRU (1)
    assert c.get(1) is None
    assert c.get(3) == 3
    assert c.get(4) == 4


def test_tie_broken_by_recency():
    c = LFUCache(3)
    for k in "abc":
        c.put(k, k)
    c.get("a")
    c.get("b")
    c.get("c")
    assert c.put("d", "d") == "a"


def test_update_counts_as_use_and_changes_value():
    c = LFUCache(2)
    c.put("x", 1)
    c.put("y", 2)
    c.put("x", 10)
    assert c.frequency("x") == 2
    assert c.put("z", 3) == "y"
    assert c.get("x") == 10


def test_zero_capacity_and_validation():
    c = LFUCache(0)
    assert c.put(1, 1) is None
    assert c.get(1) is None and len(c) == 0
    with pytest.raises(ValueError):
        LFUCache(-1)


def test_get_default_and_contains():
    c = LFUCache(1)
    assert c.get("missing", "dflt") == "dflt"
    c.put("k", None)
    assert "k" in c and c.get("k", "dflt") is None


def test_pop_updates_min_freq():
    c = LFUCache(3)
    c.put("a", 1)
    c.put("b", 2)
    c.get("b")
    c.put("c", 3)
    c.get("c")
    assert c.pop("a") == 1  # removes the only freq-1 key
    c.put("d", 4)
    c.get("d")
    c.get("d")
    assert c.put("e", 5) in {"b", "c"}
    assert c.put("f", 6) == "e"  # new keys are always the most evictable
    with pytest.raises(KeyError):
        c.pop("zzz")
    assert c.pop("zzz", None) is None


class NaiveLFU:
    """O(n) reference: track (freq, last_use) and scan for the victim."""

    def __init__(self, cap):
        self.cap, self.data, self.t = cap, {}, 0

    def get(self, k):
        if k not in self.data:
            return None
        v, f, _ = self.data[k]
        self.t += 1
        self.data[k] = (v, f + 1, self.t)
        return v

    def put(self, k, v):
        if self.cap == 0:
            return None
        self.t += 1
        if k in self.data:
            _, f, _ = self.data[k]
            self.data[k] = (v, f + 1, self.t)
            return None
        victim = None
        if len(self.data) >= self.cap:
            victim = min(self.data, key=lambda x: (self.data[x][1], self.data[x][2]))
            del self.data[victim]
        self.data[k] = (v, 1, self.t)
        return victim


def test_randomized_against_naive():
    rng = random.Random(5)
    for _ in range(50):
        cap = rng.randint(0, 5)
        fast, slow = LFUCache(cap), NaiveLFU(cap)
        for _ in range(300):
            k = rng.randint(0, 8)
            if rng.random() < 0.5:
                assert fast.get(k) == slow.get(k)
            else:
                v = rng.random()
                assert fast.put(k, v) == slow.put(k, v)
            assert len(fast) == len(slow.data)
