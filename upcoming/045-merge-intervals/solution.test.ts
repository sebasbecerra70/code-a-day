import { test } from "node:test";
import assert from "node:assert/strict";
import { insert, intersect, merge, minRemovalsToNonOverlap, type Interval } from "./solution.ts";

test("merge overlapping, touching, nested, unsorted", () => {
  assert.deepEqual(merge([[1, 3], [2, 6], [8, 10], [15, 18]]), [[1, 6], [8, 10], [15, 18]]);
  assert.deepEqual(merge([[1, 4], [4, 5]]), [[1, 5]]);
  assert.deepEqual(merge([[1, 10], [2, 3], [4, 5]]), [[1, 10]]);
  assert.deepEqual(merge([[5, 6], [1, 2]]), [[1, 2], [5, 6]]);
});

test("merge edge cases and validation", () => {
  assert.deepEqual(merge([]), []);
  assert.deepEqual(merge([[3, 3]]), [[3, 3]]);
  assert.throws(() => merge([[5, 1]]), RangeError);
});

test("merge does not mutate input", () => {
  const input: Interval[] = [[2, 3], [1, 2]];
  merge(input);
  assert.deepEqual(input, [[2, 3], [1, 2]]);
});

test("insert", () => {
  assert.deepEqual(insert([[1, 3], [6, 9]], [2, 5]), [[1, 5], [6, 9]]);
  assert.deepEqual(
    insert([[1, 2], [3, 5], [6, 7], [8, 10], [12, 16]], [4, 8]),
    [[1, 2], [3, 10], [12, 16]],
  );
  assert.deepEqual(insert([], [5, 7]), [[5, 7]]);
  assert.deepEqual(insert([[1, 2]], [5, 7]), [[1, 2], [5, 7]]);
  assert.deepEqual(insert([[5, 7]], [1, 2]), [[1, 2], [5, 7]]);
  assert.deepEqual(insert([[1, 5]], [2, 3]), [[1, 5]]);
});

test("intersect", () => {
  assert.deepEqual(
    intersect([[0, 2], [5, 10], [13, 23], [24, 25]], [[1, 5], [8, 12], [15, 24], [25, 26]]),
    [[1, 2], [5, 5], [8, 10], [15, 23], [24, 24], [25, 25]],
  );
  assert.deepEqual(intersect([], [[1, 2]]), []);
  assert.deepEqual(intersect([[1, 2]], [[3, 4]]), []);
});

test("min removals", () => {
  assert.equal(minRemovalsToNonOverlap([[1, 2], [2, 3], [3, 4], [1, 3]]), 1);
  assert.equal(minRemovalsToNonOverlap([[1, 2], [1, 2], [1, 2]]), 2);
  assert.equal(minRemovalsToNonOverlap([[1, 2], [2, 3]]), 0);
  assert.equal(minRemovalsToNonOverlap([]), 0);
});

test("randomized cross-check using a covered-points bitmap", () => {
  let seed = 41;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  // Use doubled coordinates so touching endpoints are distinguishable from gaps.
  const cover = (iv: readonly Interval[]) => {
    const pts = new Set<number>();
    for (const [s, e] of iv) for (let x = 2 * s; x <= 2 * e; x++) pts.add(x);
    return [...pts].sort((a, b) => a - b).join(",");
  };
  const randList = () =>
    Array.from({ length: r(6) }, (): Interval => {
      const s = r(20);
      return [s, s + r(5)];
    });
  for (let t = 0; t < 300; t++) {
    const a = randList();
    const m = merge(a);
    assert.equal(cover(m), cover(a));
    for (let k = 1; k < m.length; k++) assert.ok(m[k][0] > m[k - 1][1]);
    const add: Interval = (() => { const s = r(20); return [s, s + r(5)]; })();
    assert.deepEqual(insert(m, add), merge([...m, add]));
    const b = merge(randList());
    const inter = intersect(m, b);
    const setA = new Set(cover(m).split(",").filter(Boolean));
    const expected = cover(b).split(",").filter((x) => x && setA.has(x)).join(",");
    assert.equal(cover(inter), expected);
  }
});
