"""Run-length encoding in two flavors:

- Text: "aaabcc" <-> "3a1b2c" (count before each character; input must not
  contain digits, otherwise the format would be ambiguous).
- Bytes: (count, value) pairs with counts capped at 255, so any binary data works.
"""

from itertools import groupby


def encode(s: str) -> str:
    if any(ch.isdigit() for ch in s):
        raise ValueError("text RLE cannot encode digits; use encode_bytes")
    return "".join(f"{sum(1 for _ in run)}{ch}" for ch, run in groupby(s))


def decode(s: str) -> str:
    out, count = [], ""
    for ch in s:
        if ch.isdigit():
            count += ch
            continue
        if not count:
            raise ValueError(f"missing run length before {ch!r}")
        out.append(ch * int(count))
        count = ""
    if count:
        raise ValueError("dangling run length at end")
    return "".join(out)


def encode_bytes(data: bytes) -> bytes:
    out = bytearray()
    i = 0
    while i < len(data):
        j = i
        while j < len(data) and data[j] == data[i] and j - i < 255:
            j += 1
        out += bytes((j - i, data[i]))  # long runs split into chunks of 255
        i = j
    return bytes(out)


def decode_bytes(data: bytes) -> bytes:
    if len(data) % 2:
        raise ValueError("encoded data must have even length")
    out = bytearray()
    for k in range(0, len(data), 2):
        count, value = data[k], data[k + 1]
        if count == 0:
            raise ValueError("zero-length run")
        out += bytes((value,)) * count
    return bytes(out)
