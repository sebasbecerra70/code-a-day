import { test } from "node:test";
import assert from "node:assert/strict";
import { TTLCache } from "./solution.ts";

function clock() {
  let t = 1000;
  return { now: () => t, advance: (ms: number) => void (t += ms) };
}

test("get returns values until they expire", () => {
  const c = clock();
  const cache = new TTLCache<string, number>(100, Infinity, c.now);
  cache.set("a", 1);
  c.advance(99);
  assert.equal(cache.get("a"), 1);
  c.advance(1);
  assert.equal(cache.get("a"), undefined);
  assert.equal(cache.has("a"), false);
});

test("per-entry ttl overrides default and ttl() reports remaining", () => {
  const c = clock();
  const cache = new TTLCache<string, string>(100, Infinity, c.now);
  cache.set("short", "x", 10).set("long", "y", 1000);
  c.advance(50);
  assert.equal(cache.get("short"), undefined);
  assert.equal(cache.ttl("long"), 950);
  assert.equal(cache.ttl("missing"), undefined);
});

test("set on existing key resets expiry", () => {
  const c = clock();
  const cache = new TTLCache<string, number>(100, Infinity, c.now);
  cache.set("a", 1);
  c.advance(80);
  cache.set("a", 2);
  c.advance(80);
  assert.equal(cache.get("a"), 2);
});

test("LRU eviction at maxSize, get refreshes recency, peek does not", () => {
  const c = clock();
  const cache = new TTLCache<string, number>(1000, 2, c.now);
  cache.set("a", 1).set("b", 2);
  cache.get("a"); // a is now most recent
  cache.set("c", 3); // evicts b
  assert.equal(cache.peek("b"), undefined);
  assert.equal(cache.peek("a"), 1); // peek does not refresh a, so a stays LRU
  cache.set("d", 4); // evicts a
  assert.equal(cache.has("a"), false);
  assert.equal(cache.has("c"), true);
  assert.equal(cache.size, 2);
});

test("expired entries are dropped before evicting live ones", () => {
  const c = clock();
  const cache = new TTLCache<string, number>(1000, 2, c.now);
  cache.set("old", 0, 10).set("live", 1);
  c.advance(20);
  cache.set("new", 2);
  assert.equal(cache.get("live"), 1);
  assert.equal(cache.get("new"), 2);
});

test("prune removes only expired entries", () => {
  const c = clock();
  const cache = new TTLCache<number, number>(100, Infinity, c.now);
  for (let i = 0; i < 10; i++) cache.set(i, i, i < 4 ? 10 : 500);
  c.advance(50);
  assert.equal(cache.prune(), 4);
  assert.equal(cache.size, 6);
});

test("getOrSet computes once while fresh", () => {
  const c = clock();
  const cache = new TTLCache<string, number>(100, Infinity, c.now);
  let calls = 0;
  const f = () => ++calls;
  assert.equal(cache.getOrSet("k", f), 1);
  assert.equal(cache.getOrSet("k", f), 1);
  c.advance(100);
  assert.equal(cache.getOrSet("k", f), 2);
});

test("delete and validation", () => {
  const cache = new TTLCache<string, number>(100);
  cache.set("a", 1);
  assert.equal(cache.delete("a"), true);
  assert.equal(cache.delete("a"), false);
  assert.throws(() => new TTLCache(0), RangeError);
  assert.throws(() => new TTLCache(10, 0), RangeError);
  assert.throws(() => cache.set("x", 1, -5), RangeError);
});

test("randomized against a simple model", () => {
  const c = clock();
  const cache = new TTLCache<number, number>(50, 5, c.now);
  const model = new Map<number, { v: number; exp: number; used: number }>();
  let tick = 0;
  let seed = 77;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  for (let i = 0; i < 3000; i++) {
    c.advance(r(10));
    const k = r(8);
    const now = c.now();
    for (const [mk, e] of model) if (e.exp <= now) model.delete(mk);
    if (r(2)) {
      const v = r(100);
      model.set(k, { v, exp: now + 50, used: ++tick });
      if (model.size > 5) {
        const lru = [...model].sort((a, b) => a[1].used - b[1].used)[0][0];
        model.delete(lru);
      }
      cache.set(k, v);
    } else {
      const e = model.get(k);
      if (e) e.used = ++tick;
      assert.equal(cache.get(k), e?.v);
    }
  }
});
