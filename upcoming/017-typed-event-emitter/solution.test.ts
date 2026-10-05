import { test } from "node:test";
import assert from "node:assert/strict";
import { TypedEmitter } from "./solution.ts";

type Events = {
  message: [from: string, text: string];
  count: [n: number];
  ready: [];
};

test("on/emit passes typed args in order", () => {
  const e = new TypedEmitter<Events>();
  const got: string[] = [];
  e.on("message", (from, text) => got.push(`1:${from}:${text}`));
  e.on("message", (from) => got.push(`2:${from}`));
  assert.equal(e.emit("message", "ann", "hi"), true);
  assert.deepEqual(got, ["1:ann:hi", "2:ann"]);
});

test("emit with no listeners returns false", () => {
  const e = new TypedEmitter<Events>();
  assert.equal(e.emit("ready"), false);
});

test("unsubscribe function and off", () => {
  const e = new TypedEmitter<Events>();
  let total = 0;
  const add = (n: number) => (total += n);
  const unsub = e.on("count", add);
  e.emit("count", 2);
  unsub();
  e.emit("count", 5);
  assert.equal(total, 2);
  assert.equal(e.listenerCount("count"), 0);
  e.off("count", add); // removing again is harmless
});

test("once fires a single time", () => {
  const e = new TypedEmitter<Events>();
  let calls = 0;
  e.once("ready", () => calls++);
  e.emit("ready");
  e.emit("ready");
  assert.equal(calls, 1);
  assert.equal(e.listenerCount("ready"), 0);
});

test("once can be cancelled before firing", () => {
  const e = new TypedEmitter<Events>();
  let calls = 0;
  const cancel = e.once("ready", () => calls++);
  cancel();
  e.emit("ready");
  assert.equal(calls, 0);
});

test("same listener registered twice is called twice; off removes one", () => {
  const e = new TypedEmitter<Events>();
  let calls = 0;
  const fn = () => calls++;
  e.on("ready", fn);
  e.on("ready", fn);
  e.emit("ready");
  e.off("ready", fn);
  e.emit("ready");
  assert.equal(calls, 3);
});

test("listeners added or removed during emit don't affect the current emit", () => {
  const e = new TypedEmitter<Events>();
  const log: string[] = [];
  const b = () => log.push("b");
  e.on("ready", () => {
    log.push("a");
    e.off("ready", b);
    e.on("ready", () => log.push("c"));
  });
  e.on("ready", b);
  e.emit("ready");
  assert.deepEqual(log, ["a", "b"]);
  log.length = 0;
  e.emit("ready");
  assert.deepEqual(log, ["a", "c"]);
});

test("removeAllListeners for one event or all", () => {
  const e = new TypedEmitter<Events>();
  e.on("ready", () => {});
  e.on("count", () => {});
  e.removeAllListeners("ready");
  assert.equal(e.listenerCount("ready"), 0);
  assert.equal(e.listenerCount("count"), 1);
  e.removeAllListeners();
  assert.equal(e.listenerCount("count"), 0);
});

test("waitFor resolves with next emission", async () => {
  const e = new TypedEmitter<Events>();
  const p = e.waitFor("message");
  e.emit("message", "bob", "yo");
  assert.deepEqual(await p, ["bob", "yo"]);
});

test("compile-time checks (type-level only)", () => {
  const e = new TypedEmitter<Events>();
  // @ts-expect-error wrong payload type
  e.emit("count", "nope");
  // @ts-expect-error unknown event
  e.on("nope", () => {});
  assert.ok(true);
});
