import { test } from "node:test";
import assert from "node:assert/strict";
import { shallowEqual, Store, type Middleware } from "./solution.ts";

type State = { count: number; todos: string[]; user: { name: string } };
type Action =
  | { type: "inc"; by?: number }
  | { type: "addTodo"; text: string }
  | { type: "rename"; name: string }
  | { type: "noop" };

const initial: State = { count: 0, todos: [], user: { name: "ann" } };

function reducer(s: State, a: Action): State {
  switch (a.type) {
    case "inc": return { ...s, count: s.count + (a.by ?? 1) };
    case "addTodo": return { ...s, todos: [...s.todos, a.text] };
    case "rename": return a.name === s.user.name ? s : { ...s, user: { name: a.name } };
    default: return s;
  }
}

test("dispatch updates state via reducer", () => {
  const store = new Store(reducer, initial);
  store.dispatch({ type: "inc" });
  store.dispatch({ type: "inc", by: 4 });
  assert.equal(store.getState().count, 5);
  assert.equal(initial.count, 0); // initial object untouched
});

test("subscribers get new and previous state; unsubscribe works", () => {
  const store = new Store(reducer, initial);
  const seen: [number, number][] = [];
  const unsub = store.subscribe((s, p) => seen.push([s.count, p.count]));
  store.dispatch({ type: "inc" });
  unsub();
  store.dispatch({ type: "inc" });
  assert.deepEqual(seen, [[1, 0]]);
});

test("no notification when reducer returns same state", () => {
  const store = new Store(reducer, initial);
  let calls = 0;
  store.subscribe(() => calls++);
  store.dispatch({ type: "noop" });
  store.dispatch({ type: "rename", name: "ann" });
  assert.equal(calls, 0);
});

test("select fires only when the selected slice changes", () => {
  const store = new Store(reducer, initial);
  const names: string[] = [];
  store.select((s) => s.user.name, (n) => names.push(n));
  store.dispatch({ type: "inc" });
  store.dispatch({ type: "addTodo", text: "x" });
  store.dispatch({ type: "rename", name: "bob" });
  assert.deepEqual(names, ["bob"]);
});

test("select with shallowEqual ignores fresh-but-equal objects", () => {
  const store = new Store(reducer, initial);
  let calls = 0;
  store.select((s) => ({ n: s.todos.length }), () => calls++, shallowEqual);
  store.dispatch({ type: "inc" });
  assert.equal(calls, 0);
  store.dispatch({ type: "addTodo", text: "a" });
  assert.equal(calls, 1);
});

test("listener unsubscribing another during notify is safe", () => {
  const store = new Store(reducer, initial);
  const log: string[] = [];
  let unsubB = () => {};
  store.subscribe(() => { log.push("a"); unsubB(); });
  unsubB = store.subscribe(() => log.push("b"));
  store.dispatch({ type: "inc" });
  store.dispatch({ type: "inc" });
  assert.deepEqual(log, ["a", "b", "a"]);
});

test("reducer cannot dispatch", () => {
  let store!: Store<number, string>;
  store = new Store<number, string>((s, a) => {
    if (a === "bad") store.dispatch("x");
    return s + 1;
  }, 0);
  assert.throws(() => store.dispatch("bad"), /may not dispatch/);
  store.dispatch("ok"); // flag reset after the throw
  assert.equal(store.getState(), 1);
});

test("middleware runs in order and can block actions", () => {
  const log: string[] = [];
  const logger: Middleware<State, Action> = (s, next) => (a) => {
    log.push(`before ${a.type} ${s.getState().count}`);
    next(a);
    log.push(`after ${s.getState().count}`);
  };
  const blockNoop: Middleware<State, Action> = (_s, next) => (a) => {
    if (a.type !== "noop") next(a);
    else log.push("blocked");
  };
  const store = new Store(reducer, initial, [logger, blockNoop]);
  store.dispatch({ type: "inc" });
  store.dispatch({ type: "noop" });
  assert.deepEqual(log, ["before inc 0", "after 1", "before noop 1", "blocked", "after 1"]);
});

test("shallowEqual", () => {
  assert.equal(shallowEqual({ a: 1 }, { a: 1 }), true);
  assert.equal(shallowEqual({ a: {} }, { a: {} }), false);
  assert.equal(shallowEqual([1, 2], [1, 2]), true);
  assert.equal(shallowEqual({ a: 1 }, { b: 1 }), false);
  assert.equal(shallowEqual(null, {}), false);
});
