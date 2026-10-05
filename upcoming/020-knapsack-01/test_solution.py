import itertools
import random

import pytest

from solution import knapsack_items, knapsack_value


def test_classic_example():
    w, v = [1, 3, 4, 5], [1, 4, 5, 7]
    assert knapsack_value(w, v, 7) == 9
    best, items = knapsack_items(w, v, 7)
    assert best == 9
    assert sum(w[i] for i in items) <= 7
    assert sum(v[i] for i in items) == 9


def test_empty_and_zero_capacity():
    assert knapsack_value([], [], 10) == 0
    assert knapsack_value([1, 2], [5, 6], 0) == 0
    assert knapsack_items([], [], 0) == (0, [])


def test_nothing_fits():
    assert knapsack_items([5, 6], [10, 20], 4) == (0, [])


def test_everything_fits():
    assert knapsack_items([1, 2, 3], [1, 2, 3], 100) == (6, [0, 1, 2])


def test_each_item_used_once():
    # An unbounded knapsack would take item 0 ten times for 100.
    assert knapsack_value([1], [10], 10) == 10


def test_zero_weight_item_always_taken():
    assert knapsack_items([0, 5], [3, 4], 4) == (3, [0])


def test_greedy_by_ratio_fails_but_dp_succeeds():
    # Ratio greedy picks item 0 (6/1) then item 1 (10/2) = 16; optimum is 22.
    assert knapsack_value([1, 2, 3], [6, 10, 12], 5) == 22


def test_validation():
    with pytest.raises(ValueError):
        knapsack_value([1], [1, 2], 3)
    with pytest.raises(ValueError):
        knapsack_value([1], [1], -1)
    with pytest.raises(ValueError):
        knapsack_items([-1], [1], 3)


def test_randomized_against_brute_force():
    rng = random.Random(11)
    for _ in range(200):
        n = rng.randint(0, 8)
        w = [rng.randint(0, 10) for _ in range(n)]
        v = [rng.randint(0, 20) for _ in range(n)]
        cap = rng.randint(0, 25)
        brute = max(
            sum(v[i] for i in combo)
            for r in range(n + 1)
            for combo in itertools.combinations(range(n), r)
            if sum(w[i] for i in combo) <= cap
        )
        assert knapsack_value(w, v, cap) == brute
        best, items = knapsack_items(w, v, cap)
        assert best == brute
        assert len(set(items)) == len(items)
        assert sum(w[i] for i in items) <= cap
        assert sum(v[i] for i in items) == brute
