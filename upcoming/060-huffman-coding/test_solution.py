import math
import random
from collections import Counter
from itertools import combinations

import pytest

from solution import build_codes, decode, encode


def is_prefix_free(codes):
    return not any(a.startswith(b) or b.startswith(a) for a, b in combinations(codes.values(), 2))


def cost(codes, freqs):
    return sum(len(codes[s]) * f for s, f in freqs.items())


def test_round_trip():
    text = "abracadabra alakazam"
    bits, codes = encode(text)
    assert decode(bits, codes) == text
    assert is_prefix_free(codes)


def test_known_optimal_cost():
    # Classic CLRS example: optimal total = 224 bits (in thousands).
    freqs = {"a": 45, "b": 13, "c": 12, "d": 16, "e": 9, "f": 5}
    codes = build_codes(freqs)
    assert cost(codes, freqs) == 224
    assert len(codes["a"]) == 1


def test_empty_and_single_symbol():
    assert encode("") == ("", {})
    assert decode("", {}) == ""
    bits, codes = encode("aaaa")
    assert codes == {"a": "0"} and bits == "0000"
    assert decode(bits, codes) == "aaaa"


def test_frequent_symbols_get_shorter_codes():
    freqs = Counter("a" * 50 + "b" * 20 + "c" * 5 + "d")
    codes = build_codes(freqs)
    assert len(codes["a"]) <= len(codes["b"]) <= len(codes["c"]) <= len(codes["d"])


def test_beats_fixed_width_and_close_to_entropy():
    rng = random.Random(0)
    text = "".join(rng.choices("abcdefgh", weights=[40, 20, 10, 10, 8, 6, 4, 2], k=5000))
    bits, codes = encode(text)
    assert len(bits) < 3 * len(text)
    freqs = Counter(text)
    n = len(text)
    entropy = -sum(f / n * math.log2(f / n) for f in freqs.values())
    assert entropy * n <= len(bits) < (entropy + 1) * n


def test_decode_rejects_bad_bits():
    codes = {"a": "0", "b": "10", "c": "11"}
    assert decode("01011", codes) == "abc"
    with pytest.raises(ValueError):
        decode("1", codes)
    with pytest.raises(ValueError):
        decode("2", codes)


def test_unicode_round_trip():
    text = "héllo wörld ☕☕"
    bits, codes = encode(text)
    assert decode(bits, codes) == text


def test_randomized_full_tree_and_optimal_cost():
    # Checks Kraft equality (the code tree is full) and that the total cost
    # equals an independent sum-of-merge-weights computation.
    rng = random.Random(3)
    for _ in range(200):
        k = rng.randint(2, 8)
        freqs = {chr(97 + i): rng.randint(1, 30) for i in range(k)}
        codes = build_codes(freqs)
        assert is_prefix_free(codes)
        assert sum(2 ** -len(c) for c in codes.values()) == 1  # full binary tree
        # Optimal cost equals the sum of all merge weights.
        ws = sorted(freqs.values())
        total = 0
        while len(ws) > 1:
            a, b = ws.pop(0), ws.pop(0)
            total += a + b
            ws.append(a + b)
            ws.sort()
        assert cost(codes, freqs) == total
