# Observer pattern (stock price alerts)

**Problem:** A price feed publishes ticks for many symbols. Any number of independent components (alerts, charts, loggers) should react without the feed knowing about them, and they must be able to subscribe and unsubscribe at any time.

## Approach
- **Subject:** `PriceFeed` keeps the listeners for each symbol plus a `*` wildcard list, and remembers the last price so unchanged ticks are suppressed.
- **Observer:** `PriceListener.onPrice(PriceUpdate)` is a functional interface, so lambdas and method references work as observers.
- **Subscription handle:** `subscribe` returns an `AutoCloseable` whose `close()` unsubscribes (idempotently). Callers don't need to keep the listener reference around for removal.
- **Safe iteration:** `CopyOnWriteArrayList` notifies over a snapshot, so a listener can unsubscribe itself (or add others) during a callback.
- **Isolation:** each callback runs in `try/catch`, and failures go to an error handler. One bad observer can't block the rest.
- **Concrete observers:** `ThresholdAlert` fires on *crossing* (edge-triggered) and re-arms when the price moves back. `PercentMoveAlert` fires on large single-tick moves.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| subscribe / unsubscribe | O(k) copy-on-write for k listeners on that symbol | O(k) |
| publish | O(k_symbol + k_all) | O(1) |

## Interview talking points
- Observer decouples the producer from consumers: the feed depends only on an interface. This is the basis of event buses, UI frameworks and reactive streams.
- **Lapsed listener leak:** a subscriber that's never removed keeps itself (and everything it references) alive. Returning a `Subscription` handle, or using weak references, addresses this.
- Copy-on-write is ideal when notifications vastly outnumber subscription changes. Otherwise, copy the list under a lock before iterating.
- **Synchronous vs asynchronous delivery:** here callbacks run on the publisher's thread, so a slow observer slows the feed. Async delivery (one queue per subscriber) adds ordering and backpressure questions, which is what Reactive Streams and `java.util.concurrent.Flow` define.
- Edge-triggered vs level-triggered alerts: firing on every tick above the threshold would spam the user. Hysteresis bands are a common refinement.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
