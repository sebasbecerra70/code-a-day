import { test } from "node:test";
import assert from "node:assert/strict";
import { StateMachine, reachableStates, type MachineConfig } from "./solution.ts";

type Light = "green" | "yellow" | "red";
const light = () =>
  new StateMachine<Light, "TIMER">({
    initial: "green",
    context: undefined,
    states: { green: { on: { TIMER: "yellow" } }, yellow: { on: { TIMER: "red" } }, red: { on: { TIMER: "green" } } },
  });

type DoorState = "locked" | "closed" | "open";
type DoorEvent = "UNLOCK" | "LOCK" | "OPEN" | "CLOSE";
interface DoorCtx { code: string; attempts: number }
const doorConfig = (): MachineConfig<DoorState, DoorEvent, DoorCtx> => ({
  initial: "locked",
  context: { code: "1234", attempts: 0 },
  states: {
    locked: {
      on: {
        UNLOCK: [
          { target: "closed", guard: (c, p) => p === c.code, action: (c) => ({ ...c, attempts: 0 }) },
          { target: "locked", action: (c) => ({ ...c, attempts: c.attempts + 1 }) },
        ],
      },
    },
    closed: { on: { LOCK: "locked", OPEN: "open" } },
    open: { on: { CLOSE: "closed" } },
  },
});

test("simple cycle of transitions", () => {
  const m = light();
  assert.equal(m.state, "green");
  m.send("TIMER");
  m.send("TIMER");
  assert.equal(m.state, "red");
  m.send("TIMER");
  assert.equal(m.state, "green");
});

test("events with no transition are ignored", () => {
  const m = new StateMachine(doorConfig());
  assert.equal(m.send("OPEN"), false);
  assert.equal(m.state, "locked");
  assert.equal(m.can("OPEN"), false);
});

test("guards pick the first passing transition; actions update context", () => {
  const m = new StateMachine(doorConfig());
  assert.equal(m.send("UNLOCK", "0000"), true); // self-transition via fallback
  assert.equal(m.state, "locked");
  assert.equal(m.context.attempts, 1);
  assert.equal(m.can("UNLOCK", "1234"), true);
  m.send("UNLOCK", "1234");
  assert.equal(m.state, "closed");
  assert.equal(m.context.attempts, 0);
  assert.deepEqual(m.availableEvents().sort(), ["LOCK", "OPEN"]);
});

test("exit, action, and enter run in order; subscribers notified after", () => {
  const log: string[] = [];
  const m = new StateMachine<"a" | "b", "GO", null>({
    initial: "a",
    context: null,
    states: {
      a: { onEnter: () => log.push("enter a"), onExit: () => log.push("exit a"), on: { GO: { target: "b", action: () => void log.push("action") } } },
      b: { onEnter: () => log.push("enter b") },
    },
  });
  const unsub = m.subscribe((s, e) => log.push(`notify ${e}->${s}`));
  m.send("GO");
  assert.deepEqual(log, ["enter a", "exit a", "action", "enter b", "notify GO->b"]);
  unsub();
});

test("unsubscribe stops notifications", () => {
  const m = light();
  let n = 0;
  const unsub = m.subscribe(() => n++);
  m.send("TIMER");
  unsub();
  m.send("TIMER");
  assert.equal(n, 1);
});

test("final states accept no further events", () => {
  const m = new StateMachine<"pending" | "paid" | "cancelled", "PAY" | "CANCEL", undefined>({
    initial: "pending",
    context: undefined,
    states: { pending: { on: { PAY: "paid", CANCEL: "cancelled" } }, paid: { final: true }, cancelled: { final: true } },
  });
  m.send("PAY");
  assert.equal(m.done, true);
  assert.equal(m.send("CANCEL"), false);
  assert.equal(m.state, "paid");
});

test("re-entrant send from a hook is rejected", () => {
  const m: StateMachine<"a" | "b", "GO", undefined> = new StateMachine<"a" | "b", "GO", undefined>({
    initial: "a",
    context: undefined,
    states: { a: { on: { GO: "b" } }, b: { onEnter: () => m.send("GO"), on: { GO: "a" } } },
  });
  assert.throws(() => m.send("GO"), /re-entrantly/);
});

test("config validation and reachability", () => {
  assert.throws(
    () => new StateMachine({ initial: "a", context: 0, states: { a: { on: { X: "zzz" as "a" } } } }),
    /unknown state "zzz"/,
  );
  const cfg: MachineConfig<"a" | "b" | "orphan", "X", undefined> = {
    initial: "a",
    context: undefined,
    states: { a: { on: { X: "b" } }, b: { on: { X: "a" } }, orphan: {} },
  };
  assert.deepEqual([...reachableStates(cfg)].sort(), ["a", "b"]);
});
