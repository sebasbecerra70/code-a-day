import { test } from "node:test";
import assert from "node:assert/strict";
import { CircularBuffer, MovingAverage } from "./solution.ts";

test("FIFO behavior with wraparound", () => {
  const b = new CircularBuffer<number>(3);
  b.push(1);
  b.push(2);
  assert.equal(b.shift(), 1);
  b.push(3);
  b.push(4); // wraps to index 0
  assert.deepEqual(b.toArray(), [2, 3, 4]);
  assert.equal(b.isFull, true);
});

test("rejects writes when full without overwrite", () => {
  const b = new CircularBuffer<string>(2);
  b.push("a");
  b.push("b");
  assert.throws(() => b.push("c"), RangeError);
  assert.deepEqual(b.toArray(), ["a", "b"]);
});

test("overwrite mode evicts the oldest and returns it", () => {
  const b = new CircularBuffer<number>(3, true);
  for (const x of [1, 2, 3]) assert.equal(b.push(x), undefined);
  assert.equal(b.push(4), 1);
  assert.equal(b.push(5), 2);
  assert.deepEqual([...b], [3, 4, 5]);
});

test("pop from back, at() with negative index, peeks", () => {
  const b = new CircularBuffer<number>(4, true);
  for (const x of [1, 2, 3, 4, 5]) b.push(x);
  assert.equal(b.peekFront(), 2);
  assert.equal(b.peekBack(), 5);
  assert.equal(b.at(-2), 4);
  assert.equal(b.at(10), undefined);
  assert.equal(b.pop(), 5);
  assert.deepEqual(b.toArray(), [2, 3, 4]);
});

test("empty errors, clear, and capacity validation", () => {
  const b = new CircularBuffer<number>(1);
  assert.throws(() => b.shift(), RangeError);
  assert.throws(() => b.pop(), RangeError);
  assert.equal(b.peekFront(), undefined);
  b.push(9);
  b.clear();
  assert.equal(b.isEmpty, true);
  b.push(10);
  assert.deepEqual(b.toArray(), [10]);
  assert.throws(() => new CircularBuffer(0), RangeError);
  assert.throws(() => new CircularBuffer(2.5), RangeError);
});

test("moving average", () => {
  const m = new MovingAverage(3);
  assert.equal(m.next(1), 1);
  assert.equal(m.next(10), 5.5);
  assert.equal(m.next(3), 14 / 3);
  assert.equal(m.next(5), 6);
});

test("randomized against an array model (overwrite mode)", () => {
  let seed = 8;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  for (const cap of [1, 2, 5, 8]) {
    const b = new CircularBuffer<number>(cap, true);
    const model: number[] = [];
    for (let i = 0; i < 3000; i++) {
      const op = r(4);
      if (op < 2) {
        const x = r(1000);
        const evicted = b.push(x);
        model.push(x);
        assert.equal(evicted, model.length > cap ? model.shift() : undefined);
      } else if (model.length) {
        assert.equal(op === 2 ? b.shift() : b.pop(), op === 2 ? model.shift() : model.pop());
      }
      assert.deepEqual(b.toArray(), model);
      assert.equal(b.size, model.length);
    }
  }
});
