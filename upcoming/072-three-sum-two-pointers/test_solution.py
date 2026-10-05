import itertools
import random

import pytest

from solution import three_sum, three_sum_closest, two_sum_sorted


def brute_three_sum(nums, target=0):
    return sorted({tuple(sorted(t)) for t in itertools.combinations(nums, 3) if sum(t) == target})


def test_classic():
    assert three_sum([-1, 0, 1, 2, -1, -4]) == [[-1, -1, 2], [-1, 0, 1]]


def test_no_triplets_and_short_input():
    assert three_sum([0, 1, 1]) == []
    assert three_sum([]) == []
    assert three_sum([0, 0]) == []


def test_all_zeros_deduplicated():
    assert three_sum([0] * 10) == [[0, 0, 0]]


def test_nonzero_target():
    assert three_sum([1, 2, 3, 4, 5], 9) == [[1, 3, 5], [2, 3, 4]]


def test_three_sum_closest():
    assert three_sum_closest([-1, 2, 1, -4], 1) == 2
    assert three_sum_closest([0, 0, 0], 1) == 0
    assert three_sum_closest([1, 1, 1, 0], 100) == 3
    with pytest.raises(ValueError):
        three_sum_closest([1, 2], 3)


def test_two_sum_sorted():
    assert two_sum_sorted([2, 7, 11, 15], 9) == (0, 1)
    assert two_sum_sorted([1, 2, 3], 7) is None
    assert two_sum_sorted([], 0) is None


def test_randomized_against_brute_force():
    rng = random.Random(10)
    for _ in range(300):
        nums = [rng.randint(-6, 6) for _ in range(rng.randint(0, 12))]
        t = rng.randint(-3, 3)
        assert [tuple(x) for x in three_sum(nums, t)] == brute_three_sum(nums, t)
        if len(nums) >= 3:
            sums = {sum(c) for c in itertools.combinations(nums, 3)}
            best = min(abs(s - t) for s in sums)
            assert abs(three_sum_closest(nums, t) - t) == best
