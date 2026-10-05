import itertools
import random
import sys

from solution import condensation, tarjan_scc, two_sat


def normalize(sccs):
    return sorted(sorted(c) for c in sccs)


def test_classic_example():
    adj = [[1], [2, 4, 5], [3, 6], [2, 7], [0, 5], [6], [5], [3, 6]]
    assert normalize(tarjan_scc(8, adj)) == [[0, 1, 4], [2, 3, 7], [5, 6]]


def test_reverse_topological_order():
    # 0 -> 1 -> 2, all singletons: sinks come first.
    assert tarjan_scc(3, [[1], [2], []]) == [[2], [1], [0]]


def test_empty_isolated_and_self_loop():
    assert tarjan_scc(0, []) == []
    assert normalize(tarjan_scc(3, [[], [1], []])) == [[0], [1], [2]]


def test_single_big_cycle():
    n = 6
    adj = [[(i + 1) % n] for i in range(n)]
    assert normalize(tarjan_scc(n, adj)) == [list(range(n))]


def test_deep_graph_no_recursion_error():
    n = sys.getrecursionlimit() * 5
    adj = [[i + 1] for i in range(n - 1)] + [[0]]  # one huge cycle
    assert len(tarjan_scc(n, adj)) == 1


def test_condensation_is_dag_in_topological_order():
    adj = [[1], [2, 4, 5], [3, 6], [2, 7], [0, 5], [6], [5], [3, 6]]
    comp_of, dag = condensation(8, adj)
    assert comp_of[0] == comp_of[1] == comp_of[4]
    for c, outs in enumerate(dag):
        assert all(d > c for d in outs)  # edges go forward in topological order
    assert dag[comp_of[0]] == {comp_of[2], comp_of[5]}


def test_two_sat():
    # (x1 or x2) and (!x1 or x2) and (!x2 or x3) -> x2 = x3 = True
    sol = two_sat(3, [(1, 2), (-1, 2), (-2, 3)])
    assert sol is not None and sol[1] and sol[2]
    # x1 and !x1 -> unsatisfiable
    assert two_sat(1, [(1, 1), (-1, -1)]) is None


def reach(n, adj):
    r = [[False] * n for _ in range(n)]
    for s in range(n):
        st = [s]
        r[s][s] = True
        while st:
            u = st.pop()
            for v in adj[u]:
                if not r[s][v]:
                    r[s][v] = True
                    st.append(v)
    return r


def test_randomized_against_reachability():
    rng = random.Random(3)
    for _ in range(100):
        n = rng.randint(1, 10)
        adj = [[rng.randrange(n) for _ in range(rng.randint(0, 3))] for _ in range(n)]
        r = reach(n, adj)
        expected = {frozenset(v for v in range(n) if r[u][v] and r[v][u]) for u in range(n)}
        assert {frozenset(c) for c in tarjan_scc(n, adj)} == expected


def test_randomized_two_sat_against_brute_force():
    rng = random.Random(4)
    for _ in range(150):
        k = rng.randint(1, 4)
        lit = lambda: rng.choice([1, -1]) * rng.randint(1, k)
        clauses = [(lit(), lit()) for _ in range(rng.randint(1, 6))]
        holds = lambda a, l: a[abs(l) - 1] == (l > 0)
        satisfiable = any(all(holds(a, x) or holds(a, y) for x, y in clauses) for a in itertools.product([False, True], repeat=k))
        sol = two_sat(k, clauses)
        assert (sol is not None) == satisfiable
        if sol is not None:
            assert all(holds(sol, x) or holds(sol, y) for x, y in clauses)
