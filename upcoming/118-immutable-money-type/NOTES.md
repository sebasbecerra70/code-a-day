# Immutable Money value type (allocation without losing cents)

**Problem:** Design a `Money` type that is exact, immutable and currency-safe: parse and format amounts, add, subtract, multiply by tax or FX rates with explicit rounding, and split an amount by ratios so that no cent is lost or invented.

## Approach
- **Representation:** a `long` count of **minor units** plus a `java.util.Currency`. The currency's `getDefaultFractionDigits()` handles USD (2), JPY (0) and KWD (3) without special cases.
- **Immutability:** `final` class, `final` fields, a private constructor, and every operation returns a new instance. That makes it safe to share, cache and use as a map key.
- **Safety:** mixing currencies throws, and overflow uses `Math.*Exact`. Parsing rejects excess precision (`1.001 USD`) instead of silently rounding.
- **Decimal multiply:** go through `BigDecimal` and require a `RoundingMode` from the caller (banker's `HALF_EVEN` for accounting, `HALF_UP` for retail). Rounding is a business decision, so it shouldn't be hidden in the type.
- **Allocation (largest remainder method):** give each share `floor(total × ratio / sum)`, then hand out the leftover units to the shares with the largest dropped fractions (ties go to the earliest share). Every share then ends up within one unit of its exact value, and zero-ratio shares get nothing. Negative amounts allocate their absolute value and restore the sign. `BigDecimal` keeps `total × ratio` from overflowing.
- Value semantics: `equals`, `hashCode` and `compareTo` are based on (minor units, currency).

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| plus / minus / times(long) | O(1) | O(1) |
| times(BigDecimal) | O(digits) | O(1) |
| allocate(k ratios) | O(k log k) | O(k) |

## Interview talking points
- **Never use `double` for money:** `0.1 + 0.2 != 0.3`. Integer minor units are fast and exact; `BigDecimal` is the alternative when you need fractional cents (interest accrual, FX), and in that case you have to manage its scale.
- Allocation is the classic trap: `$100 / 3 = 33.33 × 3 = 99.99`. Every remainder unit must go somewhere, deterministically. Fowler's version in *Patterns of Enterprise Application Architecture* hands leftovers to the first shares in order, which is simpler but can put a share a whole unit away from its fair value. Largest remainder (Hamilton's apportionment method) avoids that.
- Watch out for `BigDecimal.equals`: `new BigDecimal("1.0")` isn't equal to `new BigDecimal("1.00")` because the scale differs. Normalizing to minor units avoids this.
- Immutable value objects make reasoning, concurrency and caching trivial. Contrast with `java.util.Date`, which was mutable.
- In practice, mention JSR 354 (`javax.money`, Moneta) and storing amounts as integer minor units plus an ISO-4217 code in databases.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
