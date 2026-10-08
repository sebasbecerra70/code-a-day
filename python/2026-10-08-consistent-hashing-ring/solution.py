"""Consistent hashing ring with virtual nodes, backed by a sorted list + bisect."""

import bisect
import hashlib


def _hash(key: str) -> int:
    # md5 is fine here: we want a stable, well-distributed hash, not security.
    return int.from_bytes(hashlib.md5(key.encode()).digest()[:8], "big")


class HashRing:
    def __init__(self, nodes=(), replicas: int = 100):
        if replicas <= 0:
            raise ValueError("replicas must be positive")
        self.replicas = replicas
        self._keys: list[int] = []  # sorted virtual-node hashes
        self._owner: dict[int, str] = {}  # virtual-node hash -> physical node
        self._nodes: set[str] = set()
        for n in nodes:
            self.add_node(n)

    @property
    def nodes(self) -> set[str]:
        return set(self._nodes)

    def add_node(self, node: str) -> None:
        if node in self._nodes:
            return
        self._nodes.add(node)
        for i in range(self.replicas):
            h = _hash(f"{node}#{i}")
            # A hash collision between virtual nodes is astronomically unlikely;
            # if it happens the first owner keeps the point.
            if h in self._owner:
                continue
            self._owner[h] = node
            bisect.insort(self._keys, h)

    def remove_node(self, node: str) -> None:
        if node not in self._nodes:
            raise KeyError(node)
        self._nodes.remove(node)
        for i in range(self.replicas):
            h = _hash(f"{node}#{i}")
            if self._owner.get(h) == node:
                del self._owner[h]
                self._keys.pop(bisect.bisect_left(self._keys, h))

    def get_node(self, key: str) -> str:
        """Return the node owning `key`: the first virtual node clockwise from its hash."""
        if not self._keys:
            raise LookupError("ring is empty")
        i = bisect.bisect_right(self._keys, _hash(key)) % len(self._keys)
        return self._owner[self._keys[i]]
