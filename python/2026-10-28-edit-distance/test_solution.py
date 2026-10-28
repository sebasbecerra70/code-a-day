import random
from functools import lru_cache

import pytest

from solution import apply_script, edit_distance, edit_script


@pytest.mark.parametrize(
    "a,b,d",
    [
        ("kitten", "sitting", 3),
        ("horse", "ros", 3),
        ("intention", "execution", 5),
        ("", "", 0),
        ("", "abc", 3),
        ("abc", "", 3),
        ("same", "same", 0),
        ("a", "b", 1),
    ],
)
def test_known_distances(a, b, d):
    assert edit_distance(a, b) == d
    assert edit_distance(b, a) == d  # symmetric


def test_script_cost_and_result():
    ops = edit_script("kitten", "sitting")
    assert sum(op != "keep" for op, _, _ in ops) == 3
    assert apply_script("kitten", ops) == "sitting"


def test_script_for_empty_strings():
    assert edit_script("", "") == []
    assert edit_script("", "ab") == [("ins", "", "a"), ("ins", "", "b")]
    assert edit_script("ab", "") == [("del", "a", ""), ("del", "b", "")]


def test_unicode():
    assert edit_distance("café", "cafe") == 1
    assert edit_distance("🙂🙃", "🙃") == 1


def test_randomized_against_recursive_definition():
    def brute(a, b):
        @lru_cache(None)
        def go(i, j):
            if i == len(a):
                return len(b) - j
            if j == len(b):
                return len(a) - i
            return min(go(i + 1, j) + 1, go(i, j + 1) + 1, go(i + 1, j + 1) + (a[i] != b[j]))

        return go(0, 0)

    rng = random.Random(2)
    for _ in range(300):
        a = "".join(rng.choice("abc") for _ in range(rng.randint(0, 8)))
        b = "".join(rng.choice("abc") for _ in range(rng.randint(0, 8)))
        d = brute(a, b)
        assert edit_distance(a, b) == d
        ops = edit_script(a, b)
        assert sum(op != "keep" for op, _, _ in ops) == d
        assert apply_script(a, ops) == b


def test_triangle_inequality():
    rng = random.Random(4)
    for _ in range(100):
        a, b, c = ("".join(rng.choice("xy") for _ in range(rng.randint(0, 6))) for _ in range(3))
        assert edit_distance(a, c) <= edit_distance(a, b) + edit_distance(b, c)
