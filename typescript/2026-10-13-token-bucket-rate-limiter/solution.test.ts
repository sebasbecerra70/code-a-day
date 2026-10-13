import { test } from "node:test";
import assert from "node:assert/strict";
import { KeyedRateLimiter, TokenBucket } from "./solution.ts";

function fakeClock(start = 0) {
  let t = start;
  return { now: () => t, advance: (ms: number) => void (t += ms) };
}

test("allows a burst up to capacity then rejects", () => {
  const c = fakeClock();
  const b = new TokenBucket(3, 1, c.now);
  assert.deepEqual([1, 2, 3, 4].map(() => b.tryConsume()), [true, true, true, false]);
});

test("refills over time and caps at capacity", () => {
  const c = fakeClock();
  const b = new TokenBucket(2, 2, c.now); // 2 tokens/sec
  b.tryConsume(2);
  c.advance(250);
  assert.equal(b.tryConsume(), false); // only 0.5 tokens
  c.advance(250);
  assert.equal(b.tryConsume(), true);
  c.advance(60_000);
  assert.equal(b.available, 2);
});

test("multi-token cost", () => {
  const c = fakeClock();
  const b = new TokenBucket(5, 1, c.now);
  assert.equal(b.tryConsume(4), true);
  assert.equal(b.tryConsume(2), false);
  assert.equal(b.tryConsume(1), true);
});

test("waitTime reports how long until allowed", () => {
  const c = fakeClock();
  const b = new TokenBucket(2, 4, c.now);
  assert.equal(b.waitTime(), 0);
  b.tryConsume(2);
  assert.equal(b.waitTime(), 250);
  assert.equal(b.waitTime(3), Infinity); // exceeds capacity
  c.advance(250);
  assert.equal(b.tryConsume(), true);
});

test("zero refill never recovers", () => {
  const c = fakeClock();
  const b = new TokenBucket(1, 0, c.now);
  assert.equal(b.tryConsume(), true);
  c.advance(1e9);
  assert.equal(b.tryConsume(), false);
  assert.equal(b.waitTime(), Infinity);
});

test("clock going backward does not mint tokens", () => {
  const c = fakeClock(10_000);
  const b = new TokenBucket(1, 1, c.now);
  b.tryConsume();
  c.advance(-5_000);
  assert.equal(b.tryConsume(), false);
});

test("validates arguments", () => {
  assert.throws(() => new TokenBucket(0, 1), RangeError);
  assert.throws(() => new TokenBucket(1, -1), RangeError);
  assert.throws(() => new TokenBucket(1, 1).tryConsume(0), RangeError);
});

test("keyed limiter isolates keys", () => {
  const c = fakeClock();
  const rl = new KeyedRateLimiter(1, 1, c.now);
  assert.equal(rl.allow("alice"), true);
  assert.equal(rl.allow("alice"), false);
  assert.equal(rl.allow("bob"), true);
  c.advance(1000);
  assert.equal(rl.allow("alice"), true);
});

test("long-run throughput matches refill rate", () => {
  const c = fakeClock();
  const b = new TokenBucket(5, 10, c.now);
  let allowed = 0;
  for (let ms = 0; ms < 10_000; ms += 10) {
    c.advance(10);
    if (b.tryConsume()) allowed++;
  }
  // 5 initial burst + 10/sec * 10 sec
  assert.ok(Math.abs(allowed - 105) <= 1, `allowed=${allowed}`);
});
