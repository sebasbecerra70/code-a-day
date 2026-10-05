// Structural deep equality for plain data: primitives, arrays, plain objects,
// Map, Set, Date, RegExp, typed arrays. Handles cyclic references.

export function deepEqual(a: unknown, b: unknown): boolean {
  return eq(a, b, new Map());
}

// `seen` maps each visited object in `a` to the set of objects in `b` it is
// currently assumed equal to. Revisiting a pair means a cycle: assume true.
function eq(a: unknown, b: unknown, seen: Map<object, Set<object>>): boolean {
  if (Object.is(a, b)) return true; // NaN === NaN, but +0 !== -0
  if (typeof a !== "object" || typeof b !== "object" || a === null || b === null) return false;
  if (Object.getPrototypeOf(a) !== Object.getPrototypeOf(b)) return false;

  let pairs = seen.get(a);
  if (pairs?.has(b)) return true;
  if (!pairs) seen.set(a, (pairs = new Set()));
  pairs.add(b);

  if (a instanceof Date) return Object.is(a.getTime(), (b as Date).getTime());
  if (a instanceof RegExp) return String(a) === String(b);

  if (ArrayBuffer.isView(a)) {
    const x = a as unknown as ArrayLike<unknown>;
    const y = b as unknown as ArrayLike<unknown>;
    if (x.length !== y.length) return false;
    for (let i = 0; i < x.length; i++) if (!Object.is(x[i], y[i])) return false;
    return true;
  }

  if (Array.isArray(a)) {
    const y = b as unknown[];
    if (a.length !== y.length) return false;
    for (let i = 0; i < a.length; i++) if (!eq(a[i], y[i], seen)) return false;
    return true;
  }

  if (a instanceof Map) {
    const y = b as Map<unknown, unknown>;
    if (a.size !== y.size) return false;
    for (const [k, v] of a) {
      // Keys compare by identity/SameValueZero, matching Map semantics.
      if (!y.has(k) || !eq(v, y.get(k), seen)) return false;
    }
    return true;
  }

  if (a instanceof Set) {
    const y = b as Set<unknown>;
    if (a.size !== y.size) return false;
    // Primitives match directly; objects need an O(n^2) deep search.
    const unmatched = [...y].filter((v) => typeof v === "object" && v !== null);
    for (const v of a) {
      if (y.has(v)) continue;
      if (typeof v !== "object" || v === null) return false;
      const i = unmatched.findIndex((w) => eq(v, w, seen));
      if (i === -1) return false;
      unmatched.splice(i, 1);
    }
    return true;
  }

  const ka = Object.keys(a as object);
  const kb = Object.keys(b as object);
  if (ka.length !== kb.length) return false;
  for (const k of ka) {
    if (!Object.prototype.hasOwnProperty.call(b, k)) return false;
    if (!eq((a as any)[k], (b as any)[k], seen)) return false;
  }
  return true;
}
