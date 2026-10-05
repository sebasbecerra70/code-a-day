import { test } from "node:test";
import assert from "node:assert/strict";
import { deepEqual } from "./solution.ts";

test("primitives and special numbers", () => {
  assert.equal(deepEqual(1, 1), true);
  assert.equal(deepEqual("a", "a"), true);
  assert.equal(deepEqual(1, "1"), false);
  assert.equal(deepEqual(NaN, NaN), true);
  assert.equal(deepEqual(0, -0), false);
  assert.equal(deepEqual(null, undefined), false);
  assert.equal(deepEqual(null, {}), false);
});

test("nested objects and arrays", () => {
  assert.equal(deepEqual({ a: [1, { b: 2 }] }, { a: [1, { b: 2 }] }), true);
  assert.equal(deepEqual({ a: [1, { b: 2 }] }, { a: [1, { b: 3 }] }), false);
  assert.equal(deepEqual([1, 2], [1, 2, 3]), false);
  assert.equal(deepEqual({ a: 1 }, { a: 1, b: undefined }), false);
  assert.equal(deepEqual({ a: 1, b: 2 }, { b: 2, a: 1 }), true); // key order irrelevant
});

test("array vs object with same keys", () => {
  assert.equal(deepEqual([1], { 0: 1 }), false);
  assert.equal(deepEqual([], {}), false);
});

test("dates and regexps", () => {
  assert.equal(deepEqual(new Date(5), new Date(5)), true);
  assert.equal(deepEqual(new Date(5), new Date(6)), false);
  assert.equal(deepEqual(new Date(NaN), new Date(NaN)), true);
  assert.equal(deepEqual(/a/g, /a/g), true);
  assert.equal(deepEqual(/a/g, /a/i), false);
});

test("maps and sets", () => {
  assert.equal(deepEqual(new Map([["x", { v: 1 }]]), new Map([["x", { v: 1 }]])), true);
  assert.equal(deepEqual(new Map([["x", 1]]), new Map([["y", 1]])), false);
  assert.equal(deepEqual(new Set([1, 2, 3]), new Set([3, 2, 1])), true);
  assert.equal(deepEqual(new Set([1, 2]), new Set([1, 3])), false);
  assert.equal(deepEqual(new Set([{ a: 1 }, { a: 2 }]), new Set([{ a: 2 }, { a: 1 }])), true);
  // each object in one set must match a distinct object in the other
  assert.equal(deepEqual(new Set([{ a: 1 }, { a: 2 }]), new Set([{ a: 1 }, { a: 1 }])), false);
});

test("typed arrays", () => {
  assert.equal(deepEqual(new Uint8Array([1, 2]), new Uint8Array([1, 2])), true);
  assert.equal(deepEqual(new Uint8Array([1, 2]), new Uint8Array([1, 3])), false);
  assert.equal(deepEqual(new Uint8Array([1]), new Int8Array([1])), false);
});

test("class instances compare by prototype and fields", () => {
  class P { constructor(public x: number) {} }
  class Q { constructor(public x: number) {} }
  assert.equal(deepEqual(new P(1), new P(1)), true);
  assert.equal(deepEqual(new P(1), new Q(1)), false);
  assert.equal(deepEqual(new P(1), { x: 1 }), false);
});

test("cyclic structures", () => {
  const a: any = { name: "a" };
  a.self = a;
  const b: any = { name: "a" };
  b.self = b;
  assert.equal(deepEqual(a, b), true);
  const c: any = { name: "c" };
  c.self = c;
  assert.equal(deepEqual(a, c), false);
  const l1: any[] = [1];
  l1.push(l1);
  const l2: any[] = [1];
  l2.push(l2);
  assert.equal(deepEqual(l1, l2), true);
});

test("randomized agreement with node's deepStrictEqual on JSON-like data", () => {
  let seed = 3;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  const gen = (depth: number): unknown => {
    const k = r(depth > 2 ? 3 : 5);
    if (k === 0) return r(3);
    if (k === 1) return ["x", "y"][r(2)];
    if (k === 2) return null;
    if (k === 3) return Array.from({ length: r(3) }, () => gen(depth + 1));
    return Object.fromEntries(Array.from({ length: r(3) }, () => ["abc"[r(3)], gen(depth + 1)]));
  };
  for (let i = 0; i < 500; i++) {
    const a = gen(0), b = r(2) ? structuredClone(a) : gen(0);
    let expected = true;
    try { assert.deepStrictEqual(a, b); } catch { expected = false; }
    assert.equal(deepEqual(a, b), expected);
  }
});
