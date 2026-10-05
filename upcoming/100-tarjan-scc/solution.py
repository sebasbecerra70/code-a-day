"""Tarjan's strongly connected components, written iteratively so deep
graphs don't hit Python's recursion limit. Also builds the condensation DAG."""

from __future__ import annotations


def tarjan_scc(n: int, adj: list[list[int]]) -> list[list[int]]:
    """Returns SCCs of a directed graph (nodes 0..n-1).

    Components come out in reverse topological order of the condensation:
    if there is an edge from SCC A to SCC B, B is listed before A.
    """
    index = [-1] * n  # discovery time
    low = [0] * n  # lowest discovery time reachable via tree edges + one back edge
    on_stack = [False] * n
    stack: list[int] = []
    sccs: list[list[int]] = []
    counter = 0

    for root in range(n):
        if index[root] != -1:
            continue
        # Explicit DFS stack of (node, next-neighbor position) replaces recursion.
        work = [(root, 0)]
        while work:
            v, i = work[-1]
            if i == 0:  # first visit
                index[v] = low[v] = counter
                counter += 1
                stack.append(v)
                on_stack[v] = True
            if i < len(adj[v]):
                work[-1] = (v, i + 1)
                w = adj[v][i]
                if index[w] == -1:
                    work.append((w, 0))  # tree edge: recurse
                elif on_stack[w]:
                    low[v] = min(low[v], index[w])  # back/cross edge into the current SCC
                continue
            # All neighbors done: "return" from v.
            work.pop()
            if work:
                parent = work[-1][0]
                low[parent] = min(low[parent], low[v])
            if low[v] == index[v]:  # v is the root of an SCC: pop it off
                comp = []
                while True:
                    w = stack.pop()
                    on_stack[w] = False
                    comp.append(w)
                    if w == v:
                        break
                sccs.append(comp)
    return sccs


def condensation(n: int, adj: list[list[int]]) -> tuple[list[int], list[set[int]]]:
    """Returns (comp_of, dag): comp_of[v] is v's SCC id, dag[c] is the set of SCC ids c points to.

    SCC ids are numbered in topological order (sources first).
    """
    sccs = tarjan_scc(n, adj)[::-1]  # reverse => topological order
    comp_of = [0] * n
    for cid, comp in enumerate(sccs):
        for v in comp:
            comp_of[v] = cid
    dag: list[set[int]] = [set() for _ in sccs]
    for u in range(n):
        for v in adj[u]:
            if comp_of[u] != comp_of[v]:
                dag[comp_of[u]].add(comp_of[v])
    return comp_of, dag


def two_sat(num_vars: int, clauses: list[tuple[int, int]]) -> list[bool] | None:
    """Solves 2-SAT. Literals are +i / -i for variable i (1-based).

    Each clause (a OR b) adds implications (!a -> b) and (!b -> a). The formula
    is unsatisfiable iff some x and !x share an SCC.
    """
    def node(lit: int) -> int:
        v = abs(lit) - 1
        return 2 * v + (lit < 0)

    n = 2 * num_vars
    adj: list[list[int]] = [[] for _ in range(n)]
    for a, b in clauses:
        adj[node(-a)].append(node(b))
        adj[node(-b)].append(node(a))
    comp_of, _ = condensation(n, adj)
    result = []
    for v in range(num_vars):
        t, f = comp_of[2 * v], comp_of[2 * v + 1]
        if t == f:
            return None
        result.append(t > f)  # pick the literal later in topological order
    return result
