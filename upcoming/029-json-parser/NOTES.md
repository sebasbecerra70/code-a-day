# JSON parser (recursive descent)

**Problem:** Write `parseJSON(text)` from scratch: accept exactly the JSON grammar (RFC 8259), build the value, and reject invalid input with a useful error position.

## Approach
- One cursor `i` into the string and a function per grammar rule: `parseValue` dispatches on the first non-whitespace character to `parseObject`, `parseArray`, `parseString`, `parseNumber`, or a literal (`true`/`false`/`null`).
- **Numbers:** a sticky regex (`/y`) matches the exact JSON number grammar at the cursor, which rejects `01`, `1.`, `.5`, `+1`.
- **Strings:** handle the 8 simple escapes and `\uXXXX`. Surrogate pairs come as two escapes and join naturally as UTF-16 code units. Raw control characters are rejected.
- **Objects** use `Object.create(null)` so a `"__proto__"` key is plain data, not prototype pollution.
- After the top-level value, only whitespace may remain.
- Errors are a `JSONParseError` with the position.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(n) (string building with `+=` is amortized fine in V8) |
| Space | O(n) output + O(depth) recursion |

## Interview talking points
- Recursive descent maps 1:1 onto the grammar; JSON is LL(1), so one character of lookahead is enough.
- Deeply nested input can overflow the stack; production parsers use an explicit stack or a depth limit.
- Number precision: JSON allows arbitrary precision, JS doubles don't (big integers lose digits). Options: return strings or BigInt.
- Streaming/incremental parsing (SAX style) for huge documents.
- Security: prototype pollution via `__proto__`, and duplicate keys (spec says behavior is undefined; most parsers keep the last).

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
