import math
import random

import pytest

from solution import BloomFilter


def test_no_false_negatives():
    bf = BloomFilter(1000, 0.01)
    words = [f"word-{i}" for i in range(1000)]
    for w in words:
        bf.add(w)
    assert all(w in bf for w in words)


def test_empty_filter_contains_nothing():
    bf = BloomFilter(10)
    assert "anything" not in bf
    assert "" not in bf


def test_sizing_matches_formula():
    bf = BloomFilter(1000, 0.01)
    assert bf.size == math.ceil(-1000 * math.log(0.01) / math.log(2) ** 2)  # 9586
    assert bf.num_hashes == 7


def test_false_positive_rate_near_target():
    rng = random.Random(0)
    bf = BloomFilter(5000, 0.02)
    for i in range(5000):
        bf.add(f"in-{i}")
    trials = 20000
    fp = sum(f"out-{rng.random()}" in bf for _ in range(trials))
    assert fp / trials < 0.04  # target 2%, generous slack


def test_estimated_rate_grows_with_load():
    bf = BloomFilter(100, 0.01)
    assert bf.estimated_false_positive_rate() == 0
    for i in range(100):
        bf.add(str(i))
    mid = bf.estimated_false_positive_rate()
    assert 0.005 < mid < 0.02
    for i in range(100, 400):
        bf.add(str(i))
    assert bf.estimated_false_positive_rate() > mid


def test_tiny_capacity_still_works():
    bf = BloomFilter(1, 0.5)
    bf.add("x")
    assert "x" in bf
    assert bf.size >= 1 and bf.num_hashes >= 1


def test_unicode_items():
    bf = BloomFilter(10)
    bf.add("naïve ☕")
    assert "naïve ☕" in bf


@pytest.mark.parametrize("cap,err", [(0, 0.1), (10, 0), (10, 1), (10, 1.5)])
def test_invalid_args(cap, err):
    with pytest.raises(ValueError):
        BloomFilter(cap, err)
