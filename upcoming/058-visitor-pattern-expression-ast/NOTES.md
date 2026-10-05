# Visitor pattern (expression AST: eval, print, derivative)

**Problem:** Represent arithmetic expressions as a tree and support many independent operations on them (evaluation, pretty-printing, symbolic differentiation, simplification, collecting variables) without stuffing every operation into every node class.

## Approach
- **Nodes:** a `sealed interface Expr` with record implementations `Num`, `Var`, `Add`, `Mul`, `Neg` and `Pow`. Each node implements `accept(visitor)` by calling the visitor's method for its own type (**double dispatch**).
- **Visitors:** `ExprVisitor<R>` has one `visitX` per node type and is generic in its result:
  - `Evaluator` → `Double`, using an environment of variable bindings.
  - `Printer` → `String`, with minimal parentheses based on precedence.
  - `Derivative` → `Expr`, using the sum, product and power/chain rules.
  - `Simplifier` → `Expr`, bottom-up: constant folding, `x+0`, `x*1`, `x*0`, `--x`, `x^0`, `x^1`.
  - `VariableCollector` → `Set<String>`.
- Tests cross-check the symbolic derivative against a central finite difference on random expressions, and check that simplification never changes an expression's value.

## Complexity
| Visitor | Time | Space |
|---------|------|-------|
| Evaluate / print / collect | O(n) nodes | O(depth) recursion |
| Derivative | O(n) nodes, output O(n) before simplification | O(n) |
| Simplify | O(n) | O(depth) |

## Interview talking points
- **The expression problem:** visitors make adding *operations* easy (one new class) but adding *node types* hard (every visitor changes). Plain OO polymorphism has the opposite trade-off. Choose based on which axis changes more often; compilers add passes far more often than node kinds.
- Double dispatch: Java dispatches virtual calls on the receiver only. `accept` → `visitX` adds a second dispatch on the node type.
- **Java 21 alternative:** sealed interfaces plus pattern-matching `switch` (used in `Printer.prec`) give exhaustive, compiler-checked matching without the `accept` boilerplate. That's often the modern choice.
- Real uses: javac's `TreeVisitor`, ANTLR parse-tree visitors, the ASM bytecode library and linters.
- The derivative is unsimplified on purpose, since composing visitors (`derive` then `simplify`) keeps each one small.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
