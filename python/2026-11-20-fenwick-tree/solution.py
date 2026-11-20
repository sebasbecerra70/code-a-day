"""Fenwick tree (binary indexed tree) for prefix sums with point updates."""


class FenwickTree:
    def __init__(self, n_or_values):
        if isinstance(n_or_values, int):
            self.n = n_or_values
            self.tree = [0] * (self.n + 1)
        else:
            values = list(n_or_values)
            self.n = len(values)
            # O(n) build: push each node's total into its parent once.
            self.tree = [0] + values
            for i in range(1, self.n + 1):
                parent = i + (i & -i)
                if parent <= self.n:
                    self.tree[parent] += self.tree[i]

    def __len__(self) -> int:
        return self.n

    def add(self, i: int, delta: int) -> None:
        """values[i] += delta (0-based)."""
        if not 0 <= i < self.n:
            raise IndexError(i)
        i += 1  # tree is 1-based
        while i <= self.n:
            self.tree[i] += delta
            i += i & -i  # jump to the next node whose range covers i

    def prefix_sum(self, k: int) -> int:
        """Sum of values[0:k]."""
        if not 0 <= k <= self.n:
            raise IndexError(k)
        total = 0
        while k > 0:
            total += self.tree[k]
            k -= k & -k  # strip the lowest set bit
        return total

    def range_sum(self, lo: int, hi: int) -> int:
        """Sum of values[lo:hi]."""
        if lo > hi:
            raise IndexError("lo > hi")
        return self.prefix_sum(hi) - self.prefix_sum(lo)

    def lower_bound(self, target: int) -> int:
        """Smallest k such that prefix_sum(k) >= target, assuming non-negative values.
        Returns n + 1 if the total is below target."""
        if target <= 0:
            return 0
        pos, remaining = 0, target
        step = 1 << self.n.bit_length()
        while step:
            nxt = pos + step
            if nxt <= self.n and self.tree[nxt] < remaining:
                pos = nxt
                remaining -= self.tree[nxt]
            step >>= 1
        return pos + 1 if pos < self.n else self.n + 1


def count_inversions(nums: list[int]) -> int:
    """Classic application: pairs i < j with nums[i] > nums[j], in O(n log n)."""
    ranks = {v: r for r, v in enumerate(sorted(set(nums)))}
    bit = FenwickTree(len(ranks))
    inversions = 0
    for seen, x in enumerate(nums):
        r = ranks[x]
        inversions += seen - bit.prefix_sum(r + 1)  # earlier elements greater than x
        bit.add(r, 1)
    return inversions
