import { test } from "node:test";
import assert from "node:assert/strict";
import { JSONParseError, parseJSON } from "./solution.ts";

// Compare as plain data (our objects have a null prototype).
const same = (text: string) =>
  assert.deepEqual(JSON.parse(JSON.stringify(parseJSON(text))), JSON.parse(text));

test("literals and numbers", () => {
  assert.equal(parseJSON("true"), true);
  assert.equal(parseJSON("false"), false);
  assert.equal(parseJSON("null"), null);
  assert.equal(parseJSON("0"), 0);
  assert.equal(parseJSON("-12.5e+2"), -1250);
  assert.equal(parseJSON("1E3"), 1000);
});

test("strings with escapes and unicode", () => {
  assert.equal(parseJSON(String.raw`"a\"b\\c\/d\n\t"`), 'a"b\\c/d\n\t');
  assert.equal(parseJSON(String.raw`"é"`), "é");
  assert.equal(parseJSON(String.raw`"😀"`), "😀");
  assert.equal(parseJSON('"naïve 😀"'), "naïve 😀");
});

test("nested structures and whitespace", () => {
  same(' { "a" : [ 1 , 2 , { "b" : null } ] ,\n "c" : "d" , "e": {} , "f": [] } ');
  same("[[[[]]]]");
});

test("duplicate keys: last one wins", () => {
  assert.equal((parseJSON('{"a":1,"a":2}') as any).a, 2);
});

test("__proto__ key is stored as data", () => {
  const o = parseJSON('{"__proto__": {"x": 1}}') as any;
  assert.deepEqual(Object.keys(o), ["__proto__"]);
  assert.equal(({} as any).x, undefined);
});

const invalid = [
  "", " ", "{", "[1,]", "[1 2]", '{"a" 1}', "{a:1}", '{"a":1,}', "01", "1.", ".5", "+1",
  "-", "1e", "tru", "nul", '"abc', '"\\x"', '"\\u12G4"', '"tab\there"', "[1] x", "'single'", "NaN",
];
test("rejects malformed input", () => {
  for (const bad of invalid) {
    assert.throws(() => parseJSON(bad), JSONParseError, `should reject ${JSON.stringify(bad)}`);
  }
});

test("error includes position", () => {
  try {
    parseJSON("[1, 2, x]");
    assert.fail("should throw");
  } catch (e) {
    assert.ok(e instanceof JSONParseError);
    assert.equal(e.position, 7);
  }
});

test("round-trips random values against JSON.stringify", () => {
  let seed = 9;
  const r = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  const strs = ["", "x", 'q"uote', "back\\slash", "nl\n", "é😀", "\u0001"];
  const gen = (d: number): unknown => {
    switch (r(d > 3 ? 4 : 6)) {
      case 0: return [null, true, false][r(3)];
      case 1: return (r(2000) - 1000) / [1, 4, 1000][r(3)];
      case 2: return strs[r(strs.length)];
      case 3: return r(1e9) * 1e10;
      case 4: return Array.from({ length: r(4) }, () => gen(d + 1));
      default: return Object.fromEntries(Array.from({ length: r(4) }, () => [strs[r(strs.length)], gen(d + 1)]));
    }
  };
  for (let k = 0; k < 300; k++) {
    const text = JSON.stringify(gen(0), null, r(2) ? 2 : undefined);
    same(text);
  }
});
