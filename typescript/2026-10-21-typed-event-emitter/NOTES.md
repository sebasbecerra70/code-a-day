# Type-safe event emitter

**Problem:** Build a pub/sub event emitter where the compiler rejects unknown event names and wrong payloads, with `on`, `once`, `off`, `emit`, and a promise-based `waitFor`.

## Approach
- The class is generic over an `Events` map: `{ message: [from: string, text: string]; ready: [] }`. Methods take `K extends keyof Events` and listeners typed `(...args: Events[K]) => void`, so `emit("count", "nope")` is a type error.
- Listeners live in a `Map<event, listener[]>`, called in registration order.
- `emit` iterates a **snapshot** of the list, so listeners that subscribe or unsubscribe during an emit don't cause skipped or extra calls.
- `once` wraps the listener in a self-removing function; its returned unsubscribe cancels the wrapper.
- `waitFor` turns the next emission into a Promise.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| on / once | O(1) amortized | O(1) per listener |
| off | O(L) for that event | — |
| emit | O(L) (copy + call) | O(L) snapshot |

## Interview talking points
- Using tuple types (not a single payload object) lets listeners have named, multiple params.
- Why snapshot during emit? Mutating an array while iterating leads to skipped listeners.
- Error handling: one throwing listener stops the rest here; alternatives are try/catch per listener or an `error` event (Node crashes on unhandled `error`).
- Memory leaks from forgotten listeners; Node warns past `maxListeners`. `WeakRef` or `AbortSignal`-based unsubscription helps.
- Sync vs. async delivery (`queueMicrotask`) affects ordering guarantees.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
