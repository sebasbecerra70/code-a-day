import bisect
import random

import pytest

from solution import SkipList


def test_insert_get_and_order():
    s = SkipList(seed=1)
    for k in [5, 1, 9, 3, 7]:
        assert s.insert(k, str(k))
    assert list(s) == [1, 3, 5, 7, 9]
    assert s.get(7) == "7" and s.get(4) is None and s.get(4, "x") == "x"
    assert len(s) == 5 and 3 in s and 4 not in s


def test_update_existing_key():
    s = SkipList(seed=2)
    assert s.insert("a", 1)
    assert not s.insert("a", 2)
    assert len(s) == 1 and s.get("a") == 2


def test_delete():
    s = SkipList(seed=3)
    for k in range(10):
        s.insert(k)
    assert s.delete(0) and s.delete(9) and s.delete(5)
    assert not s.delete(5) and not s.delete(100)
    assert list(s) == [1, 2, 3, 4, 6, 7, 8]
    for k in list(s):
        s.delete(k)
    assert len(s) == 0 and list(s) == [] and s._level == 1


def test_empty_list_queries():
    s = SkipList()
    assert s.get(1) is None and s.floor(1) is None and s.ceiling(1) is None
    assert s.range(0, 10) == [] and not s.delete(1)


def test_range_floor_ceiling():
    s = SkipList(seed=4)
    for k in [10, 20, 30, 40]:
        s.insert(k, k * 2)
    assert s.range(15, 40) == [(20, 40), (30, 60)]
    assert s.range(40, 10) == []
    assert s.floor(25) == 20 and s.floor(20) == 20 and s.floor(5) is None
    assert s.ceiling(25) == 30 and s.ceiling(40) == 40 and s.ceiling(41) is None


def test_invalid_p():
    with pytest.raises(ValueError):
        SkipList(p=1.0)


def test_levels_stay_logarithmic():
    s = SkipList(seed=5)
    for k in range(20_000):
        s.insert(k)
    assert s._level <= 30  # expected ~log2(20000) ≈ 14


def test_randomized_against_sorted_list():
    rng = random.Random(6)
    s = SkipList(p=0.25, seed=7)
    ref: dict[int, int] = {}
    for _ in range(3000):
        k = rng.randint(0, 200)
        op = rng.random()
        if op < 0.5:
            assert s.insert(k, k * 3) == (k not in ref)
            ref[k] = k * 3
        elif op < 0.8:
            assert s.delete(k) == (ref.pop(k, None) is not None)
        else:
            keys = sorted(ref)
            i = bisect.bisect_right(keys, k)
            assert s.floor(k) == (keys[i - 1] if i else None)
            j = bisect.bisect_left(keys, k)
            assert s.ceiling(k) == (keys[j] if j < len(keys) else None)
        assert len(s) == len(ref)
    assert list(s.items()) == sorted(ref.items())
