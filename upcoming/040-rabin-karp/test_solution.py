import random

from solution import find_all, find_any, longest_repeated_substring


def naive(text, pat):
    return [i for i in range(len(text) - len(pat) + 1) if text[i : i + len(pat)] == pat]


def test_basic():
    assert find_all("abracadabra", "abra") == [0, 7]
    assert find_all("aaaa", "aa") == [0, 1, 2]
    assert find_all("abc", "d") == []


def test_edges():
    assert find_all("", "a") == []
    assert find_all("ab", "abc") == []
    assert find_all("abc", "abc") == [0]
    assert find_all("ab", "") == [0, 1, 2]


def test_unicode():
    assert find_all("☕a☕a☕", "☕a") == [0, 2]


def test_multi_pattern():
    res = find_any("she sells sea shells", ["she", "sea", "ells", "zzz", "s"])
    assert res["she"] == [0, 14]
    assert res["sea"] == [10]
    assert res["ells"] == [5, 16]
    assert res["zzz"] == []
    assert res["s"] == naive("she sells sea shells", "s")


def test_longest_repeated_substring():
    assert longest_repeated_substring("banana") == "ana"
    assert longest_repeated_substring("abcd") == ""
    assert longest_repeated_substring("aaaa") == "aaa"
    assert longest_repeated_substring("") == ""


def test_randomized_against_naive():
    rng = random.Random(4)
    for _ in range(400):
        text = "".join(rng.choice("ab") for _ in range(rng.randint(0, 30)))
        pat = "".join(rng.choice("ab") for _ in range(rng.randint(1, 5)))
        assert find_all(text, pat) == naive(text, pat)


def test_randomized_multi_and_repeat():
    rng = random.Random(12)
    for _ in range(150):
        text = "".join(rng.choice("abc") for _ in range(rng.randint(0, 25)))
        pats = ["".join(rng.choice("abc") for _ in range(rng.randint(1, 3))) for _ in range(4)]
        res = find_any(text, pats)
        for p in pats:
            assert res[p] == naive(text, p)
        lrs = longest_repeated_substring(text)
        best = max(
            (L for L in range(len(text)) if any(len(naive(text, text[i : i + L])) > 1 for i in range(len(text) - L + 1))),
            default=0,
        )
        assert len(lrs) == best
        assert lrs == "" or len(naive(text, lrs)) >= 2
