import { test } from "node:test";
import assert from "node:assert/strict";
import { countIslands, UnionFind } from "./solution.ts";

test("starts fully disjoint", () => {
  const uf = new UnionFind(4);
  assert.equal(uf.components, 4);
  assert.equal(uf.connected(0, 1), false);
  assert.equal(uf.sizeOf(2), 1);
});

test("union merges and is transitive", () => {
  const uf = new UnionFind(5);
  assert.equal(uf.union(0, 1), true);
  assert.equal(uf.union(1, 2), true);
  assert.equal(uf.connected(0, 2), true);
  assert.equal(uf.connected(0, 3), false);
  assert.equal(uf.components, 3);
  assert.equal(uf.sizeOf(2), 3);
});

test("redundant union returns false", () => {
  const uf = new UnionFind(3);
  uf.union(0, 1);
  assert.equal(uf.union(1, 0), false);
  assert.equal(uf.union(2, 2), false);
  assert.equal(uf.components, 2);
});

test("empty structure and bounds checks", () => {
  const uf = new UnionFind(0);
  assert.equal(uf.components, 0);
  assert.throws(() => uf.find(0), RangeError);
  assert.throws(() => new UnionFind(-1), RangeError);
  assert.throws(() => new UnionFind(2).union(0, 2), RangeError);
});

test("long chain does not overflow and gets compressed", () => {
  const n = 200_000;
  const uf = new UnionFind(n);
  for (let i = 1; i < n; i++) uf.union(i - 1, i);
  assert.equal(uf.components, 1);
  assert.equal(uf.sizeOf(n - 1), n);
  assert.equal(uf.connected(0, n - 1), true);
});

test("count islands", () => {
  assert.equal(countIslands(["11000", "11000", "00100", "00011"]), 3);
  assert.equal(countIslands(["111", "010", "111"]), 1);
  assert.equal(countIslands(["000"]), 0);
  assert.equal(countIslands([]), 0);
});

test("randomized cross-check against naive labeling", () => {
  let seed = 23;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  for (let t = 0; t < 50; t++) {
    const n = 1 + r(30);
    const uf = new UnionFind(n);
    const label = Array.from({ length: n }, (_, i) => i);
    for (let k = 0; k < 40; k++) {
      const a = r(n), b = r(n);
      const merged = uf.union(a, b);
      assert.equal(merged, label[a] !== label[b]);
      const [from, to] = [label[b], label[a]];
      for (let i = 0; i < n; i++) if (label[i] === from) label[i] = to;
      const x = r(n), y = r(n);
      assert.equal(uf.connected(x, y), label[x] === label[y]);
      assert.equal(uf.components, new Set(label).size);
      assert.equal(uf.sizeOf(x), label.filter((l) => l === label[x]).length);
    }
  }
});
