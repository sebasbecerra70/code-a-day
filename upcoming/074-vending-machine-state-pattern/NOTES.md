# Vending machine (State pattern + change making)

**Problem:** Model a vending machine whose response to each event (insert coin, select, cancel, maintenance) depends on its mode: idle, holding money, sold out or in maintenance. Return correct change from a *limited* coin supply, and never dispense if change can't be made.

## Approach
- **State pattern:** a `VendingState` interface with one method per event, plus default implementations that reject the event. `IdleState`, `HasMoneyState`, `SoldOutState` and `MaintenanceState` override only what they allow. `VendingMachine` (the context) just delegates, so there's no `switch (state)` anywhere.
- **Transitions live in the states:** for example, `HasMoneyState.select` moves to `Idle`, or to `SoldOut` if that was the last item.
- **Transaction safety:** in `select`, check the slot, stock, balance and change availability **before** mutating anything. If any check fails, the customer can still add coins or cancel.
- Inserted coins go into the machine's coin supply immediately, so they can be used as change, and are removed again on refund.
- **Change making with limited coins:** greedy fails (30¢ from {1 quarter, 3 dimes} needs three dimes). A bounded-knapsack DP treats each physical coin as a 0/1 item and finds the fewest coins. Tests compare it against exhaustive search.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| insertCoin / cancel | O(1) / O(inserted) | — |
| select (change DP) | O(C × A) for C coins in stock, amount A in cents | O(C × A) to reconstruct |
| State transition | O(1) | — |

## Interview talking points
- State vs `switch`: with a switch, every event method branches on every state, so adding a state touches every method. With the State pattern, adding a state is one class, and each class reads like one row of the state table.
- Default methods that throw make "illegal in this state" the safe default.
- Greedy change works for *canonical* coin systems with unlimited supply (US coins), but not with limited supply or non-canonical sets like {1, 3, 4}.
- Real machines also need cash-box limits, timeouts that auto-cancel, and handling power loss mid-dispense (persisting the state is one way).
- Stateless state objects (as here) could be shared singletons; stateful ones (with a timer, say) are created per transition.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
