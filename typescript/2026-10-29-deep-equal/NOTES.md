# Deep equality (cycles, Maps, Sets, Dates)

**Problem:** Implement `deepEqual(a, b)` that compares two values structurally: nested objects and arrays, plus `Map`, `Set`, `Date`, `RegExp`, typed arrays, and cyclic references.

## Approach
- Start with `Object.is` (so `NaN` equals `NaN`, `+0` differs from `-0`). Non-objects that aren't `Object.is` equal are unequal.
- Require the same prototype, so `[]` ≠ `{}`, `Uint8Array` ≠ `Int8Array`, and different classes differ.
- Dispatch by type: `Date` by timestamp, `RegExp` by source+flags, typed arrays element-wise, arrays index-wise, `Map` by key lookup then deep value compare, plain objects by own enumerable keys.
- `Set` with object members needs matching: each object in `a` must find a distinct deep-equal partner in `b` (O(n²) for those members).
- **Cycles:** a `seen` map records pairs `(a, b)` currently being compared; if we meet the same pair again, assume equal (co-inductive reasoning) instead of recursing forever.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(total nodes) for trees; O(n²) worst case for Sets of objects |
| Space | O(total nodes) for `seen` + recursion depth |

## Interview talking points
- `JSON.stringify` comparison fails on key order, `undefined`, `NaN`, Maps/Sets, cycles.
- Which equality for leaves? `===` vs. `Object.is` vs. SameValueZero; Node's `deepStrictEqual` uses `Object.is`.
- Map keys that are objects are compared by identity here (matching `Map.has`); a deeper version would match keys structurally too.
- Recursion depth: very deep structures can overflow the stack; an explicit stack fixes that.
- Used in test frameworks, React memo comparisons (usually shallow for speed), and state diffing.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
