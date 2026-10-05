import { test } from "node:test";
import assert from "node:assert/strict";
import { backoffDelay, retry, RetryError } from "./solution.ts";

// Records requested delays instead of sleeping.
function fakeSleep() {
  const delays: number[] = [];
  return { delays, sleep: async (ms: number) => void delays.push(ms) };
}

function failTimes<T>(n: number, value: T, err: () => unknown = () => new Error("flaky")) {
  let calls = 0;
  const fn = async () => {
    calls++;
    if (calls <= n) throw err();
    return value;
  };
  return { fn, calls: () => calls };
}

test("returns immediately on success", async () => {
  const s = fakeSleep();
  assert.equal(await retry(async () => 42, { sleep: s.sleep }), 42);
  assert.deepEqual(s.delays, []);
});

test("retries until success with exponential delays (no jitter)", async () => {
  const s = fakeSleep();
  const f = failTimes(3, "ok");
  assert.equal(await retry(f.fn, { retries: 5, baseMs: 100, jitter: false, sleep: s.sleep }), "ok");
  assert.equal(f.calls(), 4);
  assert.deepEqual(s.delays, [100, 200, 400]);
});

test("gives up after retries and wraps the last error", async () => {
  const s = fakeSleep();
  const f = failTimes(10, "never");
  await assert.rejects(retry(f.fn, { retries: 2, sleep: s.sleep }), (e: unknown) => {
    assert.ok(e instanceof RetryError);
    assert.equal(e.attempts, 3);
    assert.equal((e.lastError as Error).message, "flaky");
    return true;
  });
  assert.equal(f.calls(), 3);
});

test("retries: 0 means a single attempt", async () => {
  const f = failTimes(1, "x");
  await assert.rejects(retry(f.fn, { retries: 0, sleep: fakeSleep().sleep }), RetryError);
  assert.equal(f.calls(), 1);
});

test("non-retryable errors are rethrown as-is", async () => {
  class Fatal extends Error {}
  const f = failTimes(5, "x", () => new Fatal("bad request"));
  await assert.rejects(
    retry(f.fn, { shouldRetry: (e) => !(e instanceof Fatal), sleep: fakeSleep().sleep }),
    Fatal,
  );
  assert.equal(f.calls(), 1);
});

test("delays are capped at maxMs", () => {
  const delays = [1, 2, 3, 4, 5, 6].map((a) => backoffDelay(a, { baseMs: 100, maxMs: 1000, jitter: false }));
  assert.deepEqual(delays, [100, 200, 400, 800, 1000, 1000]);
});

test("full jitter stays within [0, ceiling]", () => {
  assert.equal(backoffDelay(3, { baseMs: 100, random: () => 0 }), 0);
  assert.equal(backoffDelay(3, { baseMs: 100, random: () => 0.999999 }), 400);
  for (let i = 0; i < 1000; i++) {
    const d = backoffDelay(4, { baseMs: 50 });
    assert.ok(d >= 0 && d <= 400);
  }
});

test("onRetry sees attempt numbers and delays", async () => {
  const seen: [number, number][] = [];
  const f = failTimes(2, "ok");
  await retry(f.fn, {
    jitter: false,
    baseMs: 10,
    sleep: fakeSleep().sleep,
    onRetry: (_e, attempt, delay) => seen.push([attempt, delay]),
  });
  assert.deepEqual(seen, [[1, 10], [2, 20]]);
});

test("abort signal stops retrying (real timers)", async () => {
  const ac = new AbortController();
  let calls = 0;
  const p = retry(
    async () => {
      calls++;
      throw new Error("down");
    },
    { retries: 100, baseMs: 20, jitter: false, signal: ac.signal },
  );
  setTimeout(() => ac.abort(new Error("cancelled")), 30);
  await assert.rejects(p, /cancelled/);
  assert.ok(calls >= 1 && calls <= 3, `calls=${calls}`);
});
