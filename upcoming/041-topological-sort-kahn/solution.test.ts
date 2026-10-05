import { test } from "node:test";
import assert from "node:assert/strict";
import { CycleError, parallelLevels, topoSort, topoSortLexicographic } from "./solution.ts";

function assertValidOrder(deps: Record<string, string[]>, order: string[]) {
  const pos = new Map(order.map((n, i) => [n, i]));
  assert.equal(pos.size, order.length, "no duplicates");
  for (const [n, reqs] of Object.entries(deps))
    for (const r of reqs) assert.ok(pos.get(r)! < pos.get(n)!, `${r} must precede ${n}`);
}

const build = {
  app: ["lib", "utils"],
  lib: ["utils", "core"],
  utils: ["core"],
  test: ["app"],
};

test("orders a build graph and includes dependency-only nodes", () => {
  const order = topoSort(build);
  assertValidOrder(build, order);
  assert.equal(order.length, 5);
  assert.equal(order[0], "core");
});

test("empty graph and isolated nodes", () => {
  assert.deepEqual(topoSort({}), []);
  assert.deepEqual(topoSort({ a: [], b: [] }).sort(), ["a", "b"]);
});

test("detects cycles and reports the nodes involved", () => {
  assert.throws(() => topoSort({ a: ["b"], b: ["a"] }), CycleError);
  try {
    topoSort({ x: [], a: ["c", "x"], b: ["a"], c: ["b"] });
    assert.fail("expected cycle");
  } catch (e) {
    assert.ok(e instanceof CycleError);
    assert.deepEqual([...e.remaining].sort(), ["a", "b", "c"]);
  }
  assert.throws(() => topoSort({ a: ["a"] }), CycleError);
});

test("duplicate dependency entries are tolerated", () => {
  const deps = { b: ["a", "a"] };
  assert.deepEqual(topoSort(deps), ["a", "b"]);
});

test("lexicographic variant is deterministic", () => {
  const deps = { c: [], b: [], a: ["c"], d: ["b"] };
  assert.deepEqual(topoSortLexicographic(deps), ["b", "c", "a", "d"]);
  assert.throws(() => topoSortLexicographic({ a: ["b"], b: ["a"] }), CycleError);
});

test("parallel levels", () => {
  assert.deepEqual(
    parallelLevels(build).map((l) => l.sort()),
    [["core"], ["utils"], ["lib"], ["app"], ["test"]],
  );
  assert.deepEqual(
    parallelLevels({ a: [], b: [], c: ["a", "b"] }).map((l) => l.sort()),
    [["a", "b"], ["c"]],
  );
});

test("randomized DAGs: valid order, and lexicographic matches brute force", () => {
  let seed = 31;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  for (let t = 0; t < 100; t++) {
    const n = 1 + r(8);
    const names = Array.from({ length: n }, (_, i) => String.fromCharCode(97 + i));
    // Edges only go forward in a random hidden order, which guarantees acyclicity.
    const rank = [...names];
    for (let i = n - 1; i > 0; i--) {
      const j = r(i + 1);
      [rank[i], rank[j]] = [rank[j], rank[i]];
    }
    const deps: Record<string, string[]> = {};
    for (let i = 0; i < n; i++) {
      deps[rank[i]] = [];
      for (let j = 0; j < i; j++) if (r(3) === 0) deps[rank[i]].push(rank[j]);
    }
    assertValidOrder(deps, topoSort(deps));
    const lex = topoSortLexicographic(deps);
    assertValidOrder(deps, lex);
    // Brute force: greedily take the smallest node whose deps are all placed.
    const placed = new Set<string>();
    const expected: string[] = [];
    while (expected.length < n) {
      const next = [...names].sort().find((x) => !placed.has(x) && deps[x].every((d) => placed.has(d)))!;
      placed.add(next);
      expected.push(next);
    }
    assert.deepEqual(lex, expected);
    assert.equal(parallelLevels(deps).flat().length, n);
  }
});
