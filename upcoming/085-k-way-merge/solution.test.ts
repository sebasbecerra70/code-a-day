import { test } from "node:test";
import assert from "node:assert/strict";
import { mergeK, mergeKArrays, smallestRange } from "./solution.ts";

test("merges several sorted arrays", () => {
  assert.deepEqual(mergeKArrays([[1, 4, 7], [2, 5, 8], [3, 6, 9]]), [1, 2, 3, 4, 5, 6, 7, 8, 9]);
});

test("handles empty input and empty sources", () => {
  assert.deepEqual(mergeKArrays<number>([]), []);
  assert.deepEqual(mergeKArrays<number>([[], [], []]), []);
  assert.deepEqual(mergeKArrays([[], [1, 2], []]), [1, 2]);
});

test("single source passes through", () => {
  assert.deepEqual(mergeKArrays([[1, 1, 2, 3]]), [1, 1, 2, 3]);
});

test("stable: equal keys come out in source order", () => {
  type R = { k: number; src: string };
  const a: R[] = [{ k: 1, src: "a" }, { k: 2, src: "a" }];
  const b: R[] = [{ k: 1, src: "b" }, { k: 2, src: "b" }];
  const out = mergeKArrays([a, b], (x, y) => x.k - y.k).map((r) => `${r.k}${r.src}`);
  assert.deepEqual(out, ["1a", "1b", "2a", "2b"]);
});

test("custom comparator (descending strings)", () => {
  const out = mergeKArrays([["z", "m", "a"], ["y", "b"]], (x, y) => y.localeCompare(x));
  assert.deepEqual(out, ["z", "y", "m", "b", "a"]);
});

test("lazy: works with infinite generators", () => {
  function* multiples(k: number) {
    for (let i = k; ; i += k) yield i;
  }
  const out: number[] = [];
  for (const x of mergeK([multiples(3), multiples(5)])) {
    out.push(x);
    if (out.length === 7) break;
  }
  assert.deepEqual(out, [3, 5, 6, 9, 10, 12, 15]);
});

test("randomized cross-check against concat + sort", () => {
  let seed = 42;
  const rand = (n: number) => ((seed = (seed * 1103515245 + 12345) % 2 ** 31) % n);
  for (let trial = 0; trial < 200; trial++) {
    const k = rand(8);
    const arrays = Array.from({ length: k }, () =>
      Array.from({ length: rand(10) }, () => rand(50) - 25).sort((a, b) => a - b),
    );
    const expected = arrays.flat().sort((a, b) => a - b);
    assert.deepEqual(mergeKArrays(arrays, (a, b) => a - b), expected);
  }
});

test("smallest range covering all lists", () => {
  assert.deepEqual(smallestRange([[4, 10, 15, 24, 26], [0, 9, 12, 20], [5, 18, 22, 30]]), [20, 24]);
  assert.deepEqual(smallestRange([[1, 2, 3], [1, 2, 3], [1, 2, 3]]), [1, 1]);
  assert.deepEqual(smallestRange([[5]]), [5, 5]);
  assert.throws(() => smallestRange([[1], []]));
});
