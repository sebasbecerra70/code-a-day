import { test } from "node:test";
import assert from "node:assert/strict";
import { dijkstra, pathTo, type Graph } from "./solution.ts";

test("classic small graph", () => {
  const g: Graph = [
    [[1, 4], [2, 1]],
    [[3, 1]],
    [[1, 2], [3, 5]],
    [],
  ];
  const r = dijkstra(g, 0);
  assert.deepEqual(r.dist, [0, 3, 1, 4]);
  assert.deepEqual(pathTo(r, 3), [0, 2, 1, 3]);
});

test("unreachable nodes stay Infinity with empty path", () => {
  const g: Graph = [[[1, 2]], [], []];
  const r = dijkstra(g, 0);
  assert.equal(r.dist[2], Infinity);
  assert.deepEqual(pathTo(r, 2), []);
});

test("single node", () => {
  const r = dijkstra([[]], 0);
  assert.deepEqual(r.dist, [0]);
  assert.deepEqual(pathTo(r, 0), [0]);
});

test("zero-weight edges and cycles", () => {
  const g: Graph = [[[1, 0]], [[2, 0], [0, 0]], [[0, 0]]];
  assert.deepEqual(dijkstra(g, 0).dist, [0, 0, 0]);
});

test("parallel edges pick the cheapest", () => {
  const g: Graph = [[[1, 10], [1, 3], [1, 7]], []];
  assert.equal(dijkstra(g, 0).dist[1], 3);
});

test("rejects bad source and negative weights", () => {
  assert.throws(() => dijkstra([[]], 1), RangeError);
  assert.throws(() => dijkstra([[[1, -1]], []], 0), RangeError);
});

test("randomized cross-check against Bellman-Ford", () => {
  let seed = 42;
  const rand = () => ((seed = (seed * 1103515245 + 12345) % 2 ** 31) / 2 ** 31);
  for (let trial = 0; trial < 50; trial++) {
    const n = 1 + Math.floor(rand() * 12);
    const g: Graph = Array.from({ length: n }, () => []);
    const edges: [number, number, number][] = [];
    const m = Math.floor(rand() * n * 3);
    for (let i = 0; i < m; i++) {
      const u = Math.floor(rand() * n), v = Math.floor(rand() * n), w = Math.floor(rand() * 20);
      g[u].push([v, w]);
      edges.push([u, v, w]);
    }
    const expected = new Array(n).fill(Infinity);
    expected[0] = 0;
    for (let k = 0; k < n; k++)
      for (const [u, v, w] of edges) if (expected[u] + w < expected[v]) expected[v] = expected[u] + w;
    const r = dijkstra(g, 0);
    assert.deepEqual(r.dist, expected);
    // Every reconstructed path's length matches the reported distance.
    for (let t = 0; t < n; t++) {
      const p = pathTo(r, t);
      if (p.length === 0) continue;
      let len = 0;
      for (let i = 0; i + 1 < p.length; i++)
        len += Math.min(...g[p[i]].filter(([v]) => v === p[i + 1]).map(([, w]) => w));
      assert.equal(len, r.dist[t]);
    }
  }
});
