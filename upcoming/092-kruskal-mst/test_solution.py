import itertools
import random

import pytest

from solution import DSU, k_clusters, kruskal, min_cost_connect_points, mst_or_none


def test_classic_graph():
    edges = [(0, 1, 4), (0, 7, 8), (1, 2, 8), (1, 7, 11), (2, 3, 7), (2, 8, 2), (2, 5, 4), (3, 4, 9),
             (3, 5, 14), (4, 5, 10), (5, 6, 2), (6, 7, 1), (6, 8, 6), (7, 8, 7)]
    total, chosen = kruskal(9, edges)
    assert total == 37
    assert len(chosen) == 8


def test_disconnected_graph_gives_forest():
    total, chosen = kruskal(5, [(0, 1, 1), (1, 2, 2), (3, 4, 5)])
    assert total == 8 and len(chosen) == 3
    assert mst_or_none(5, [(0, 1, 1), (3, 4, 5)]) is None


def test_trivial_graphs():
    assert kruskal(0, []) == (0, [])
    assert kruskal(1, []) == (0, [])
    assert mst_or_none(1, []) == 0


def test_self_loops_and_parallel_edges():
    total, chosen = kruskal(2, [(0, 0, -5), (0, 1, 9), (1, 0, 3)])
    assert total == 3 and chosen == [(1, 0, 3)]


def test_negative_weights():
    total, _ = kruskal(3, [(0, 1, -2), (1, 2, -1), (0, 2, 5)])
    assert total == -3


def test_dsu():
    d = DSU(4)
    assert d.union(0, 1) and d.union(2, 3)
    assert not d.union(1, 0)
    assert d.components == 2
    assert d.find(0) == d.find(1) != d.find(2)


def test_k_clusters():
    # Two tight groups far apart.
    edges = [(0, 1, 1), (1, 2, 1), (3, 4, 1), (2, 3, 100), (0, 4, 120)]
    assert k_clusters(5, edges, 2) == [0, 0, 0, 3, 3]
    assert k_clusters(5, edges, 5) == [0, 1, 2, 3, 4]
    with pytest.raises(ValueError):
        k_clusters(5, edges, 0)


def test_min_cost_connect_points():
    assert min_cost_connect_points([(0, 0), (2, 2), (3, 10), (5, 2), (7, 0)]) == 20
    assert min_cost_connect_points([(1, 1)]) == 0


def brute_force_mst(n, edges):
    best = None
    for combo in itertools.combinations(edges, n - 1):
        d = DSU(n)
        if all(d.union(u, v) for u, v, _ in combo):
            w = sum(e[2] for e in combo)
            best = w if best is None else min(best, w)
    return best


def test_randomized_against_brute_force():
    rng = random.Random(1)
    for _ in range(80):
        n = rng.randint(1, 6)
        edges = [(rng.randrange(n), rng.randrange(n), rng.randint(-5, 10)) for _ in range(rng.randint(0, 9))]
        assert mst_or_none(n, edges) == brute_force_mst(n, edges)
