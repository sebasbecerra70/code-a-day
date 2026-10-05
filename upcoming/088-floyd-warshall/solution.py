"""Floyd-Warshall all-pairs shortest paths with path reconstruction and
negative-cycle detection."""

from __future__ import annotations

INF = float("inf")


class NegativeCycleError(Exception):
    pass


def floyd_warshall(n: int, edges: list[tuple[int, int, float]]) -> tuple[list[list[float]], list[list[int]]]:
    """Returns (dist, nxt) for a directed graph with nodes 0..n-1.

    dist[i][j] is the shortest distance (INF if unreachable). nxt[i][j] is the
    node after i on a shortest i->j path (-1 if none), used by `path`.
    Raises NegativeCycleError if any negative cycle exists.
    """
    dist = [[INF] * n for _ in range(n)]
    nxt = [[-1] * n for _ in range(n)]
    for i in range(n):
        dist[i][i] = 0
        nxt[i][i] = i
    for u, v, w in edges:
        if w < dist[u][v]:  # keep the cheapest parallel edge
            dist[u][v] = w
            nxt[u][v] = v

    # After iteration k, dist[i][j] is the shortest path using only
    # intermediate nodes from {0..k}.
    for k in range(n):
        dk = dist[k]
        for i in range(n):
            dik = dist[i][k]
            if dik == INF:
                continue
            di, ni = dist[i], nxt[i]
            for j in range(n):
                cand = dik + dk[j]
                if cand < di[j]:
                    di[j] = cand
                    ni[j] = ni[k]

    if any(dist[i][i] < 0 for i in range(n)):
        raise NegativeCycleError("graph contains a negative cycle")
    return dist, nxt


def path(nxt: list[list[int]], u: int, v: int) -> list[int]:
    """Reconstructs a shortest u->v path, or [] if v is unreachable."""
    if nxt[u][v] == -1:
        return []
    out = [u]
    while u != v:
        u = nxt[u][v]
        out.append(u)
    return out


def transitive_closure(n: int, edges: list[tuple[int, int]]) -> list[list[bool]]:
    """Warshall's algorithm: reach[i][j] is True if j is reachable from i."""
    reach = [[i == j for j in range(n)] for i in range(n)]
    for u, v in edges:
        reach[u][v] = True
    for k in range(n):
        for i in range(n):
            if reach[i][k]:
                rk, ri = reach[k], reach[i]
                for j in range(n):
                    if rk[j]:
                        ri[j] = True
    return reach


def graph_center(n: int, edges: list[tuple[int, int, float]]) -> int:
    """Node minimizing its maximum distance to all others (eccentricity). Ties -> lowest id."""
    dist, _ = floyd_warshall(n, edges)
    return min(range(n), key=lambda i: (max(dist[i]), i))
