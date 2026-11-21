# Observable store with selectors

**Problem:** Build a tiny state container (think Redux or Zustand): state changes only through `dispatch(action)` and a pure reducer, components subscribe to changes, and selector subscriptions fire only when the slice they care about actually changes.

## Approach
- `Store<S, A>` holds the state, a reducer, and a `Set` of listeners.
- `dispatch` runs the reducer and notifies listeners with `(state, prev)`. If the reducer returns the **same reference**, nothing changed, so no one is notified. This is why reducers must return new objects on change.
- Listeners are iterated over a snapshot, so (un)subscribing during a notification is safe.
- `select(selector, onChange, equals)` caches the last selected value and only calls `onChange` when `equals` says it differs. `shallowEqual` handles selectors that build fresh objects.
- A `dispatching` flag forbids reducers from dispatching, to keep updates predictable.
- **Middleware** wraps `dispatch` (`store => next => action`), composed so the first listed runs outermost: logging, blocking, async thunks.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| dispatch | O(reducer + L·selector) | O(L) snapshot |
| subscribe / unsubscribe | O(1) | O(1) |

## Interview talking points
- Reference equality as change detection is cheap but requires immutable updates (spread, Immer).
- Selector memoization (reselect) avoids recomputing derived data; `useSyncExternalStore` is how React subscribes to such stores safely.
- Batching notifications (one notify per tick) reduces re-renders.
- Why a reducer instead of `setState`? Actions are serializable: logging, time-travel debugging, replay.
- Signals/observables (fine-grained reactivity) as the alternative model.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
