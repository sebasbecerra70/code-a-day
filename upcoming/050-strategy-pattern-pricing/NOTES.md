# Strategy pattern (pluggable pricing rules)

**Problem:** A checkout must support many promotions (percent off, buy-X-get-Y, threshold discounts, combinations) and different rules per customer tier, without a growing `if/switch` in the checkout code.

## Approach
- **Strategy interface:** `PricingStrategy.discountCents(items)`. It's a `@FunctionalInterface`, so any lambda is a strategy.
- **Concrete strategies** are static factories: `percentOff`, `buyXGetYFree`, `amountOffOver`. Each one is small, pure and testable on its own.
- **Composites:** `bestOf` (promotions don't stack, so pick the largest) and `stacked` (sum them, capped at the subtotal). Composites are also strategies, so they nest freely.
- **Context:** `Checkout` maps each `CustomerTier` to a strategy (an `EnumMap`) and can swap them at runtime. It also enforces the invariant `0 ≤ discount ≤ subtotal`, so a buggy rule can't produce a negative total.
- Money is held as `long` cents with `Math.*Exact` overflow checks, and percentages round half-up.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| Simple strategy | O(n) items | O(1) |
| Composite of k strategies | O(k · n) | O(k) |
| Checkout total | O(n) plus the strategy | O(1) |

## Interview talking points
- Strategy is about the open/closed principle: adding a promotion means adding a class or lambda, not editing `Checkout`.
- In modern Java, strategies are often just lambdas or method references (`Comparator` is the canonical example). Use a full class when the strategy has state or configuration worth naming.
- Strategy vs State: both swap behavior behind an interface, but in State the *object itself* moves between states, while a Strategy is chosen by the client.
- Composite strategies (best-of, stacked) show how patterns combine. Discuss business rules such as which promotions stack, the order of application (percent before or after a flat amount) and rounding policy.
- Never use `double` for money. Use integer minor units or `BigDecimal` with an explicit `RoundingMode`.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
