"""Prim's minimum spanning tree: a lazy binary-heap version for sparse graphs
and an O(V^2) array version for dense graphs."""

from __future__ import annotations

import heapq
import math

Adj = list[list[tuple[int, float]]]  # adj[u] = [(v, w), ...], undirected (both directions present)


def build_adj(n: int, edges: list[tuple[int, int, float]]) -> Adj:
    adj: Adj = [[] for _ in range(n)]
    for u, v, w in edges:
        adj[u].append((v, w))
        adj[v].append((u, w))
    return adj


def prim(adj: Adj, start: int = 0) -> tuple[float, list[tuple[int, int, float]]]:
    """MST of the component containing `start`.

    Returns (total_weight, edges) where each edge is (parent, child, weight).
    Lazy deletion: stale heap entries are skipped when popped.
    """
    n = len(adj)
    if n == 0:
        return 0.0, []
    in_tree = [False] * n
    heap: list[tuple[float, int, int]] = [(0.0, start, -1)]  # (weight, node, parent)
    total = 0.0
    edges: list[tuple[int, int, float]] = []
    while heap:
        w, u, parent = heapq.heappop(heap)
        if in_tree[u]:
            continue
        in_tree[u] = True
        if parent != -1:
            total += w
            edges.append((parent, u, w))
        # Grow the tree: every edge leaving it is a candidate (cut property).
        for v, wv in adj[u]:
            if not in_tree[v]:
                heapq.heappush(heap, (wv, v, u))
    return total, edges


def prim_forest(adj: Adj) -> tuple[float, list[tuple[int, int, float]]]:
    """Minimum spanning forest: run Prim from every not-yet-covered node."""
    covered = [False] * len(adj)
    total, out = 0.0, []
    for s in range(len(adj)):
        if covered[s]:
            continue
        t, es = prim(adj, s)
        total += t
        out.extend(es)
        covered[s] = True
        for _, v, _ in es:
            covered[v] = True
    return total, out


def prim_dense(matrix: list[list[float]]) -> float:
    """O(V^2) Prim on an adjacency matrix (math.inf = no edge).

    Faster than the heap version on complete/dense graphs because it avoids
    pushing O(V^2) heap entries. Returns inf if the graph is disconnected.
    """
    n = len(matrix)
    if n == 0:
        return 0.0
    best = [math.inf] * n  # cheapest edge connecting each node to the tree
    best[0] = 0.0
    in_tree = [False] * n
    total = 0.0
    for _ in range(n):
        u = min((i for i in range(n) if not in_tree[i]), key=best.__getitem__)
        if best[u] == math.inf:
            return math.inf
        in_tree[u] = True
        total += best[u]
        row = matrix[u]
        for v in range(n):
            if not in_tree[v] and row[v] < best[v]:
                best[v] = row[v]
    return total
