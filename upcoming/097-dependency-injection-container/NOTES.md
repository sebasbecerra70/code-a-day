# Dependency injection container (typed tokens, scopes)

**Problem:** Build an IoC container that wires up a graph of services from factories, supports singleton / scoped / transient lifetimes, detects cycles, disposes resources, and is fully type-safe without decorators or reflection.

## Approach
- **Typed tokens:** `new Token<Db>("Db")` carries the resolved type as a phantom field, so `resolve(DB)` returns `Db` with no casts, and mismatched factories fail to compile.
- **Providers** map a token to `{ factory, lifetime }`. A factory receives the container and pulls its own dependencies, so the graph is built lazily on first resolve.
- **Lifetimes:**
  - *transient*: call the factory every time.
  - *singleton*: cache on the root, shared by every scope.
  - *scoped*: cache on the child scope (e.g. one per HTTP request). Resolving from the root is an error.
- **Captive dependencies:** singletons are built through a root view, so a singleton that depends on a scoped service fails loudly instead of keeping one request's instance forever.
- **Cycle detection:** keep the stack of tokens being resolved. Re-entering one throws with the chain (`A -> B -> C -> A`), and `try/finally` keeps the stack clean after failures.
- **Disposal:** cached instances with a `dispose()` method are disposed in reverse creation order, so dependents go before their dependencies.

## Complexity
| Aspect | Cost |
|--------|------|
| Resolve (cached) | O(1) |
| Resolve (cold) | O(size of dependency subgraph) |
| Cycle check | O(depth) per resolve |
| Space | O(providers + cached instances) |

## Interview talking points
- DI vs service locator: here factories call `resolve`, which is a locator internally. Constructor injection keeps classes container-agnostic (`new Db(cfg, logger)`).
- Why tokens instead of classes as keys? Interfaces vanish at runtime. Angular's `InjectionToken` and InversifyJS use the same idea.
- Decorator/reflect-metadata containers infer deps from constructor types but need compiler flags. Explicit factories are simpler and tree-shakable.
- Captive dependency is the classic lifetime bug (ASP.NET Core validates scopes for this reason).
- Extensions: async factories, multi-bindings, child containers with overrides for tests, auto-dispose with `Symbol.dispose` / `using`.

## Run
From this folder: `npx --yes tsx --test solution.test.ts`
