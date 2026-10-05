# Rod cutting (unbounded knapsack DP)

**Problem:** A rod of length n can be cut into integer pieces, and a piece of length i sells for `price[i]`. Find the maximum revenue and the cuts that achieve it. Variant: each cut has a fixed cost.

## Approach
- **Recurrence:** decide the first piece's length k, then solve the rest optimally: `best[len] = max over k of price[k] + best[len − k]`, with `best[0] = 0`. This is an unbounded knapsack where the capacity is the length and each item (piece length) can be used any number of times.
- **Reconstruction:** store the optimal first piece for each length, then peel pieces off from n.
- **Cut cost:** subtract `cutCost` whenever the first piece leaves a remainder (that cut is real), and don't subtract it when the whole remaining rod is sold.
- Pieces longer than the price list are unsellable, so the inner loop is capped at `price.length`.
- Tests check the CLRS table, compare top-down and bottom-up, and compare against brute force over all 2ⁿ⁻¹ cut patterns.

## Complexity
| Version | Time | Space |
|---------|------|-------|
| Bottom-up / memoized | O(n · min(n, P)) for P priced lengths | O(n) |
| Brute force | O(2ⁿ⁻¹ · n) | O(1) |

## Interview talking points
- **Optimal substructure:** after the first cut, the remainder must itself be cut optimally (cut-and-paste argument).
- Why does "first piece + rest" suffice instead of trying every left/right split? Every cut pattern has a well-defined first piece, so this enumerates each one once, at O(n) per state instead of O(n²).
- A greedy rule (best price per unit length first) fails: with CLRS prices, length 4 by density takes a 3 (8/3 ≈ 2.67) plus a 1 = 9, but 2 + 2 = 10.
- Same family: coin change (count ways, or fewest coins), unbounded knapsack and integer break (maximize the product).
- The cut-cost variant shows how to fold a per-transition penalty into the recurrence without adding a state dimension.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
