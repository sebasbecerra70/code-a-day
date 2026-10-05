# Schema validator with inferred static types

**Problem:** Validate untrusted `unknown` data (JSON bodies, config files) against a declared schema, report every problem with a path like `items.1.qty`, and get a precise TypeScript type out of the same declaration, the way zod does.

## Approach
- Every schema is a `Schema<T>` with one method, `check(input, path, issues)`, which validates, appends issues, and returns the value. `parse`/`safeParse` are built on top.
- Combinators (`object`, `array`, `union`, `optional`, `refine`) wrap child schemas and extend the path as they recurse, so errors are collected rather than thrown at the first problem.
- The phantom type parameter `T` makes `Infer<typeof S>` work via `S extends Schema<infer T> ? T : never`.
- Object inference: keys whose schema accepts `undefined` become `?:` properties, using a mapped type plus a `Flatten` helper so hovers stay readable.
- Objects strip unknown keys by default. `.strict()` reports them instead.
- `union` tries each member against a scratch issue list and commits the first one that passes.
- `refine` runs its predicate only when the base schema produced no new issues, so the predicate always sees a correctly typed value.

## Complexity
| Aspect | Cost |
|--------|------|
| Time | O(size of input) per validation |
| Space | O(depth) for paths + O(issues) |

## Interview talking points
- "Parse, don't validate": the output is a typed value, so downstream code needs no casts.
- Why collect all issues instead of failing fast? Better UX for forms and APIs. Fail-fast is a trivial option to add.
- Type-level tricks: conditional types with `infer`, key remapping for optional properties, and why `Flatten` exists.
- Union error reporting is hard; zod reports per-branch issues, and discriminated unions make it precise.
- Extensions: transforms/coercion (`s.coerce.number()`), defaults, recursive schemas via lazy thunks, JSON Schema export.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
