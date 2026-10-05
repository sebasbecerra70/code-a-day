import { test } from "node:test";
import assert from "node:assert/strict";
import { applyDiff, diffLines, formatDiff, lcs } from "./solution.ts";

const lines = (s: string) => (s === "" ? [] : s.split(""));

test("lcs of classic example", () => {
  assert.equal(lcs(lines("ABCBDAB"), lines("BDCABA")).length, 4);
  assert.deepEqual(lcs(lines("abc"), lines("abc")), ["a", "b", "c"]);
  assert.deepEqual(lcs(lines("abc"), lines("xyz")), []);
});

test("identical inputs produce only equal ops", () => {
  const a = ["x", "y", "z"];
  assert.ok(diffLines(a, a).every((op) => op.kind === "equal"));
});

test("pure insertions and deletions", () => {
  assert.equal(formatDiff(diffLines([], ["a", "b"])), "+a\n+b");
  assert.equal(formatDiff(diffLines(["a", "b"], [])), "-a\n-b");
  assert.deepEqual(diffLines([], []), []);
});

test("modified line in the middle", () => {
  const a = ["import x", "const a = 1;", "export a"];
  const b = ["import x", "const a = 2;", "export a"];
  assert.equal(formatDiff(diffLines(a, b)), " import x\n-const a = 1;\n+const a = 2;\n export a");
});

test("moved block shows as delete + insert", () => {
  const a = ["a", "b", "c", "d"];
  const b = ["c", "d", "a", "b"];
  const ops = diffLines(a, b);
  assert.equal(ops.filter((o) => o.kind === "equal").length, 2);
  assert.deepEqual(applyDiff(a, ops), b);
});

test("applyDiff rejects mismatched input", () => {
  const ops = diffLines(["a"], ["b"]);
  assert.throws(() => applyDiff(["z"], ops), /does not apply/);
  assert.throws(() => applyDiff(["a", "extra"], ops), /consume/);
});

test("randomized: diff is minimal and applies cleanly", () => {
  let seed = 13;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  const bruteLcs = (a: string[], b: string[]): number => {
    const dp = Array.from({ length: a.length + 1 }, () => new Array(b.length + 1).fill(0));
    for (let i = 1; i <= a.length; i++)
      for (let j = 1; j <= b.length; j++)
        dp[i][j] = a[i - 1] === b[j - 1] ? dp[i - 1][j - 1] + 1 : Math.max(dp[i - 1][j], dp[i][j - 1]);
    return dp[a.length][b.length];
  };
  for (let t = 0; t < 300; t++) {
    const a = Array.from({ length: r(10) }, () => "xyz"[r(3)]);
    const b = Array.from({ length: r(10) }, () => "xyz"[r(3)]);
    const ops = diffLines(a, b);
    assert.deepEqual(applyDiff(a, ops), b);
    const k = bruteLcs(a, b);
    assert.equal(ops.filter((o) => o.kind === "equal").length, k);
    assert.equal(ops.length, a.length + b.length - k); // minimal edit script
  }
});
