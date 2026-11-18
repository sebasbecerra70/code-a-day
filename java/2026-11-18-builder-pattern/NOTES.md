# Builder pattern (immutable HTTP request)

**Problem:** Construct an immutable object with one required field and many optional ones, without telescoping constructors, and guarantee that every instance satisfies cross-field rules (e.g. GET can't have a body, a retried POST needs an idempotency key).

## Approach
- `HttpRequest` has a **private** constructor that takes the `Builder`. All fields are `final`, and the headers map is defensively copied and wrapped as unmodifiable.
- The required `url` goes in the entry point `HttpRequest.builder(url)`. Optional fields have defaults and fluent setters that return `this`.
- Setters fail fast on nulls. **Cross-field rules are checked once in `build()`**, so the object can never exist in an invalid state.
- `toBuilder()` gives "copy with modifications" (a wither) without exposing mutators.
- Convenience setters encode domain knowledge: `jsonBody` sets the body and `Content-Type` together, and header names are normalized to lowercase.

## Complexity
| Operation | Time | Space |
|-----------|------|-------|
| Each setter | O(1) | — |
| build() | O(h) to copy h headers | O(h) |
| toBuilder() | O(h) | O(h) |

## Interview talking points
- Builder vs telescoping constructors vs JavaBeans setters: telescoping constructors become unreadable (`new Req(url, null, null, 5, 0)`), and setters leave the object mutable and half-built. A builder gives named parameters, defaults and atomic validation.
- Validate in `build()`, not in each setter: rules like "body only for POST/PUT/PATCH" depend on several fields, and the order of setter calls shouldn't matter.
- Defensive copying matters: if the object kept the builder's map, reusing the builder would mutate a "built" object.
- Variants: a **step builder** uses interfaces so the compiler enforces required fields in order; Lombok `@Builder` generates this boilerplate; and Java records with compact constructors cover simple cases.
- Real examples: `java.net.http.HttpRequest.newBuilder()`, `StringBuilder`, `Stream.builder()` and protobuf message builders.

## Run
From this folder: `javac *.java && java -ea SolutionTest`
