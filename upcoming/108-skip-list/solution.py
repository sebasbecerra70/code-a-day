"""Skip list: a sorted map with expected O(log n) search/insert/delete.

Every node is in level 0 (a sorted linked list). Each node is promoted to the
next level with probability p, so higher levels are sparse "express lanes".
"""

from __future__ import annotations

import random
from typing import Any, Iterator


class _Node:
    __slots__ = ("key", "value", "forward")

    def __init__(self, key: Any, value: Any, level: int):
        self.key = key
        self.value = value
        self.forward: list[_Node | None] = [None] * level


class SkipList:
    MAX_LEVEL = 32

    def __init__(self, p: float = 0.5, seed: int | None = None):
        if not 0 < p < 1:
            raise ValueError("p must be in (0, 1)")
        self.p = p
        self._rng = random.Random(seed)
        self._head = _Node(None, None, self.MAX_LEVEL)  # sentinel; key is never compared
        self._level = 1  # number of levels currently in use
        self._len = 0

    def __len__(self) -> int:
        return self._len

    def _random_level(self) -> int:
        lvl = 1
        while lvl < self.MAX_LEVEL and self._rng.random() < self.p:
            lvl += 1
        return lvl

    def _find_update(self, key: Any) -> list[_Node]:
        """For each level, the last node with node.key < key (where a new node would be spliced in)."""
        update = [self._head] * self.MAX_LEVEL
        x = self._head
        for i in range(self._level - 1, -1, -1):
            while (nxt := x.forward[i]) is not None and nxt.key < key:
                x = nxt
            update[i] = x
        return update

    def get(self, key: Any, default: Any = None) -> Any:
        x = self._find_update(key)[0].forward[0]
        return x.value if x is not None and x.key == key else default

    def __contains__(self, key: Any) -> bool:
        x = self._find_update(key)[0].forward[0]
        return x is not None and x.key == key

    def insert(self, key: Any, value: Any = None) -> bool:
        """Inserts or updates. Returns True if the key was new."""
        update = self._find_update(key)
        x = update[0].forward[0]
        if x is not None and x.key == key:
            x.value = value
            return False
        lvl = self._random_level()
        if lvl > self._level:
            # update[] already defaults to head for the new levels.
            self._level = lvl
        node = _Node(key, value, lvl)
        for i in range(lvl):
            node.forward[i] = update[i].forward[i]
            update[i].forward[i] = node
        self._len += 1
        return True

    def delete(self, key: Any) -> bool:
        update = self._find_update(key)
        x = update[0].forward[0]
        if x is None or x.key != key:
            return False
        for i in range(len(x.forward)):
            update[i].forward[i] = x.forward[i]
        while self._level > 1 and self._head.forward[self._level - 1] is None:
            self._level -= 1
        self._len -= 1
        return True

    def __iter__(self) -> Iterator[Any]:
        x = self._head.forward[0]
        while x is not None:
            yield x.key
            x = x.forward[0]

    def items(self) -> Iterator[tuple[Any, Any]]:
        x = self._head.forward[0]
        while x is not None:
            yield x.key, x.value
            x = x.forward[0]

    def range(self, lo: Any, hi: Any) -> list[tuple[Any, Any]]:
        """All (key, value) with lo <= key < hi: O(log n + k)."""
        x = self._find_update(lo)[0].forward[0]
        out = []
        while x is not None and x.key < hi:
            out.append((x.key, x.value))
            x = x.forward[0]
        return out

    def floor(self, key: Any) -> Any:
        """Largest key <= key, or None."""
        update = self._find_update(key)
        x = update[0].forward[0]
        if x is not None and x.key == key:
            return key
        return update[0].key  # None when update[0] is the head sentinel

    def ceiling(self, key: Any) -> Any:
        """Smallest key >= key, or None."""
        x = self._find_update(key)[0].forward[0]
        return None if x is None else x.key
