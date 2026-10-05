import { test } from "node:test";
import assert from "node:assert/strict";
import { MaxQueue, MinStack, TwoStackMaxQueue } from "./solution.ts";

test("min stack tracks minimum through pushes and pops", () => {
  const s = new MinStack();
  s.push(-2);
  s.push(0);
  s.push(-3);
  assert.equal(s.min(), -3);
  assert.equal(s.pop(), -3);
  assert.equal(s.top(), 0);
  assert.equal(s.min(), -2);
});

test("min stack handles duplicate minimums", () => {
  const s = new MinStack();
  for (const x of [2, 1, 1, 3]) s.push(x);
  s.pop();
  s.pop();
  assert.equal(s.min(), 1);
  s.pop();
  assert.equal(s.min(), 2);
  assert.equal(s.size, 1);
});

test("empty stack throws", () => {
  const s = new MinStack();
  assert.throws(() => s.pop(), RangeError);
  assert.throws(() => s.top(), RangeError);
  assert.throws(() => s.min(), RangeError);
});

for (const Q of [MaxQueue, TwoStackMaxQueue]) {
  test(`${Q.name}: FIFO order and max`, () => {
    const q = new Q();
    for (const x of [1, 3, 2, 5, 4]) q.enqueue(x);
    assert.equal(q.max(), 5);
    assert.equal(q.dequeue(), 1);
    assert.equal(q.dequeue(), 3);
    assert.equal(q.dequeue(), 2);
    assert.equal(q.max(), 5);
    assert.equal(q.dequeue(), 5);
    assert.equal(q.max(), 4);
    assert.equal(q.size, 1);
  });

  test(`${Q.name}: duplicates of the max and empty errors`, () => {
    const q = new Q();
    q.enqueue(7);
    q.enqueue(7);
    q.dequeue();
    assert.equal(q.max(), 7);
    q.dequeue();
    assert.throws(() => q.max(), RangeError);
    assert.throws(() => q.dequeue(), RangeError);
  });
}

test("randomized against naive arrays", () => {
  let seed = 5;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  const stack = new MinStack();
  const q1 = new MaxQueue();
  const q2 = new TwoStackMaxQueue();
  const refStack: number[] = [];
  const refQueue: number[] = [];
  for (let i = 0; i < 5000; i++) {
    const x = r(50) - 25;
    if (r(3) || refStack.length === 0) {
      stack.push(x);
      refStack.push(x);
    } else {
      assert.equal(stack.pop(), refStack.pop());
    }
    if (refStack.length) assert.equal(stack.min(), Math.min(...refStack));

    if (r(3) || refQueue.length === 0) {
      q1.enqueue(x);
      q2.enqueue(x);
      refQueue.push(x);
    } else {
      const expected = refQueue.shift();
      assert.equal(q1.dequeue(), expected);
      assert.equal(q2.dequeue(), expected);
    }
    if (refQueue.length) {
      const m = Math.max(...refQueue);
      assert.equal(q1.max(), m);
      assert.equal(q2.max(), m);
      assert.equal(q1.front(), refQueue[0]);
    }
    assert.equal(q1.size, refQueue.length);
  }
});
