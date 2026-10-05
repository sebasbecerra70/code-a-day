import { test } from "node:test";
import assert from "node:assert/strict";
import { Container, Token, CircularDependencyError } from "./solution.ts";

interface Config { url: string }
interface Logger { log(m: string): void; lines: string[] }
class Db {
  constructor(readonly cfg: Config, readonly logger: Logger) {}
}

const CONFIG = new Token<Config>("Config");
const LOGGER = new Token<Logger>("Logger");
const DB = new Token<Db>("Db");
const REQUEST_ID = new Token<number>("RequestId");

const makeLogger = (): Logger => {
  const lines: string[] = [];
  return { lines, log: (m) => lines.push(m) };
};

test("resolves a dependency graph with typed tokens", () => {
  const c = Container.create()
    .value(CONFIG, { url: "pg://x" })
    .singleton(LOGGER, makeLogger)
    .singleton(DB, (c) => new Db(c.resolve(CONFIG), c.resolve(LOGGER)));
  const db = c.resolve(DB);
  assert.equal(db.cfg.url, "pg://x");
  assert.equal(db.logger, c.resolve(LOGGER));
});

test("lifetimes: transient vs singleton", () => {
  let n = 0;
  const T = new Token<number>("T"), S = new Token<number>("S");
  const c = Container.create().register(T, () => ++n).singleton(S, () => ++n);
  assert.notEqual(c.resolve(T), c.resolve(T));
  assert.equal(c.resolve(S), c.resolve(S));
});

test("scoped instances are per scope; singletons shared across scopes", () => {
  let next = 0;
  const c = Container.create().scoped(REQUEST_ID, () => ++next).singleton(LOGGER, makeLogger);
  const a = c.createScope(), b = c.createScope();
  assert.equal(a.resolve(REQUEST_ID), a.resolve(REQUEST_ID));
  assert.notEqual(a.resolve(REQUEST_ID), b.resolve(REQUEST_ID));
  assert.equal(a.resolve(LOGGER), b.resolve(LOGGER));
  assert.equal(a.resolve(LOGGER), c.resolve(LOGGER));
});

test("scoped from root is an error (captive dependency guard)", () => {
  const c = Container.create().scoped(REQUEST_ID, () => 1);
  assert.throws(() => c.resolve(REQUEST_ID), /use createScope/);
  // A singleton must not capture a scoped dependency either, even when first resolved inside a scope.
  const BAD = new Token<number>("Bad");
  c.singleton(BAD, (k) => k.resolve(REQUEST_ID));
  assert.throws(() => c.createScope().resolve(BAD), /use createScope/);
});

test("missing provider", () => {
  assert.throws(() => Container.create().resolve(DB), /no provider for Db/);
});

test("circular dependencies report the chain", () => {
  const A = new Token<unknown>("A"), B = new Token<unknown>("B"), C = new Token<unknown>("C");
  const c = Container.create()
    .register(A, (k) => k.resolve(B))
    .register(B, (k) => k.resolve(C))
    .register(C, (k) => k.resolve(A));
  assert.throws(
    () => c.resolve(A),
    (e: unknown) => e instanceof CircularDependencyError && e.chain.join(">") === "A>B>C>A",
  );
  // The container is still usable after the failure.
  const D = new Token<number>("D");
  c.register(D, () => 4);
  assert.equal(c.resolve(D), 4);
});

test("self-dependency is circular", () => {
  const A = new Token<unknown>("A");
  const c = Container.create().singleton(A, (k) => k.resolve(A));
  assert.throws(() => c.resolve(A), /A -> A/);
});

test("dispose runs in reverse order and only for the owning scope", () => {
  const order: string[] = [];
  const disposable = (name: string) => ({ dispose: () => order.push(name) });
  const X = new Token<{ dispose(): void }>("X"), Y = new Token<{ dispose(): void }>("Y"), Z = new Token<{ dispose(): void }>("Z");
  const c = Container.create()
    .singleton(X, () => disposable("x"))
    .singleton(Y, (k) => (k.resolve(X), disposable("y")))
    .scoped(Z, () => disposable("z"));
  const scope = c.createScope();
  scope.resolve(Y);
  scope.resolve(Z);
  scope.dispose();
  assert.deepEqual(order, ["z"]);
  c.dispose();
  assert.deepEqual(order, ["z", "y", "x"]);
});

test("registration only on the root", () => {
  assert.throws(() => Container.create().createScope().value(CONFIG, { url: "" }), /root/);
});
