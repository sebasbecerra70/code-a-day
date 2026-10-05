"""Huffman coding: optimal prefix-free codes from symbol frequencies."""

from __future__ import annotations

import heapq
from collections import Counter
from itertools import count


def build_codes(freqs: dict[str, int]) -> dict[str, str]:
    """Symbol -> bit string. Deterministic for equal frequencies."""
    if not freqs:
        return {}
    if len(freqs) == 1:
        # A lone symbol still needs at least one bit.
        return {next(iter(freqs)): "0"}
    tiebreak = count()
    # Heap entries: (weight, tiebreak, tree). A tree is a symbol or a (left, right) pair.
    heap = [(w, next(tiebreak), sym) for sym, w in sorted(freqs.items())]
    heapq.heapify(heap)
    while len(heap) > 1:
        w1, _, a = heapq.heappop(heap)
        w2, _, b = heapq.heappop(heap)
        heapq.heappush(heap, (w1 + w2, next(tiebreak), (a, b)))
    codes: dict[str, str] = {}
    stack = [(heap[0][2], "")]
    while stack:
        node, prefix = stack.pop()
        if isinstance(node, tuple):
            stack.append((node[0], prefix + "0"))
            stack.append((node[1], prefix + "1"))
        else:
            codes[node] = prefix
    return codes


def encode(text: str) -> tuple[str, dict[str, str]]:
    codes = build_codes(Counter(text))
    return "".join(codes[ch] for ch in text), codes


def decode(bits: str, codes: dict[str, str]) -> str:
    # Rebuild the decoding trie from the code table.
    root: dict = {}
    for sym, code in codes.items():
        node = root
        for b in code:
            node = node.setdefault(b, {})
        node["sym"] = sym
    out, node = [], root
    for b in bits:
        if b not in node:
            raise ValueError("invalid bit sequence")
        node = node[b]
        if "sym" in node:
            out.append(node["sym"])
            node = root
    if node is not root:
        raise ValueError("trailing bits do not form a complete code")
    return "".join(out)
