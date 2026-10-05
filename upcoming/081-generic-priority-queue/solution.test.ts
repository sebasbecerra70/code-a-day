import { test } from "node:test";
import assert from "node:assert/strict";
import { PriorityQueue } from "./solution.ts";

interface Task { name: string; priority: number }
const byPriority = (a: Task, b: Task) => a.priority - b.priority;

test("pops in comparator order (min-heap of numbers)", () => {
  const pq = new PriorityQueue<number>((a, b) => a - b, [5, 1, 4]);
  pq.push(2);
  pq.push(3);
  assert.deepEqual([...pq.drain()], [1, 2, 3, 4, 5]);
  assert.equal(pq.pop(), undefined);
  assert.equal(pq.peek(), undefined);
});

test("max-heap of strings via comparator", () => {
  const pq = new PriorityQueue<string>((a, b) => b.localeCompare(a), ["pear", "apple", "zoo"]);
  assert.equal(pq.pop(), "zoo");
  assert.equal(pq.peek(), "pear");
});

test("update after changing priority (decrease and increase key)", () => {
  const a = { name: "a", priority: 5 }, b = { name: "b", priority: 3 }, c = { name: "c", priority: 4 };
  const pq = new PriorityQueue(byPriority, [a, b, c]);
  a.priority = 1;
  pq.update(a);
  assert.equal(pq.peek(), a);
  a.priority = 10;
  pq.update(a);
  assert.deepEqual([...pq.drain()].map((t) => t.name), ["b", "c", "a"]);
});

test("remove arbitrary values", () => {
  const tasks = [1, 2, 3, 4, 5].map((p) => ({ name: `t${p}`, priority: p }));
  const pq = new PriorityQueue(byPriority, tasks);
  assert.equal(pq.remove(tasks[2]), true);
  assert.equal(pq.remove(tasks[2]), false);
  assert.equal(pq.has(tasks[2]), false);
  assert.equal(pq.remove(tasks[0]), true); // remove the root
  assert.deepEqual([...pq.drain()].map((t) => t.priority), [2, 4, 5]);
});

test("duplicates are rejected; update of a missing value throws", () => {
  const t = { name: "x", priority: 1 };
  const pq = new PriorityQueue(byPriority, [t]);
  assert.throws(() => pq.push(t), /duplicate/);
  assert.throws(() => new PriorityQueue(byPriority, [t, t]), /duplicate/);
  assert.throws(() => pq.update({ name: "y", priority: 0 }), /not in queue/);
});

test("Dijkstra with decrease-key", () => {
  const edges: [number, number, number][] = [[0, 1, 4], [0, 2, 1], [2, 1, 2], [1, 3, 1], [2, 3, 5]];
  const n = 4;
  const nodes = Array.from({ length: n }, (_, id) => ({ id, dist: id === 0 ? 0 : Infinity }));
  const pq = new PriorityQueue<(typeof nodes)[number]>((a, b) => a.dist - b.dist, nodes);
  while (pq.size) {
    const u = pq.pop()!;
    for (const [from, to, w] of edges) {
      if (from !== u.id) continue;
      const v = nodes[to];
      if (pq.has(v) && u.dist + w < v.dist) {
        v.dist = u.dist + w;
        pq.update(v);
      }
    }
  }
  assert.deepEqual(nodes.map((x) => x.dist), [0, 3, 1, 4]);
});

test("randomized against sorting", () => {
  let seed = 19;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  const live = new Set<Task>();
  const pq = new PriorityQueue(byPriority);
  for (let i = 0; i < 4000; i++) {
    const op = r(5);
    const items = [...live];
    if (op <= 1 || items.length === 0) {
      const t = { name: `n${i}`, priority: r(100) };
      live.add(t);
      pq.push(t);
    } else if (op === 2) {
      const t = items[r(items.length)];
      t.priority = r(100);
      pq.update(t);
    } else if (op === 3) {
      const t = items[r(items.length)];
      live.delete(t);
      assert.equal(pq.remove(t), true);
    } else {
      const min = Math.min(...items.map((t) => t.priority));
      const t = pq.pop()!;
      assert.equal(t.priority, min);
      live.delete(t);
    }
    assert.equal(pq.size, live.size);
  }
});
