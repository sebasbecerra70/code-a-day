"""Bellman-Ford single-source shortest paths with negative-cycle detection."""

from __future__ import annotations

import math

INF = float("inf")


class NegativeCycleError(Exception):
    def __init__(self, cycle: list[int]):
        super().__init__(f"negative cycle: {cycle}")
        self.cycle = cycle


def bellman_ford(n: int, edges: list[tuple[int, int, float]], source: int) -> tuple[list[float], list[int]]:
    """Returns (dist, prev). Raises NegativeCycleError if a negative cycle is
    reachable from source."""
    if not 0 <= source < n:
        raise ValueError("source out of range")
    dist = [INF] * n
    prev = [-1] * n
    dist[source] = 0
    for _ in range(n - 1):
        changed = False
        for u, v, w in edges:
            if dist[u] + w < dist[v]:
                dist[v] = dist[u] + w
                prev[v] = u
                changed = True
        if not changed:
            break  # early exit: distances are final
    # One more pass: any improvement means a reachable negative cycle.
    for u, v, w in edges:
        if dist[u] + w < dist[v]:
            prev[v] = u
            raise NegativeCycleError(_extract_cycle(prev, v, n))
    return dist, prev


def _extract_cycle(prev: list[int], v: int, n: int) -> list[int]:
    # Walking back n steps guarantees we're inside the cycle.
    for _ in range(n):
        v = prev[v]
    cycle, x = [v], prev[v]
    while x != v:
        cycle.append(x)
        x = prev[x]
    return cycle[::-1]


def path_to(prev: list[int], dist: list[float], target: int) -> list[int]:
    if dist[target] == INF:
        return []
    path = []
    while target != -1:
        path.append(target)
        target = prev[target]
    return path[::-1]


def has_arbitrage(rates: list[list[float]]) -> bool:
    """Currency arbitrage: a cycle with product of rates > 1 is a negative cycle
    under weights -log(rate). Uses a virtual source connected to every node."""
    n = len(rates)
    edges = [(i, j, -math.log(rates[i][j])) for i in range(n) for j in range(n) if i != j and rates[i][j] > 0]
    edges += [(n, i, 0.0) for i in range(n)]
    try:
        bellman_ford(n + 1, edges, n)
    except NegativeCycleError:
        return True
    return False
