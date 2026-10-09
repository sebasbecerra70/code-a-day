import { test } from "node:test";
import assert from "node:assert/strict";
import { Trie } from "./solution.ts";

test("insert, has, startsWith", () => {
  const t = new Trie();
  t.insert("apple");
  assert.equal(t.has("apple"), true);
  assert.equal(t.has("app"), false);
  assert.equal(t.startsWith("app"), true);
  assert.equal(t.startsWith("b"), false);
  t.insert("app");
  assert.equal(t.has("app"), true);
  assert.equal(t.size, 2);
});

test("frequency accumulates and size counts distinct words", () => {
  const t = new Trie();
  t.insert("go", 3);
  t.insert("go");
  assert.equal(t.frequency("go"), 4);
  assert.equal(t.frequency("g"), 0);
  assert.equal(t.size, 1);
});

test("autocomplete orders by frequency then alphabetically", () => {
  const t = new Trie();
  t.insert("car", 2);
  t.insert("cat", 5);
  t.insert("cart", 2);
  t.insert("care", 1);
  t.insert("dog", 9);
  assert.deepEqual(t.autocomplete("ca", 3), ["cat", "car", "cart"]);
  assert.deepEqual(t.autocomplete("ca", 10), ["cat", "car", "cart", "care"]);
  assert.deepEqual(t.autocomplete("x"), []);
  assert.deepEqual(t.autocomplete("ca", 0), []);
});

test("empty prefix and empty word", () => {
  const t = new Trie();
  t.insert("");
  t.insert("a");
  assert.equal(t.has(""), true);
  assert.deepEqual(t.autocomplete("", 5), ["", "a"]);
});

test("delete prunes but keeps shared prefixes", () => {
  const t = new Trie();
  t.insert("tea");
  t.insert("team");
  assert.equal(t.delete("team"), true);
  assert.equal(t.has("team"), false);
  assert.equal(t.startsWith("team"), false);
  assert.equal(t.has("tea"), true);
  assert.equal(t.delete("te"), false);
  assert.equal(t.delete("zzz"), false);
  assert.equal(t.delete("tea"), true);
  assert.equal(t.startsWith("t"), false);
  assert.equal(t.size, 0);
});

test("unicode characters", () => {
  const t = new Trie();
  t.insert("café");
  t.insert("😀smile");
  assert.equal(t.has("café"), true);
  assert.deepEqual(t.autocomplete("😀"), ["😀smile"]);
});

test("randomized cross-check against a Map", () => {
  let seed = 1;
  const rand = (n: number) => (seed = (seed * 16807) % 2147483647) % n;
  const t = new Trie();
  const ref = new Map<string, number>();
  for (let i = 0; i < 2000; i++) {
    const w = Array.from({ length: rand(4) }, () => "abc"[rand(3)]).join("");
    if (rand(4) === 0) {
      assert.equal(t.delete(w), ref.delete(w));
    } else {
      t.insert(w);
      ref.set(w, (ref.get(w) ?? 0) + 1);
    }
  }
  assert.equal(t.size, ref.size);
  for (const prefix of ["", "a", "ab", "c", "cba"]) {
    const expected = [...ref]
      .filter(([w]) => w.startsWith(prefix))
      .sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : 1))
      .slice(0, 4)
      .map(([w]) => w);
    assert.deepEqual(t.autocomplete(prefix, 4), expected);
  }
});
