# Typed finite state machine (guards, actions, hooks)

**Problem:** Model workflows such as order lifecycles, UI flows, connection handling, and traffic lights as a finite state machine. Invalid transitions should be impossible, behavior should be declarative, and TypeScript should catch typos in state and event names.

## Approach
- The machine is **data**: `{ initial, context, states: { [state]: { on: { [event]: target | transition | transition[] } } } }`. State and event names are string-literal union types, so `send("OPNE")` doesn't compile.
- A transition can have a **guard** (`(ctx, payload) => boolean`) and an **action** that returns a new context, which keeps updates immutable. An array of transitions is tried in order and the first passing guard wins, which gives a natural if/else (see the door's fallback self-transition that counts failed attempts).
- Order on `send`: pick transition, run `onExit` of the old state, run the action, switch state, run `onEnter` of the new state, then notify subscribers.
- Unknown or blocked events return `false` instead of throwing. Final states ignore everything.
- Re-entrant `send` from inside hooks throws, so a transition can never be interrupted halfway. (XState queues such events instead.)
- Construction validates that every target exists. `reachableStates` runs a BFS over the transition graph to find dead states.

## Complexity
| Aspect | Cost |
|--------|------|
| send / can | O(g) guards checked for that event |
| Construction | O(total transitions) |
| Reachability | O(S + T) |
| Space | O(S + T) |

## Interview talking points
- Why FSMs? They replace a tangle of boolean flags (`isLoading && !isError && ...`) with explicit states, so impossible combinations can't be represented.
- Mealy vs Moore machines: outputs on transitions (actions) vs outputs on states (entry hooks). This implementation supports both.
- Statecharts (Harel, as in XState) add hierarchy, parallel states, and history, which helps against state explosion.
- Guards keep the machine finite while still depending on data (the context is "extended state").
- Testing: enumerate states x events into a table, and use reachability to find dead or unreachable states.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
