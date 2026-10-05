import { test } from "node:test";
import assert from "node:assert/strict";
import { slidingWindowMax, slidingWindowMin, MaxWindow, longestWithinLimit } from "./solution.ts";

test("classic example", () => {
  assert.deepEqual(slidingWindowMax([1, 3, -1, -3, 5, 3, 6, 7], 3), [3, 3, 5, 5, 6, 7]);
  assert.deepEqual(slidingWindowMin([1, 3, -1, -3, 5, 3, 6, 7], 3), [-1, -3, -3, -3, 3, 3]);
});

test("k = 1 and k = n", () => {
  assert.deepEqual(slidingWindowMax([4, 2, 9], 1), [4, 2, 9]);
  assert.deepEqual(slidingWindowMax([4, 2, 9], 3), [9]);
});

test("k larger than input, empty input, invalid k", () => {
  assert.deepEqual(slidingWindowMax([1, 2], 3), []);
  assert.deepEqual(slidingWindowMax([], 1), []);
  assert.throws(() => slidingWindowMax([1], 0), RangeError);
  assert.throws(() => slidingWindowMax([1], 1.5), RangeError);
});

test("duplicates and monotonic inputs", () => {
  assert.deepEqual(slidingWindowMax([5, 5, 5, 5], 2), [5, 5, 5]);
  assert.deepEqual(slidingWindowMax([1, 2, 3, 4, 5], 2), [2, 3, 4, 5]);
  assert.deepEqual(slidingWindowMax([5, 4, 3, 2, 1], 2), [5, 4, 3, 2]);
});

test("streaming MaxWindow", () => {
  const w = new MaxWindow(3);
  assert.equal(w.max(), undefined);
  const seen: number[] = [];
  for (const v of [1, 3, -1, -3, 5, 3, 6, 7]) {
    w.push(v);
    seen.push(w.max()!);
  }
  assert.deepEqual(seen, [1, 3, 3, 3, 5, 5, 6, 7]);
});

test("MaxWindow stays correct across compaction", () => {
  const w = new MaxWindow(5);
  const all: number[] = [];
  for (let i = 0; i < 5000; i++) {
    const v = (i * 7919) % 101; // deterministic pseudo-random values
    w.push(v);
    all.push(v);
    assert.equal(w.max(), Math.max(...all.slice(-5)));
  }
});

test("longest subarray within limit", () => {
  assert.equal(longestWithinLimit([8, 2, 4, 7], 4), 2);
  assert.equal(longestWithinLimit([10, 1, 2, 4, 7, 2], 5), 4);
  assert.equal(longestWithinLimit([4, 2, 2, 2, 4, 4, 2, 2], 0), 3);
  assert.equal(longestWithinLimit([], 3), 0);
});

test("randomized cross-check against brute force", () => {
  let seed = 1;
  const rand = (n: number) => ((seed = (seed * 1103515245 + 12345) % 2 ** 31) % n);
  for (let t = 0; t < 300; t++) {
    const nums = Array.from({ length: rand(30) }, () => rand(21) - 10);
    const k = 1 + rand(8);
    const brute = (f: (...x: number[]) => number) =>
      nums.length < k ? [] : Array.from({ length: nums.length - k + 1 }, (_, i) => f(...nums.slice(i, i + k)));
    assert.deepEqual(slidingWindowMax(nums, k), brute(Math.max));
    assert.deepEqual(slidingWindowMin(nums, k), brute(Math.min));
    const limit = rand(10);
    let best = 0;
    for (let i = 0; i < nums.length; i++)
      for (let j = i; j < nums.length; j++) {
        const s = nums.slice(i, j + 1);
        if (Math.max(...s) - Math.min(...s) <= limit) best = Math.max(best, j - i + 1);
      }
    assert.equal(longestWithinLimit(nums, limit), best);
  }
});
