import { test } from "node:test";
import assert from "node:assert/strict";
import { createLimiter, mapWithLimit, settleWithLimit } from "./solution.ts";

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** Wraps a task to record peak concurrency. */
function tracker() {
  let active = 0;
  const t = { peak: 0, started: [] as number[] };
  const wrap = async <R>(id: number, work: () => Promise<R>) => {
    t.started.push(id);
    active++;
    t.peak = Math.max(t.peak, active);
    try {
      return await work();
    } finally {
      active--;
    }
  };
  return { t, wrap };
}

test("preserves input order even when tasks finish out of order", async () => {
  const delays = [30, 5, 20, 1, 10];
  const out = await mapWithLimit(delays, 2, async (d, i) => {
    await sleep(d);
    return i * 10;
  });
  assert.deepEqual(out, [0, 10, 20, 30, 40]);
});

test("never exceeds the limit and reaches it", async () => {
  const { t, wrap } = tracker();
  await mapWithLimit(Array.from({ length: 20 }, (_, i) => i), 3, (i) =>
    wrap(i, () => sleep(1 + (i % 4))),
  );
  assert.equal(t.peak, 3);
  assert.equal(t.started.length, 20);
});

test("empty input and limit larger than input", async () => {
  assert.deepEqual(await mapWithLimit([], 4, async (x) => x), []);
  assert.deepEqual(await mapWithLimit([1, 2], 100, async (x) => x * 2), [2, 4]);
});

test("limit 1 runs sequentially", async () => {
  const log: string[] = [];
  await mapWithLimit(["a", "b", "c"], 1, async (x) => {
    log.push(`start ${x}`);
    await sleep(1);
    log.push(`end ${x}`);
  });
  assert.deepEqual(log, ["start a", "end a", "start b", "end b", "start c", "end c"]);
});

test("rejects on first failure and stops starting new tasks", async () => {
  const started: number[] = [];
  await assert.rejects(
    mapWithLimit([0, 1, 2, 3, 4, 5, 6, 7], 2, async (i) => {
      started.push(i);
      await sleep(2);
      if (i === 1) throw new Error("boom");
      return i;
    }),
    /boom/,
  );
  await sleep(20);
  assert.ok(started.length < 8, `started ${started.length}`);
});

test("invalid limits", async () => {
  await assert.rejects(mapWithLimit([1], 0, async (x) => x), RangeError);
  await assert.rejects(mapWithLimit([1], 1.5, async (x) => x), RangeError);
  assert.throws(() => createLimiter(0), RangeError);
});

test("settleWithLimit reports every outcome", async () => {
  const res = await settleWithLimit([1, 2, 3], 2, async (x) => {
    if (x === 2) throw new Error("two");
    return x;
  });
  assert.equal(res[0].status, "fulfilled");
  assert.equal(res[1].status, "rejected");
  assert.equal((res[2] as PromiseFulfilledResult<number>).value, 3);
});

test("createLimiter bounds concurrency across independent callers", async () => {
  const limit = createLimiter(2);
  const { t, wrap } = tracker();
  const results = await Promise.all(
    Array.from({ length: 10 }, (_, i) => limit(() => wrap(i, async () => (await sleep(2), i)))),
  );
  assert.deepEqual(results, [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
  assert.equal(t.peak, 2);
  // FIFO: tasks start in submission order
  assert.deepEqual(t.started, [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]);
});

test("createLimiter releases the slot when a task throws", async () => {
  const limit = createLimiter(1);
  await assert.rejects(limit(async () => { throw new Error("x"); }));
  assert.equal(await limit(async () => 7), 7);
});
