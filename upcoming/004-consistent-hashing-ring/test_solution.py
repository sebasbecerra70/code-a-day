import random
from collections import Counter

import pytest

from solution import HashRing


def test_empty_ring_raises():
    with pytest.raises(LookupError):
        HashRing().get_node("x")


def test_single_node_owns_everything():
    r = HashRing(["a"])
    assert {r.get_node(f"k{i}") for i in range(100)} == {"a"}


def test_deterministic():
    r1, r2 = HashRing(["a", "b", "c"]), HashRing(["c", "b", "a"])
    for i in range(200):
        assert r1.get_node(f"k{i}") == r2.get_node(f"k{i}")


def test_add_node_only_moves_keys_to_new_node():
    keys = [f"key-{i}" for i in range(5000)]
    r = HashRing(["a", "b", "c"])
    before = {k: r.get_node(k) for k in keys}
    r.add_node("d")
    moved = [k for k in keys if r.get_node(k) != before[k]]
    assert all(r.get_node(k) == "d" for k in moved)
    # Roughly 1/4 of keys should move; allow wide slack.
    assert 0.1 * len(keys) < len(moved) < 0.4 * len(keys)


def test_remove_node_only_moves_its_keys():
    keys = [f"key-{i}" for i in range(5000)]
    r = HashRing(["a", "b", "c", "d"])
    before = {k: r.get_node(k) for k in keys}
    r.remove_node("b")
    for k in keys:
        if before[k] != "b":
            assert r.get_node(k) == before[k]
        else:
            assert r.get_node(k) != "b"


def test_remove_then_add_restores_mapping():
    keys = [f"key-{i}" for i in range(1000)]
    r = HashRing(["a", "b", "c"])
    before = {k: r.get_node(k) for k in keys}
    r.remove_node("c")
    r.add_node("c")
    assert {k: r.get_node(k) for k in keys} == before


def test_balanced_distribution_with_virtual_nodes():
    r = HashRing([f"n{i}" for i in range(5)], replicas=200)
    counts = Counter(r.get_node(f"k{i}") for i in range(20000))
    for c in counts.values():
        assert 0.6 * 4000 < c < 1.4 * 4000


def test_add_duplicate_is_noop_and_remove_missing_raises():
    r = HashRing(["a"], replicas=10)
    r.add_node("a")
    assert len(r._keys) == 10
    with pytest.raises(KeyError):
        r.remove_node("zzz")
    with pytest.raises(ValueError):
        HashRing(replicas=0)


def test_matches_brute_force_lookup():
    rng = random.Random(7)
    r = HashRing(["a", "b", "c", "d"], replicas=20)
    from solution import _hash

    points = sorted((h, n) for h, n in r._owner.items())
    for _ in range(500):
        k = str(rng.random())
        h = _hash(k)
        expected = next((n for p, n in points if p > h), points[0][1])
        assert r.get_node(k) == expected
