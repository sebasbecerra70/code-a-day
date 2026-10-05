# Matrix chain multiplication (interval DP)

**Problem:** Given matrices A1…An where Ai is `dims[i-1] × dims[i]`, choose the parenthesization that minimizes the number of scalar multiplications. Matrix multiplication is associative, but the cost depends heavily on the order.

## Approach
- **State:** `cost[i][j]` is the minimum cost to multiply matrices i through j.
- **Transition:** the last multiplication splits the chain at some k: `cost[i][j] = min over k of cost[i][k] + cost[k+1][j] + dims[i]·dims[k+1]·dims[j+1]`.
- **Order:** fill by increasing interval length, so both sub-intervals are already solved.
- Record the best `split[i][j]` to rebuild the parenthesization recursively.
- Tests replay the returned parenthesization to recompute its cost, and compare against an exponential brute force on random chains.

## Complexity
| | Time | Space |
|-|------|-------|
| DP | O(n³) | O(n²) |
| Reconstruction | O(n) | O(n) recursion |
| Brute force | O(Catalan(n−1)) ≈ O(4ⁿ / n^1.5) | O(n) |

## Interview talking points
- This is the archetype of **interval DP**, alongside optimal BST, burst balloons, palindrome partitioning and polygon triangulation. The pattern is to try every split point of an interval and combine the two halves.
- The number of parenthesizations is a Catalan number, so brute force is exponential; overlapping subproblems are what make DP win.
- Greedy rules (such as "multiply the pair with the smallest shared dimension first") fail on some inputs.
- Use `long` for costs: dimension products overflow `int` quickly.
- Hu–Shing finds the answer in O(n log n), but nobody expects that in an interview. Knuth's optimization doesn't apply here directly, because the cost function doesn't satisfy the quadrangle inequality in general.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
