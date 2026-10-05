"""Segment trees for range-sum queries.

SegmentTree: iterative bottom-up tree, point update + range sum.
LazySegmentTree: recursive tree with lazy propagation, range add + range sum.
"""


class SegmentTree:
    def __init__(self, values: list[int]):
        self.n = len(values)
        # Leaves live at [n, 2n); node i has children 2i and 2i+1.
        self.t = [0] * self.n + list(values)
        for i in range(self.n - 1, 0, -1):
            self.t[i] = self.t[2 * i] + self.t[2 * i + 1]

    def update(self, i: int, value: int) -> None:
        """Set values[i] = value."""
        self._check(i)
        i += self.n
        self.t[i] = value
        while i > 1:
            i //= 2
            self.t[i] = self.t[2 * i] + self.t[2 * i + 1]

    def query(self, lo: int, hi: int) -> int:
        """Sum of values[lo:hi] (half-open)."""
        if not 0 <= lo <= hi <= self.n:
            raise IndexError("bad range")
        total = 0
        lo += self.n
        hi += self.n
        while lo < hi:
            if lo & 1:  # lo is a right child: take it and move right
                total += self.t[lo]
                lo += 1
            if hi & 1:  # hi is exclusive; its left sibling is inside the range
                hi -= 1
                total += self.t[hi]
            lo //= 2
            hi //= 2
        return total

    def _check(self, i):
        if not 0 <= i < self.n:
            raise IndexError(i)


class LazySegmentTree:
    def __init__(self, values: list[int]):
        self.n = len(values)
        size = 4 * max(1, self.n)
        self.sum = [0] * size
        self.lazy = [0] * size  # pending add for every element in the node's range
        if self.n:
            self._build(1, 0, self.n - 1, values)

    def _build(self, node, l, r, values):
        if l == r:
            self.sum[node] = values[l]
            return
        m = (l + r) // 2
        self._build(2 * node, l, m, values)
        self._build(2 * node + 1, m + 1, r, values)
        self.sum[node] = self.sum[2 * node] + self.sum[2 * node + 1]

    def _apply(self, node, l, r, delta):
        self.sum[node] += delta * (r - l + 1)
        self.lazy[node] += delta

    def _push(self, node, l, r):
        if self.lazy[node]:
            m = (l + r) // 2
            self._apply(2 * node, l, m, self.lazy[node])
            self._apply(2 * node + 1, m + 1, r, self.lazy[node])
            self.lazy[node] = 0

    def range_add(self, lo: int, hi: int, delta: int) -> None:
        """Add delta to values[lo:hi] (half-open)."""
        if not 0 <= lo <= hi <= self.n:
            raise IndexError("bad range")
        if lo < hi:
            self._add(1, 0, self.n - 1, lo, hi - 1, delta)

    def _add(self, node, l, r, ql, qr, delta):
        if qr < l or r < ql:
            return
        if ql <= l and r <= qr:  # fully covered: defer to children
            self._apply(node, l, r, delta)
            return
        self._push(node, l, r)
        m = (l + r) // 2
        self._add(2 * node, l, m, ql, qr, delta)
        self._add(2 * node + 1, m + 1, r, ql, qr, delta)
        self.sum[node] = self.sum[2 * node] + self.sum[2 * node + 1]

    def query(self, lo: int, hi: int) -> int:
        """Sum of values[lo:hi] (half-open)."""
        if not 0 <= lo <= hi <= self.n:
            raise IndexError("bad range")
        return self._query(1, 0, self.n - 1, lo, hi - 1) if lo < hi else 0

    def _query(self, node, l, r, ql, qr):
        if qr < l or r < ql:
            return 0
        if ql <= l and r <= qr:
            return self.sum[node]
        self._push(node, l, r)
        m = (l + r) // 2
        return self._query(2 * node, l, m, ql, qr) + self._query(2 * node + 1, m + 1, r, ql, qr)
