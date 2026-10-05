import { test } from "node:test";
import assert from "node:assert/strict";
import { ladderLength, shortestLadder, allShortestLadders } from "./solution.ts";

const WORDS = ["hot", "dot", "dog", "lot", "log", "cog"];

const differsByOne = (a: string, b: string) =>
  a.length === b.length && [...a].filter((c, i) => c !== b[i]).length === 1;

function assertValidLadder(path: string[], begin: string, end: string, dict: string[]) {
  assert.equal(path[0], begin);
  assert.equal(path.at(-1), end);
  for (let i = 1; i < path.length; i++) {
    assert.ok(differsByOne(path[i - 1], path[i]), `${path[i - 1]} -> ${path[i]}`);
    assert.ok(dict.includes(path[i]));
  }
}

test("classic example", () => {
  assert.equal(ladderLength("hit", "cog", WORDS), 5);
  const p = shortestLadder("hit", "cog", WORDS)!;
  assert.equal(p.length, 5);
  assertValidLadder(p, "hit", "cog", WORDS);
});

test("end word missing from dictionary", () => {
  assert.equal(ladderLength("hit", "cog", ["hot", "dot", "dog", "lot", "log"]), 0);
  assert.equal(shortestLadder("hit", "cog", []), null);
});

test("begin equals end, and length mismatch", () => {
  assert.deepEqual(shortestLadder("cat", "cat", ["cat"]), ["cat"]);
  assert.equal(ladderLength("cat", "cats", ["cats"]), 0);
});

test("disconnected graph", () => {
  assert.equal(ladderLength("aaa", "zzz", ["aab", "zzz", "zzy"]), 0);
});

test("one step", () => {
  assert.deepEqual(shortestLadder("a", "c", ["a", "b", "c"]), ["a", "c"]);
});

test("all shortest ladders", () => {
  assert.deepEqual(allShortestLadders("hit", "cog", WORDS), [
    ["hit", "hot", "dot", "dog", "cog"],
    ["hit", "hot", "lot", "log", "cog"],
  ]);
  assert.deepEqual(allShortestLadders("hit", "cog", ["hot"]), []);
});

test("randomized: bidirectional BFS length matches plain BFS and all ladders agree", () => {
  let seed = 7;
  const rand = (n: number) => ((seed = (seed * 1103515245 + 12345) % 2 ** 31) % n);
  const word = () => Array.from({ length: 3 }, () => "abc"[rand(3)]).join("");
  const plainBfs = (b: string, e: string, list: string[]) => {
    const dict = new Set(list);
    if (!dict.has(e)) return 0;
    const dist = new Map([[b, 1]]);
    const q = [b];
    for (let i = 0; i < q.length; i++) {
      for (const w of dict) {
        if (!dist.has(w) && differsByOne(q[i], w)) {
          dist.set(w, dist.get(q[i])! + 1);
          q.push(w);
        }
      }
    }
    return dist.get(e) ?? 0;
  };
  for (let t = 0; t < 300; t++) {
    const list = Array.from({ length: rand(12) }, word);
    const b = word(), e = list.length ? list[rand(list.length)] : word();
    const expected = b === e && list.includes(e) ? 1 : plainBfs(b, e, list);
    assert.equal(ladderLength(b, e, list), expected, `${b} -> ${e} in ${list}`);
    const all = allShortestLadders(b, e, list);
    if (expected === 0) assert.equal(all.length, 0);
    for (const p of all) {
      assert.equal(p.length, expected);
      if (p.length > 1) assertValidLadder(p, b, e, list);
    }
  }
});
