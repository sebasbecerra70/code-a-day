import math
import random

from solution import build_adj, prim, prim_dense, prim_forest

CLASSIC = [(0, 1, 4), (0, 7, 8), (1, 2, 8), (1, 7, 11), (2, 3, 7), (2, 8, 2), (2, 5, 4), (3, 4, 9),
           (3, 5, 14), (4, 5, 10), (5, 6, 2), (6, 7, 1), (6, 8, 6), (7, 8, 7)]


def to_matrix(n, edges):
    m = [[math.inf] * n for _ in range(n)]
    for u, v, w in edges:
        if u != v:
            m[u][v] = m[v][u] = min(m[u][v], w)
    return m


def test_classic_graph():
    total, edges = prim(build_adj(9, CLASSIC))
    assert total == 37
    assert len(edges) == 8
    assert prim_dense(to_matrix(9, CLASSIC)) == 37


def test_result_is_a_spanning_tree():
    _, edges = prim(build_adj(9, CLASSIC))
    children = [c for _, c, _ in edges]
    assert sorted(children + [0]) == list(range(9))  # every node except the root has exactly one parent


def test_start_node_does_not_change_weight():
    adj = build_adj(9, CLASSIC)
    assert {prim(adj, s)[0] for s in range(9)} == {37}


def test_empty_and_single_node():
    assert prim([]) == (0.0, [])
    assert prim([[]]) == (0.0, [])
    assert prim_dense([]) == 0.0
    assert prim_dense([[math.inf]]) == 0.0


def test_disconnected():
    adj = build_adj(5, [(0, 1, 1), (1, 2, 2), (3, 4, 5)])
    assert prim(adj)[0] == 3  # only start's component
    total, edges = prim_forest(adj)
    assert total == 8 and len(edges) == 3
    assert prim_dense(to_matrix(5, [(0, 1, 1), (3, 4, 5)])) == math.inf


def test_parallel_edges_self_loops_and_negative_weights():
    adj = build_adj(3, [(0, 0, -9), (0, 1, 5), (0, 1, -1), (1, 2, 2), (0, 2, 10)])
    assert prim(adj)[0] == 1


def kruskal_weight(n, edges):
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    total = 0
    for u, v, w in sorted(edges, key=lambda e: e[2]):
        ru, rv = find(u), find(v)
        if ru != rv:
            parent[ru] = rv
            total += w
    return total


def test_randomized_against_kruskal():
    rng = random.Random(2)
    for _ in range(100):
        n = rng.randint(1, 12)
        edges = [(rng.randrange(n), rng.randrange(n), rng.randint(-10, 30)) for _ in range(rng.randint(0, 40))]
        adj = build_adj(n, edges)
        expected = kruskal_weight(n, edges)
        assert prim_forest(adj)[0] == expected
        connected = len(prim(adj)[1]) == n - 1
        assert prim_dense(to_matrix(n, edges)) == (expected if connected else math.inf)
