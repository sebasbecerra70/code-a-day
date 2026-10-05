import random
from functools import lru_cache

import pytest

from solution import count_ways, min_coins, min_coins_combo


def test_examples():
    assert min_coins([1, 2, 5], 11) == 3
    assert min_coins([2], 3) == -1
    assert min_coins([1], 0) == 0
    assert count_ways([1, 2, 5], 5) == 4
    assert count_ways([2], 3) == 0
    assert count_ways([10], 10) == 1


def test_zero_amount_and_no_coins():
    assert min_coins([], 0) == 0
    assert min_coins([], 5) == -1
    assert count_ways([], 0) == 1
    assert min_coins_combo([3], 0) == []


def test_greedy_counterexample():
    # Greedy takes 4+1+1 (3 coins); optimum is 3+3.
    assert min_coins([1, 3, 4], 6) == 2
    assert min_coins_combo([1, 3, 4], 6) == [3, 3]


def test_combo_impossible():
    assert min_coins_combo([5, 10], 3) is None


def test_duplicate_coins_do_not_double_count_ways():
    assert count_ways([1, 1, 2], 3) == count_ways([1, 2], 3) == 2


def test_large_amount():
    assert count_ways([1, 2, 5, 10, 20, 50, 100, 200], 200) == 73682  # Project Euler 31
    assert min_coins([186, 419, 83, 408], 6249) == 20


def test_validation():
    with pytest.raises(ValueError):
        min_coins([1], -1)
    with pytest.raises(ValueError):
        count_ways([0, 1], 3)


def test_randomized_against_brute_force():
    rng = random.Random(6)
    for _ in range(200):
        coins = sorted({rng.randint(1, 9) for _ in range(rng.randint(1, 4))})
        amount = rng.randint(0, 30)

        @lru_cache(None)
        def best(a):
            if a == 0:
                return 0
            opts = [best(a - c) for c in coins if c <= a]
            opts = [o for o in opts if o >= 0]
            return min(opts) + 1 if opts else -1

        @lru_cache(None)
        def ways(a, i):
            if a == 0:
                return 1
            if i == len(coins):
                return 0
            return ways(a, i + 1) + (ways(a - coins[i], i) if coins[i] <= a else 0)

        assert min_coins(coins, amount) == best(amount)
        assert count_ways(coins, amount) == ways(amount, 0)
        combo = min_coins_combo(coins, amount)
        if best(amount) == -1:
            assert combo is None
        else:
            assert sum(combo) == amount and len(combo) == best(amount)
            assert all(c in coins for c in combo)
