import random

from solution import find_all, find_first, prefix_function, shortest_period


def test_prefix_function_examples():
    assert prefix_function("abcabcd") == [0, 0, 0, 1, 2, 3, 0]
    assert prefix_function("aabaaab") == [0, 1, 0, 1, 2, 2, 3]
    assert prefix_function("aaaa") == [0, 1, 2, 3]
    assert prefix_function("") == []


def test_find_all_basic():
    assert find_all("abxabcabcaby", "abcaby") == [6]
    assert find_all("hello", "ll") == [2]
    assert find_all("hello", "z") == []


def test_overlapping_matches():
    assert find_all("aaaaa", "aa") == [0, 1, 2, 3]
    assert find_all("abababa", "aba") == [0, 2, 4]


def test_edge_cases():
    assert find_all("", "a") == []
    assert find_all("abc", "abcd") == []
    assert find_all("abc", "abc") == [0]
    assert find_all("ab", "") == [0, 1, 2]
    assert find_first("abc", "") == 0
    assert find_first("abc", "c") == 2
    assert find_first("abc", "x") == -1


def test_shortest_period():
    assert shortest_period("abcabcab") == 3
    assert shortest_period("aaaa") == 1
    assert shortest_period("abcd") == 4
    assert shortest_period("") == 0


def test_prefix_function_brute_force():
    rng = random.Random(0)
    for _ in range(200):
        s = "".join(rng.choice("ab") for _ in range(rng.randint(0, 15)))
        expected = [
            max(k for k in range(i + 1) if s[:k] == s[i + 1 - k : i + 1] and k <= i)
            for i in range(len(s))
        ]
        assert prefix_function(s) == expected


def test_randomized_against_naive_search():
    rng = random.Random(1)
    for _ in range(500):
        text = "".join(rng.choice("abc") for _ in range(rng.randint(0, 40)))
        pat = "".join(rng.choice("abc") for _ in range(rng.randint(1, 4)))
        naive = [i for i in range(len(text) - len(pat) + 1) if text[i : i + len(pat)] == pat]
        assert find_all(text, pat) == naive
        assert find_first(text, pat) == text.find(pat)
