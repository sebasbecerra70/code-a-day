import { test } from "node:test";
import assert from "node:assert/strict";
import { Router } from "./solution.ts";

function makeRouter() {
  return new Router<string>()
    .add("GET", "/", "root")
    .add("GET", "/users", "listUsers")
    .add("GET", "/users/me", "me")
    .add("GET", "/users/:id", "getUser")
    .add("GET", "/users/:id/posts/:postId", "getPost")
    .add("POST", "/users", "createUser")
    .add("GET", "/static/*path", "static");
}

test("static routes and root", () => {
  const r = makeRouter();
  assert.deepEqual(r.match("GET", "/"), { handler: "root", params: {} });
  assert.equal(r.match("GET", "/users")?.handler, "listUsers");
  assert.equal(r.match("POST", "/users")?.handler, "createUser");
});

test("params are extracted", () => {
  const r = makeRouter();
  assert.deepEqual(r.match("GET", "/users/42"), { handler: "getUser", params: { id: "42" } });
  assert.deepEqual(r.match("GET", "/users/7/posts/abc"), {
    handler: "getPost",
    params: { id: "7", postId: "abc" },
  });
});

test("static beats param", () => {
  assert.equal(makeRouter().match("GET", "/users/me")?.handler, "me");
});

test("wildcard captures the rest of the path", () => {
  const r = makeRouter();
  assert.deepEqual(r.match("GET", "/static/css/site.css"), {
    handler: "static",
    params: { path: "css/site.css" },
  });
  assert.deepEqual(r.match("GET", "/static"), { handler: "static", params: { path: "" } });
});

test("trailing slashes, query strings, and URL decoding", () => {
  const r = makeRouter();
  assert.equal(r.match("GET", "/users/")?.handler, "listUsers");
  assert.deepEqual(r.match("GET", "/users/42?x=1")?.params, { id: "42" });
  assert.deepEqual(r.match("GET", "/users/a%20b")?.params, { id: "a b" });
  assert.deepEqual(r.match("GET", "/users/%E0%A4%A")?.params, { id: "%E0%A4%A" });
});

test("no match cases", () => {
  const r = makeRouter();
  assert.equal(r.match("GET", "/nope"), undefined);
  assert.equal(r.match("GET", "/users/1/posts"), undefined);
  assert.equal(r.match("DELETE", "/users"), undefined);
});

test("backtracks from a failed static branch into a param branch", () => {
  const r = new Router<string>()
    .add("GET", "/a/b/c", "abc")
    .add("GET", "/a/:x/d", "axd");
  assert.deepEqual(r.match("GET", "/a/b/d"), { handler: "axd", params: { x: "b" } });
  assert.equal(r.match("GET", "/a/b/c")?.handler, "abc");
});

test("allowed methods", () => {
  assert.deepEqual(makeRouter().allowedMethods("/users").sort(), ["GET", "POST"]);
  assert.deepEqual(makeRouter().allowedMethods("/users/1"), ["GET"]);
});

test("registration errors", () => {
  const r = new Router<string>().add("GET", "/x/:id", "a");
  assert.throws(() => r.add("GET", "/x/:id", "b"), /duplicate/);
  assert.throws(() => r.add("GET", "/x/:other/y", "c"), /conflicting/);
  assert.throws(() => r.add("GET", "/files/*rest/more", "d"), /last segment/);
  assert.throws(() => r.add("GET", "/bad/:", "e"), /name/);
});
