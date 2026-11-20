import itertools
import random

import pytest

from solution import FenwickTree, count_inversions


def test_prefix_and_range_sums():
    ft = FenwickTree([3, 2, -1, 6, 5, 4, -3, 3, 7, 2, 3])
    assert ft.prefix_sum(0) == 0
    assert ft.prefix_sum(5) == 15
    assert ft.range_sum(3, 7) == 12
    assert ft.prefix_sum(len(ft)) == 31


def test_add_updates():
    ft = FenwickTree(5)
    ft.add(0, 5)
    ft.add(4, 2)
    ft.add(2, -1)
    assert [ft.prefix_sum(k) for k in range(6)] == [0, 5, 5, 4, 4, 6]


def test_bulk_build_matches_incremental():
    vals = list(range(1, 20))
    a = FenwickTree(vals)
    b = FenwickTree(len(vals))
    for i, v in enumerate(vals):
        b.add(i, v)
    assert a.tree == b.tree


def test_bounds():
    ft = FenwickTree(3)
    with pytest.raises(IndexError):
        ft.add(3, 1)
    with pytest.raises(IndexError):
        ft.prefix_sum(4)
    with pytest.raises(IndexError):
        ft.range_sum(2, 1)
    assert FenwickTree(0).prefix_sum(0) == 0


def test_lower_bound():
    ft = FenwickTree([1, 0, 2, 3, 0, 4])  # prefix: 1 1 3 6 6 10
    assert ft.lower_bound(0) == 0
    assert ft.lower_bound(1) == 1
    assert ft.lower_bound(2) == 3
    assert ft.lower_bound(6) == 4
    assert ft.lower_bound(7) == 6
    assert ft.lower_bound(11) == 7


def test_count_inversions():
    assert count_inversions([2, 4, 1, 3, 5]) == 3
    assert count_inversions([5, 4, 3, 2, 1]) == 10
    assert count_inversions([1, 1, 1]) == 0
    assert count_inversions([]) == 0


def test_randomized_against_list():
    rng = random.Random(5)
    vals = [rng.randint(0, 9) for _ in range(50)]
    ft = FenwickTree(vals)
    for _ in range(2000):
        if rng.random() < 0.4:
            i, d = rng.randrange(50), rng.randint(0, 5)
            vals[i] += d
            ft.add(i, d)
        else:
            lo = rng.randint(0, 50)
            hi = rng.randint(lo, 50)
            assert ft.range_sum(lo, hi) == sum(vals[lo:hi])
            t = rng.randint(0, sum(vals) + 2)
            prefix = list(itertools.accumulate([0] + vals))
            expected = next((k for k, p in enumerate(prefix) if p >= t), 51)
            assert ft.lower_bound(t) == expected


def test_inversions_brute_force():
    rng = random.Random(2)
    for _ in range(100):
        a = [rng.randint(0, 5) for _ in range(rng.randint(0, 15))]
        brute = sum(a[i] > a[j] for i in range(len(a)) for j in range(i + 1, len(a)))
        assert count_inversions(a) == brute
