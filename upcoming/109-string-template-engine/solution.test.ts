import { test } from "node:test";
import assert from "node:assert/strict";
import { render, compile, TemplateError } from "./solution.ts";

test("plain text and interpolation with dotted paths", () => {
  assert.equal(render("no tags", {}), "no tags");
  assert.equal(render("Hi {{name}}, from {{ user.city }}!", { name: "Ada", user: { city: "London" } }), "Hi Ada, from London!");
});

test("missing values render empty; zero and false render as text", () => {
  assert.equal(render("[{{a}}][{{a.b.c}}][{{z}}][{{f}}]", { z: 0, f: false }), "[][][0][false]");
});

test("escapes HTML by default, triple braces render raw", () => {
  const data = { x: `<b class="c">'&'</b>` };
  assert.equal(render("{{x}}", data), "&lt;b class=&quot;c&quot;&gt;&#39;&amp;&#39;&lt;/b&gt;");
  assert.equal(render("{{{x}}}", data), data.x);
});

test("filters chain left to right, custom filters supported", () => {
  assert.equal(render("{{ name | trim | upper }}", { name: "  ada " }), "ADA");
  assert.equal(render("{{ items | length }}", { items: [1, 2, 3] }), "3");
  assert.equal(render("{{ n | double }}", { n: 21 }, { double: (v) => Number(v) * 2 }), "42");
  assert.throws(() => compile("{{ x | nope }}"), /unknown filter "nope"/);
});

test("if / else, with empty arrays falsy", () => {
  const t = compile("{{#if user}}Hi {{user.name}}{{else}}Sign in{{/if}}");
  assert.equal(t({ user: { name: "Ada" } }), "Hi Ada");
  assert.equal(t({}), "Sign in");
  assert.equal(render("{{#if xs}}some{{else}}none{{/if}}", { xs: [] }), "none");
});

test("each with this, @index, @first/@last, and outer scope access", () => {
  const t = compile("{{#each items}}{{#if @first}}[{{/if}}{{@index}}:{{this.name}}@{{shop}}{{#if @last}}]{{else}},{{/if}}{{/each}}");
  assert.equal(t({ shop: "S", items: [{ name: "a" }, { name: "b" }] }), "[0:a@S,1:b@S]");
  assert.equal(render("{{#each xs}}{{.}} {{/each}}", { xs: [1, 2, 3] }), "1 2 3 ");
  assert.equal(render("{{#each xs}}x{{/each}}", { xs: "notarray" }), "");
});

test("nested blocks", () => {
  const src = "{{#each groups}}{{name}}:{{#each members}}{{#if active}}{{this.id}}{{/if}}{{/each}};{{/each}}";
  const data = { groups: [{ name: "g1", members: [{ id: 1, active: true }, { id: 2 }] }, { name: "g2", members: [] }] };
  assert.equal(render(src, data), "g1:1;g2:;");
});

test("comments are dropped", () => {
  assert.equal(render("a{{! ignore me }}b", {}), "ab");
});

test("syntax errors are reported at compile time", () => {
  for (const [src, msg] of [
    ["{{#if a}}x", /unclosed/],
    ["x{{/if}}", /unexpected/],
    ["{{#if a}}{{/each}}", /unexpected/],
    ["{{else}}", /unexpected \{\{else\}\}/],
    ["{{#if a}}{{else}}{{else}}{{/if}}", /unexpected \{\{else\}\}/],
    ["{{#with a}}{{/with}}", /unknown block/],
    ["{{}}", /empty tag/],
  ] as const) {
    assert.throws(() => compile(src), (e: unknown) => e instanceof TemplateError && msg.test((e as Error).message), src);
  }
});
