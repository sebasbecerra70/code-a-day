import { test } from "node:test";
import assert from "node:assert/strict";
import { markdownToHtml as md, renderInline } from "./solution.ts";

test("headings h1-h6 with optional closing hashes", () => {
  assert.equal(md("# Title"), "<h1>Title</h1>");
  assert.equal(md("###### six ##"), "<h6>six</h6>");
  assert.equal(md("####### seven"), "<p>####### seven</p>");
  assert.equal(md("#nospace"), "<p>#nospace</p>");
});

test("paragraphs join soft-wrapped lines and split on blank lines", () => {
  assert.equal(md("one\ntwo\n\nthree"), "<p>one two</p>\n<p>three</p>");
  assert.equal(md(""), "");
  assert.equal(md("\n\n  \n"), "");
});

test("emphasis, strong, links, and inline code", () => {
  assert.equal(renderInline("**bold** and *it* and __b__ and _i_"), "<strong>bold</strong> and <em>it</em> and <strong>b</strong> and <em>i</em>");
  assert.equal(renderInline("see [docs](https://x.dev/a?b=1)"), 'see <a href="https://x.dev/a?b=1">docs</a>');
  assert.equal(renderInline("use `a*b*c` here"), "use <code>a*b*c</code> here");
  assert.equal(renderInline("2 * 3 * 4"), "2 * 3 * 4"); // spaced asterisks are not emphasis
});

test("HTML is escaped everywhere, javascript: links are neutralized", () => {
  assert.equal(md("<script>alert(1)</script>"), "<p>&lt;script&gt;alert(1)&lt;/script&gt;</p>");
  assert.equal(renderInline("`<b>`"), "<code>&lt;b&gt;</code>");
  assert.equal(renderInline("[x](JavaScript:steal)"), "x");
});

test("fenced code blocks keep content literal", () => {
  const src = "```ts\nconst a = 1 < 2;\n# not a heading\n```";
  assert.equal(md(src), '<pre><code class="language-ts">const a = 1 &lt; 2;\n# not a heading</code></pre>');
  assert.equal(md("```\nunclosed"), "<pre><code>unclosed</code></pre>");
});

test("unordered and ordered lists", () => {
  assert.equal(md("- a\n- *b*\n* c"), "<ul>\n<li>a</li>\n<li><em>b</em></li>\n<li>c</li>\n</ul>");
  assert.equal(md("1. x\n2. y"), "<ol>\n<li>x</li>\n<li>y</li>\n</ol>");
  assert.equal(md("- a\n1. b"), "<ul>\n<li>a</li>\n</ul>\n<ol>\n<li>b</li>\n</ol>");
});

test("blockquotes recurse into block parsing", () => {
  assert.equal(md("> # Hi\n> text"), "<blockquote>\n<h1>Hi</h1>\n<p>text</p>\n</blockquote>");
});

test("horizontal rules and paragraph interruption", () => {
  assert.equal(md("---"), "<hr>");
  assert.equal(md("* * *"), "<hr>");
  assert.equal(md("para\n# Head\n- item"), "<p>para</p>\n<h1>Head</h1>\n<ul>\n<li>item</li>\n</ul>");
});

test("full document", () => {
  const doc = ["# Notes", "", "Intro with **bold**.", "", "- one", "- two", "", "```", "x", "```"].join("\r\n");
  assert.equal(
    md(doc),
    "<h1>Notes</h1>\n<p>Intro with <strong>bold</strong>.</p>\n<ul>\n<li>one</li>\n<li>two</li>\n</ul>\n<pre><code>x</code></pre>",
  );
});
