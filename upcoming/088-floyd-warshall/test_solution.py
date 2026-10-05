import heapq
import random

import pytest

from solution import INF, NegativeCycleError, floyd_warshall, graph_center, path, transitive_closure


def test_small_graph_distances_and_paths():
    edges = [(0, 1, 3), (0, 2, 8), (1, 2, 2), (2, 3, 1), (3, 0, 4)]
    dist, nxt = floyd_warshall(4, edges)
    assert dist[0] == [0, 3, 5, 6]
    assert dist[3][2] == 9
    assert path(nxt, 0, 3) == [0, 1, 2, 3]
    assert path(nxt, 2, 1) == [2, 3, 0, 1]


def test_negative_edges_without_cycle():
    dist, nxt = floyd_warshall(3, [(0, 1, 4), (0, 2, 5), (2, 1, -3)])
    assert dist[0][1] == 2
    assert path(nxt, 0, 1) == [0, 2, 1]


def test_unreachable_and_self():
    dist, nxt = floyd_warshall(3, [(0, 1, 1)])
    assert dist[1][0] == INF
    assert path(nxt, 1, 0) == []
    assert path(nxt, 2, 2) == [2]
    assert dist[2][2] == 0


def test_parallel_edges_keep_minimum():
    dist, _ = floyd_warshall(2, [(0, 1, 5), (0, 1, 2), (0, 1, 7)])
    assert dist[0][1] == 2


def test_negative_cycle_detected():
    with pytest.raises(NegativeCycleError):
        floyd_warshall(3, [(0, 1, 1), (1, 2, -2), (2, 1, 1)])
    with pytest.raises(NegativeCycleError):
        floyd_warshall(1, [(0, 0, -1)])


def test_empty_graph():
    assert floyd_warshall(0, []) == ([], [])


def test_transitive_closure():
    reach = transitive_closure(4, [(0, 1), (1, 2)])
    assert reach[0][2] and reach[0][1] and not reach[2][0]
    assert not any(reach[3][j] for j in range(3)) and reach[3][3]


def test_graph_center():
    # Path graph 0-1-2-3-4 (undirected): center is 2.
    edges = [(i, i + 1, 1) for i in range(4)] + [(i + 1, i, 1) for i in range(4)]
    assert graph_center(5, edges) == 2


def dijkstra(n, adj, s):
    dist = [INF] * n
    dist[s] = 0
    pq = [(0, s)]
    while pq:
        d, u = heapq.heappop(pq)
        if d > dist[u]:
            continue
        for v, w in adj[u]:
            if d + w < dist[v]:
                dist[v] = d + w
                heapq.heappush(pq, (dist[v], v))
    return dist


def test_randomized_against_dijkstra():
    rng = random.Random(0)
    for _ in range(50):
        n = rng.randint(1, 9)
        edges = [(rng.randrange(n), rng.randrange(n), rng.randint(0, 20)) for _ in range(rng.randint(0, 25))]
        adj = [[] for _ in range(n)]
        for u, v, w in edges:
            adj[u].append((v, w))
        dist, nxt = floyd_warshall(n, edges)
        for s in range(n):
            assert dist[s] == dijkstra(n, adj, s)
            for t in range(n):
                p = path(nxt, s, t)
                if dist[s][t] == INF:
                    assert p == []
                else:
                    # Path cost (using the cheapest parallel edge) must equal the reported distance.
                    cost = sum(min(c for a, b, c in edges if (a, b) == (u, v)) for u, v in zip(p, p[1:]))
                    assert (p[0], p[-1], cost) == (s, t, dist[s][t])
