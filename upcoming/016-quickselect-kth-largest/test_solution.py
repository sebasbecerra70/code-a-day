import random

import pytest

from solution import kth_largest, kth_largest_heap

FUNCS = [kth_largest, kth_largest_heap]


@pytest.mark.parametrize("f", FUNCS)
def test_examples(f):
    assert f([3, 2, 1, 5, 6, 4], 2) == 5
    assert f([3, 2, 3, 1, 2, 4, 5, 5, 6], 4) == 4


@pytest.mark.parametrize("f", FUNCS)
def test_single_and_extremes(f):
    assert f([7], 1) == 7
    data = [4, -1, 9, 0]
    assert f(data, 1) == 9
    assert f(data, 4) == -1


@pytest.mark.parametrize("f", FUNCS)
def test_all_equal(f):
    assert f([2] * 1000, 500) == 2


@pytest.mark.parametrize("f", FUNCS)
def test_invalid_k(f):
    with pytest.raises(ValueError):
        f([1, 2], 0)
    with pytest.raises(ValueError):
        f([1, 2], 3)
    with pytest.raises(ValueError):
        f([], 1)


def test_input_not_mutated():
    data = [5, 1, 4, 2, 3]
    kth_largest(data, 2)
    assert data == [5, 1, 4, 2, 3]


def test_sorted_and_reversed_inputs():
    asc = list(range(10000))
    assert kth_largest(asc, 1) == 9999
    assert kth_largest(asc[::-1], 10000) == 0


def test_randomized_against_sort():
    rng = random.Random(5)
    for _ in range(300):
        n = rng.randint(1, 60)
        data = [rng.randint(-10, 10) for _ in range(n)]
        k = rng.randint(1, n)
        expected = sorted(data, reverse=True)[k - 1]
        assert kth_largest(data, k, random.Random(rng.random())) == expected
        assert kth_largest_heap(data, k) == expected
