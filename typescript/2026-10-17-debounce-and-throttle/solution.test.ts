import { test, beforeEach, afterEach, mock } from "node:test";
import assert from "node:assert/strict";
import { debounce, throttle } from "./solution.ts";

beforeEach(() => mock.timers.enable({ apis: ["setTimeout"] }));
afterEach(() => mock.timers.reset());

test("debounce fires once with the last args after quiet period", () => {
  const calls: number[] = [];
  const d = debounce((x: number) => calls.push(x), 100);
  d(1);
  mock.timers.tick(50);
  d(2);
  mock.timers.tick(50);
  d(3);
  mock.timers.tick(99);
  assert.deepEqual(calls, []);
  mock.timers.tick(1);
  assert.deepEqual(calls, [3]);
});

test("debounce separate bursts fire separately", () => {
  const calls: number[] = [];
  const d = debounce((x: number) => calls.push(x), 10);
  d(1);
  mock.timers.tick(10);
  d(2);
  mock.timers.tick(10);
  assert.deepEqual(calls, [1, 2]);
});

test("debounce leading fires immediately, then trailing only if more calls", () => {
  const calls: number[] = [];
  const d = debounce((x: number) => calls.push(x), 100, { leading: true });
  d(1);
  assert.deepEqual(calls, [1]);
  mock.timers.tick(100);
  assert.deepEqual(calls, [1]); // no trailing duplicate
  d(2); // new burst: leading edge fires again
  d(3);
  assert.deepEqual(calls, [1, 2]);
  mock.timers.tick(100);
  assert.deepEqual(calls, [1, 2, 3]);
});

test("debounce cancel and flush", () => {
  const calls: number[] = [];
  const d = debounce((x: number) => calls.push(x), 100);
  d(1);
  d.cancel();
  mock.timers.tick(200);
  assert.deepEqual(calls, []);
  d(2);
  d.flush();
  assert.deepEqual(calls, [2]);
  mock.timers.tick(200);
  assert.deepEqual(calls, [2]); // flushed call not repeated
  d.flush(); // nothing pending: no-op
  assert.deepEqual(calls, [2]);
});

test("throttle runs leading call and latest trailing call", () => {
  const calls: number[] = [];
  const t = throttle((x: number) => calls.push(x), 100);
  t(1);
  t(2);
  t(3);
  assert.deepEqual(calls, [1]);
  mock.timers.tick(100);
  assert.deepEqual(calls, [1, 3]);
  mock.timers.tick(100);
  assert.deepEqual(calls, [1, 3]);
});

test("throttle caps rate under continuous calls", () => {
  let count = 0;
  const t = throttle(() => count++, 100);
  for (let ms = 0; ms < 1000; ms += 10) {
    t();
    mock.timers.tick(10);
  }
  // one per 100ms window
  assert.ok(count >= 10 && count <= 11, `count=${count}`);
});

test("throttle after idle fires immediately again", () => {
  const calls: number[] = [];
  const t = throttle((x: number) => calls.push(x), 50);
  t(1);
  mock.timers.tick(200);
  t(2);
  assert.deepEqual(calls, [1, 2]);
});

test("throttle cancel drops trailing call", () => {
  const calls: number[] = [];
  const t = throttle((x: number) => calls.push(x), 50);
  t(1);
  t(2);
  t.cancel();
  mock.timers.tick(100);
  assert.deepEqual(calls, [1]);
  t(3);
  assert.deepEqual(calls, [1, 3]);
});
