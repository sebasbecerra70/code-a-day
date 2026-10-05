// A minimal Redux/Zustand-style store: immutable state, reducer-driven
// updates, subscriptions, and selector subscriptions that only fire on change.

export type Reducer<S, A> = (state: S, action: A) => S;
export type Listener<S> = (state: S, prev: S) => void;
export type Middleware<S, A> = (store: { getState(): S }, next: (action: A) => void) => (action: A) => void;

export class Store<S, A> {
  private state: S;
  private listeners = new Set<Listener<S>>();
  private dispatching = false;
  private readonly dispatchChain: (action: A) => void;

  constructor(
    private readonly reducer: Reducer<S, A>,
    initial: S,
    middleware: Middleware<S, A>[] = [],
  ) {
    this.state = initial;
    // Compose middleware right-to-left so the first one listed runs first.
    let chain = (action: A) => this.applyAction(action);
    for (const mw of [...middleware].reverse()) chain = mw(this, chain);
    this.dispatchChain = chain;
  }

  getState(): S {
    return this.state;
  }

  dispatch(action: A): void {
    this.dispatchChain(action);
  }

  private applyAction(action: A): void {
    if (this.dispatching) throw new Error("reducers may not dispatch");
    const prev = this.state;
    this.dispatching = true;
    try {
      this.state = this.reducer(prev, action);
    } finally {
      this.dispatching = false;
    }
    if (Object.is(prev, this.state)) return; // nothing changed: skip notifications
    for (const l of [...this.listeners]) l(this.state, prev);
  }

  subscribe(listener: Listener<S>): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  /**
   * Subscribe to a derived value. The callback runs only when the selected
   * value changes according to `equals` (Object.is by default).
   */
  select<T>(
    selector: (state: S) => T,
    onChange: (value: T, prev: T) => void,
    equals: (a: T, b: T) => boolean = Object.is,
  ): () => void {
    let current = selector(this.state);
    return this.subscribe((state) => {
      const next = selector(state);
      if (!equals(next, current)) {
        const prev = current;
        current = next;
        onChange(next, prev);
      }
    });
  }
}

/** Shallow equality for selectors that return fresh objects/arrays. */
export function shallowEqual(a: unknown, b: unknown): boolean {
  if (Object.is(a, b)) return true;
  if (typeof a !== "object" || typeof b !== "object" || !a || !b) return false;
  const ka = Object.keys(a);
  if (ka.length !== Object.keys(b).length) return false;
  return ka.every((k) => Object.prototype.hasOwnProperty.call(b, k) && Object.is((a as any)[k], (b as any)[k]));
}
