# Debounce and throttle

**Problem:** Rate-limit a noisy event handler. *Debounce* waits until events stop for `wait` ms and then runs once. *Throttle* runs at most once every `wait` ms while events keep coming.

## Approach
- **debounce:** every call clears the pending timer and starts a new one with the latest args. Optional `leading` fires the first call of a burst immediately and only fires a trailing call if more calls arrived.
- **throttle:** the first call runs immediately and opens a cool-down window. Calls during the window just store the latest args. When the window closes, run the stored call (if any) and open another window. This gives a leading call plus a trailing call with the final value.
- Both return a function with `cancel()` and `flush()`.
- Tests use `node:test`'s `mock.timers` to advance time deterministically.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| each call | O(1) | O(1) (latest args only) |

## Interview talking points
- When to use which: debounce for search-as-you-type, resize end, autosave; throttle for scroll/mousemove handlers, progress updates.
- Leading vs. trailing edges and why both matter (trailing ensures the final state is handled).
- `this` binding: a production version would use `function` and `fn.apply(this, args)`.
- Returning a Promise of the result, or `maxWait` (lodash's debounce can force a run during a never-ending burst; lodash's throttle is debounce with `maxWait = wait`).
- In React, wrap with `useMemo`/`useRef` so the debounced function isn't recreated every render.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
