import random

import pytest

from solution import decode, decode_bytes, encode, encode_bytes


def test_text_examples():
    assert encode("aaabccdddd") == "3a1b2c4d"
    assert decode("3a1b2c4d") == "aaabccdddd"
    assert encode("") == "" and decode("") == ""
    assert encode("x") == "1x"


def test_multi_digit_counts():
    s = "z" * 123 + "y"
    assert encode(s) == "123z1y"
    assert decode("123z1y") == s


def test_unicode_and_spaces():
    s = "  ☕☕é"
    assert encode(s) == "2 2☕1é"
    assert decode(encode(s)) == s


def test_text_errors():
    with pytest.raises(ValueError):
        encode("a1")
    with pytest.raises(ValueError):
        decode("a")  # missing count
    with pytest.raises(ValueError):
        decode("3a2")  # dangling count


def test_bytes_round_trip_and_format():
    assert encode_bytes(b"\x00\x00\x00\xff") == bytes([3, 0, 1, 255])
    assert decode_bytes(bytes([3, 0, 1, 255])) == b"\x00\x00\x00\xff"
    assert encode_bytes(b"") == b""


def test_bytes_long_runs_split_at_255():
    data = b"A" * 600
    enc = encode_bytes(data)
    assert enc == bytes([255, 65, 255, 65, 90, 65])
    assert decode_bytes(enc) == data


def test_bytes_errors():
    with pytest.raises(ValueError):
        decode_bytes(b"\x01")
    with pytest.raises(ValueError):
        decode_bytes(b"\x00A")


def test_randomized_round_trips():
    rng = random.Random(0)
    for _ in range(300):
        s = "".join(rng.choice("ab ") * rng.randint(1, 30) for _ in range(rng.randint(0, 6)))
        assert decode(encode(s)) == s
        enc = encode(s)
        # Adjacent runs in the encoding always have different characters.
        chars = [c for c in enc if not c.isdigit()]
        assert all(a != b for a, b in zip(chars, chars[1:]))
        data = bytes(rng.choice([0, 1, 255]) for _ in range(rng.randint(0, 700)))
        assert decode_bytes(encode_bytes(data)) == data
