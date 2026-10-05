"""Kruskal's minimum spanning tree/forest with a union-find (DSU)."""

from __future__ import annotations


class DSU:
    """Disjoint-set union with path halving and union by size."""

    def __init__(self, n: int):
        self.parent = list(range(n))
        self.size = [1] * n
        self.components = n

    def find(self, x: int) -> int:
        while self.parent[x] != x:
            self.parent[x] = self.parent[self.parent[x]]  # path halving
            x = self.parent[x]
        return x

    def union(self, a: int, b: int) -> bool:
        """Merges the sets of a and b. Returns False if already connected."""
        ra, rb = self.find(a), self.find(b)
        if ra == rb:
            return False
        if self.size[ra] < self.size[rb]:
            ra, rb = rb, ra
        self.parent[rb] = ra
        self.size[ra] += self.size[rb]
        self.components -= 1
        return True


Edge = tuple[int, int, float]


def kruskal(n: int, edges: list[Edge]) -> tuple[float, list[Edge]]:
    """Minimum spanning forest of an undirected graph on nodes 0..n-1.

    Returns (total_weight, chosen_edges). If the graph is disconnected the
    result is a forest (n - components edges).
    """
    dsu = DSU(n)
    chosen: list[Edge] = []
    total = 0.0
    # Greedy: the lightest edge that doesn't close a cycle is always safe (cut property).
    for u, v, w in sorted(edges, key=lambda e: e[2]):
        if dsu.union(u, v):
            chosen.append((u, v, w))
            total += w
            if len(chosen) == n - 1:
                break  # spanning tree complete
    return total, chosen


def mst_or_none(n: int, edges: list[Edge]) -> float | None:
    """Weight of a spanning tree, or None if the graph is disconnected."""
    total, chosen = kruskal(n, edges)
    return total if len(chosen) == max(n - 1, 0) else None


def k_clusters(n: int, edges: list[Edge], k: int) -> list[int]:
    """Single-linkage clustering: stop Kruskal at k components.

    Returns a label per node (labels are the smallest node id in each cluster).
    """
    if not 1 <= k <= n:
        raise ValueError("k must be in 1..n")
    dsu = DSU(n)
    for u, v, _ in sorted(edges, key=lambda e: e[2]):
        if dsu.components == k:
            break
        dsu.union(u, v)
    smallest: dict[int, int] = {}
    for i in range(n):
        smallest.setdefault(dsu.find(i), i)
    return [smallest[dsu.find(i)] for i in range(n)]


def min_cost_connect_points(points: list[tuple[int, int]]) -> int:
    """Classic interview task: connect all points with Manhattan-distance edges at minimum cost."""
    n = len(points)
    edges = [
        (i, j, abs(points[i][0] - points[j][0]) + abs(points[i][1] - points[j][1]))
        for i in range(n)
        for j in range(i + 1, n)
    ]
    return int(kruskal(n, edges)[0])
