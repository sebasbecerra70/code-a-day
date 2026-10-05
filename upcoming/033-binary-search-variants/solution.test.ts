import { test } from "node:test";
import assert from "node:assert/strict";
import {
  findRotationPoint, firstTrue, indexOf, isqrt, lowerBound, searchRange, searchRotated, upperBound,
} from "./solution.ts";

test("lower and upper bound", () => {
  const a = [1, 2, 2, 2, 5, 7];
  assert.equal(lowerBound(a, 2), 1);
  assert.equal(upperBound(a, 2), 4);
  assert.equal(lowerBound(a, 0), 0);
  assert.equal(lowerBound(a, 8), 6);
  assert.equal(upperBound(a, 7), 6);
  assert.equal(lowerBound([], 3), 0);
});

test("indexOf and searchRange", () => {
  const a = [5, 7, 7, 8, 8, 10];
  assert.deepEqual(searchRange(a, 8), [3, 4]);
  assert.deepEqual(searchRange(a, 6), [-1, -1]);
  assert.deepEqual(searchRange([], 0), [-1, -1]);
  assert.equal(indexOf(a, 10), 5);
  assert.equal(indexOf(a, 11), -1);
});

test("rotated search", () => {
  const a = [4, 5, 6, 7, 0, 1, 2];
  assert.equal(searchRotated(a, 0), 4);
  assert.equal(searchRotated(a, 4), 0);
  assert.equal(searchRotated(a, 3), -1);
  assert.equal(searchRotated([1], 1), 0);
  assert.equal(searchRotated([], 1), -1);
  assert.equal(findRotationPoint(a), 4);
  assert.equal(findRotationPoint([1, 2, 3]), 0);
  assert.equal(findRotationPoint([2, 1]), 1);
});

test("firstTrue and isqrt", () => {
  assert.equal(firstTrue(0, 10, (n) => n >= 7), 7);
  assert.equal(firstTrue(0, 10, () => false), 11);
  assert.equal(firstTrue(0, 10, () => true), 0);
  assert.deepEqual([0, 1, 3, 4, 8, 9, 2 ** 40].map(isqrt), [0, 1, 1, 2, 2, 3, 2 ** 20]);
});

test("binary search on the answer: min ship capacity in D days", () => {
  const weights = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  const days = (cap: number) => {
    let d = 1, load = 0;
    for (const w of weights) {
      if (load + w > cap) { d++; load = 0; }
      load += w;
    }
    return d;
  };
  assert.equal(firstTrue(Math.max(...weights), 55, (cap) => days(cap) <= 5), 15);
});

test("randomized cross-check against linear scans", () => {
  let seed = 17;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  for (let t = 0; t < 500; t++) {
    const a = Array.from({ length: r(15) }, () => r(10)).sort((x, y) => x - y);
    const x = r(12) - 1;
    const lb = a.findIndex((v) => v >= x);
    const ub = a.findIndex((v) => v > x);
    assert.equal(lowerBound(a, x), lb === -1 ? a.length : lb);
    assert.equal(upperBound(a, x), ub === -1 ? a.length : ub);
    assert.deepEqual(searchRange(a, x), a.includes(x) ? [a.indexOf(x), a.lastIndexOf(x)] : [-1, -1]);

    const distinct = [...new Set(a)];
    const k = distinct.length ? r(distinct.length) : 0;
    const rot = [...distinct.slice(k), ...distinct.slice(0, k)];
    assert.equal(searchRotated(rot, x), rot.indexOf(x));
    if (rot.length) assert.equal(findRotationPoint(rot), rot.indexOf(Math.min(...rot)));
  }
});
