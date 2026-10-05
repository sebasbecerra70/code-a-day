"""Array-backed binary min-heap with an optional key function."""

from typing import Callable, Generic, Iterable, TypeVar

T = TypeVar("T")


class MinHeap(Generic[T]):
    def __init__(self, items: Iterable[T] = (), key: Callable[[T], object] = lambda x: x):
        self._key = key
        self._a: list[T] = list(items)
        # Bottom-up heapify is O(n): sift down every internal node, last first.
        for i in range(len(self._a) // 2 - 1, -1, -1):
            self._sift_down(i)

    def __len__(self) -> int:
        return len(self._a)

    def __bool__(self) -> bool:
        return bool(self._a)

    def _less(self, i: int, j: int) -> bool:
        return self._key(self._a[i]) < self._key(self._a[j])

    def _sift_up(self, i: int) -> None:
        a = self._a
        while i > 0:
            parent = (i - 1) // 2
            if not self._less(i, parent):
                break
            a[i], a[parent] = a[parent], a[i]
            i = parent

    def _sift_down(self, i: int) -> None:
        a, n = self._a, len(self._a)
        while True:
            smallest, left, right = i, 2 * i + 1, 2 * i + 2
            if left < n and self._less(left, smallest):
                smallest = left
            if right < n and self._less(right, smallest):
                smallest = right
            if smallest == i:
                return
            a[i], a[smallest] = a[smallest], a[i]
            i = smallest

    def push(self, item: T) -> None:
        self._a.append(item)
        self._sift_up(len(self._a) - 1)

    def peek(self) -> T:
        if not self._a:
            raise IndexError("peek from empty heap")
        return self._a[0]

    def pop(self) -> T:
        if not self._a:
            raise IndexError("pop from empty heap")
        a = self._a
        a[0], a[-1] = a[-1], a[0]
        top = a.pop()
        if a:
            self._sift_down(0)
        return top

    def pushpop(self, item: T) -> T:
        """Push then pop, in one sift (faster than separate calls)."""
        if self._a and self._key(self._a[0]) < self._key(item):
            item, self._a[0] = self._a[0], item
            self._sift_down(0)
        return item


def heapsort(items: Iterable[T], key: Callable[[T], object] = lambda x: x) -> list[T]:
    h = MinHeap(items, key)
    return [h.pop() for _ in range(len(h))]
