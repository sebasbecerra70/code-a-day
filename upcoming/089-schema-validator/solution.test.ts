import { test } from "node:test";
import assert from "node:assert/strict";
import { s, ValidationError, type Infer } from "./solution.ts";

const User = s.object({
  name: s.string().refine((v) => v.length > 0, "must not be empty"),
  age: s.number().refine(Number.isInteger, "must be an integer"),
  email: s.string().optional(),
  tags: s.array(s.string()),
  role: s.union(s.literal("admin"), s.literal("user")),
});
type User = Infer<typeof User>;

test("parses valid input and infers types", () => {
  const u: User = User.parse({ name: "Ada", age: 36, tags: ["math"], role: "admin" });
  assert.equal(u.name, "Ada");
  assert.equal(u.email, undefined);
  assert.ok(!("email" in u));
});

test("primitives reject wrong types (and NaN)", () => {
  assert.equal(s.string().safeParse(1).ok, false);
  assert.equal(s.number().safeParse("1").ok, false);
  assert.equal(s.number().safeParse(NaN).ok, false);
  assert.equal(s.boolean().safeParse(false).ok, true);
});

test("collects every issue with its path", () => {
  const r = User.safeParse({ name: "", age: 1.5, tags: ["ok", 7], role: "root" });
  assert.equal(r.ok, false);
  if (r.ok) return;
  assert.deepEqual(
    r.issues.map((i) => i.path.join(".")),
    ["name", "age", "tags.1", "role"],
  );
  assert.match(r.issues[2].message, /expected string, got number/);
});

test("missing required key vs optional key", () => {
  const r = User.safeParse({ age: 1, tags: [], role: "user" });
  assert.equal(r.ok, false);
  if (!r.ok) assert.deepEqual(r.issues, [{ path: ["name"], message: "expected string, got undefined" }]);
});

test("non-object root and arrays-as-objects are rejected", () => {
  for (const bad of [null, 5, "x", []]) {
    const r = User.safeParse(bad);
    assert.equal(r.ok, false);
  }
});

test("unknown keys are stripped by default, rejected in strict mode", () => {
  const P = s.object({ x: s.number() });
  assert.deepEqual(P.parse({ x: 1, extra: true }), { x: 1 });
  const r = P.strict().safeParse({ x: 1, extra: true });
  assert.equal(r.ok, false);
  if (!r.ok) assert.deepEqual(r.issues[0].path, ["extra"]);
});

test("refine runs only after base validation passes", () => {
  let calls = 0;
  const S = s.number().refine((n) => (calls++, n > 0), "must be positive");
  assert.equal(S.safeParse("nope").ok, false);
  assert.equal(calls, 0);
  assert.equal(S.safeParse(-1).ok, false);
  assert.equal(calls, 1);
});

test("parse throws ValidationError with a readable message", () => {
  assert.throws(() => User.parse({ name: "A", age: "x", tags: [], role: "user" }), (e: unknown) => {
    assert.ok(e instanceof ValidationError);
    assert.equal(e.message, "age: expected number, got string");
    return true;
  });
  assert.throws(() => s.string().parse(1), /\(root\): expected string/);
});

test("nested objects and arrays of objects", () => {
  const Order = s.object({ items: s.array(s.object({ sku: s.string(), qty: s.number() })) });
  const r = Order.safeParse({ items: [{ sku: "a", qty: 1 }, { sku: "b" }] });
  assert.equal(r.ok, false);
  if (!r.ok) assert.deepEqual(r.issues[0].path, ["items", 1, "qty"]);
});
