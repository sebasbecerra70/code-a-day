"""Bloom filter: probabilistic set membership with no false negatives."""

import hashlib
import math


class BloomFilter:
    def __init__(self, capacity: int, error_rate: float = 0.01):
        if capacity <= 0:
            raise ValueError("capacity must be positive")
        if not 0 < error_rate < 1:
            raise ValueError("error_rate must be in (0, 1)")
        # Optimal sizing: m = -n ln p / (ln 2)^2, k = (m / n) ln 2
        self.size = max(1, math.ceil(-capacity * math.log(error_rate) / math.log(2) ** 2))
        self.num_hashes = max(1, round(self.size / capacity * math.log(2)))
        self.bits = bytearray((self.size + 7) // 8)
        self.count = 0  # number of add() calls, not distinct items

    def _positions(self, item: str):
        # Kirsch-Mitzenmacher double hashing: g_i(x) = h1(x) + i*h2(x).
        digest = hashlib.sha256(item.encode()).digest()
        h1 = int.from_bytes(digest[:8], "big")
        h2 = int.from_bytes(digest[8:16], "big") | 1  # odd, so it never collapses to 0
        for i in range(self.num_hashes):
            yield (h1 + i * h2) % self.size

    def add(self, item: str) -> None:
        for p in self._positions(item):
            self.bits[p >> 3] |= 1 << (p & 7)
        self.count += 1

    def __contains__(self, item: str) -> bool:
        return all(self.bits[p >> 3] & (1 << (p & 7)) for p in self._positions(item))

    def estimated_false_positive_rate(self) -> float:
        """(1 - e^(-k n / m))^k for the current number of insertions."""
        k, m = self.num_hashes, self.size
        return (1 - math.exp(-k * self.count / m)) ** k
