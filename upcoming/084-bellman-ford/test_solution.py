import random

import pytest

from solution import INF, NegativeCycleError, bellman_ford, has_arbitrage, path_to


def test_negative_edges_no_cycle():
    edges = [(0, 1, 4), (0, 2, 5), (1, 2, -3), (2, 3, 4), (3, 1, 2)]
    dist, prev = bellman_ford(4, edges, 0)
    assert dist == [0, 4, 1, 5]
    assert path_to(prev, dist, 3) == [0, 1, 2, 3]


def test_unreachable_nodes():
    dist, prev = bellman_ford(3, [(0, 1, 2)], 0)
    assert dist[2] == INF
    assert path_to(prev, dist, 2) == []


def test_single_node_and_bad_source():
    assert bellman_ford(1, [], 0)[0] == [0]
    with pytest.raises(ValueError):
        bellman_ford(2, [], 5)


def test_detects_negative_cycle_and_reports_it():
    edges = [(0, 1, 1), (1, 2, -1), (2, 3, -1), (3, 1, -1), (3, 4, 1)]
    with pytest.raises(NegativeCycleError) as exc:
        bellman_ford(5, edges, 0)
    cycle = exc.value.cycle
    assert sorted(cycle) == [1, 2, 3]
    weights = {(u, v): w for u, v, w in edges}
    assert sum(weights[(cycle[i], cycle[(i + 1) % len(cycle)])] for i in range(len(cycle))) < 0


def test_unreachable_negative_cycle_is_ignored():
    edges = [(0, 1, 1), (2, 3, -5), (3, 2, 1)]
    dist, _ = bellman_ford(4, edges, 0)
    assert dist == [0, 1, INF, INF]


def test_arbitrage():
    assert has_arbitrage([[1, 0.9, 0.8], [1.2, 1, 0.9], [1.3, 1.1, 1]]) is True
    assert has_arbitrage([[1, 0.5], [2, 1]]) is False


def test_randomized_against_floyd_warshall():
    rng = random.Random(7)
    for _ in range(150):
        n = rng.randint(1, 7)
        # Negative weights only on forward edges (u < v): negative cycles are possible but not the norm.
        edges = []
        for _ in range(rng.randint(0, 15)):
            u, v = rng.randrange(n), rng.randrange(n)
            w = rng.randint(-5, 10) if u < v else rng.randint(0, 10)
            edges.append((u, v, w))
        d = [[INF] * n for _ in range(n)]
        for i in range(n):
            d[i][i] = 0
        for u, v, w in edges:
            d[u][v] = min(d[u][v], w)
        for k in range(n):
            for i in range(n):
                for j in range(n):
                    if d[i][k] + d[k][j] < d[i][j]:
                        d[i][j] = d[i][k] + d[k][j]
        negative = any(d[i][i] < 0 for i in range(n) if d[0][i] < INF)
        if negative:
            with pytest.raises(NegativeCycleError):
                bellman_ford(n, edges, 0)
        else:
            dist, prev = bellman_ford(n, edges, 0)
            assert dist == d[0]
            for t in range(n):
                p = path_to(prev, dist, t)
                if p:
                    assert p[0] == 0 and p[-1] == t
