import random
from collections import Counter

import pytest

from solution import reservoir_sample, reservoir_sample_skip, weighted_sample

SAMPLERS = [reservoir_sample, reservoir_sample_skip]


@pytest.mark.parametrize("f", SAMPLERS)
def test_short_stream_returns_everything(f):
    assert sorted(f(range(3), 5, random.Random(0))) == [0, 1, 2]
    assert f([], 3, random.Random(0)) == []
    assert f(range(10), 0, random.Random(0)) == []


@pytest.mark.parametrize("f", SAMPLERS)
def test_sample_size_and_distinct_members(f):
    s = f(range(1000), 10, random.Random(1))
    assert len(s) == 10 and len(set(s)) == 10
    assert all(0 <= x < 1000 for x in s)


@pytest.mark.parametrize("f", SAMPLERS)
def test_works_on_one_shot_generator(f):
    gen = (x * x for x in range(100))
    s = f(gen, 5, random.Random(2))
    assert len(s) == 5 and all(int(x**0.5) ** 2 == x for x in s)


@pytest.mark.parametrize("f", SAMPLERS)
def test_uniform_distribution(f):
    # Each of n=10 items should be chosen with probability k/n = 0.3.
    rng = random.Random(3)
    trials = 20000
    counts = Counter()
    for _ in range(trials):
        counts.update(f(range(10), 3, rng))
    for x in range(10):
        assert abs(counts[x] / trials - 0.3) < 0.02, (x, counts[x])


def test_weighted_prefers_heavy_items():
    rng = random.Random(4)
    counts = Counter()
    for _ in range(20000):
        counts.update(weighted_sample([("a", 1), ("b", 1), ("c", 8)], 1, rng))
    assert abs(counts["c"] / 20000 - 0.8) < 0.02
    assert abs(counts["a"] / 20000 - 0.1) < 0.015


def test_weighted_without_replacement_and_validation():
    s = weighted_sample([(i, 1 + i) for i in range(20)], 5, random.Random(5))
    assert len(s) == len(set(s)) == 5
    assert sorted(weighted_sample([("x", 2.0)], 3)) == ["x"]
    with pytest.raises(ValueError):
        weighted_sample([("x", 0)], 1)
    with pytest.raises(ValueError):
        reservoir_sample([1], -1)
