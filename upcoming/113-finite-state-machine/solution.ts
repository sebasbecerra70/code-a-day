// A small typed finite state machine. The transition table is plain data, the
// compiler checks state and event names, and the runtime supports guards,
// transition actions, entry/exit hooks, and subscribers.

export interface Transition<S extends string, C> {
  target: S;
  /** Transition is taken only if the guard returns true (first passing one wins). */
  guard?: (ctx: C, payload?: unknown) => boolean;
  /** Runs during the transition and may return an updated context. */
  action?: (ctx: C, payload?: unknown) => C | void;
}

export interface StateConfig<S extends string, E extends string, C> {
  on?: Partial<Record<E, S | Transition<S, C> | Transition<S, C>[]>>;
  onEnter?: (ctx: C) => void;
  onExit?: (ctx: C) => void;
  final?: boolean;
}

export interface MachineConfig<S extends string, E extends string, C> {
  initial: S;
  context: C;
  states: Record<S, StateConfig<S, E, C>>;
}

export type Listener<S, E, C> = (state: S, event: E, ctx: C) => void;

export class StateMachine<S extends string, E extends string, C = undefined> {
  private current: S;
  private ctx: C;
  private listeners = new Set<Listener<S, E, C>>();
  private busy = false;

  constructor(private readonly config: MachineConfig<S, E, C>) {
    for (const [name, st] of Object.entries(config.states) as [S, StateConfig<S, E, C>][]) {
      for (const t of Object.values(st.on ?? {}) as (S | Transition<S, C> | Transition<S, C>[])[]) {
        for (const target of toList(t).map((x) => x.target)) {
          if (!(target in config.states)) throw new Error(`state "${name}" targets unknown state "${target}"`);
        }
      }
    }
    if (!(config.initial in config.states)) throw new Error(`unknown initial state "${config.initial}"`);
    this.current = config.initial;
    this.ctx = config.context;
    config.states[this.current].onEnter?.(this.ctx);
  }

  get state(): S {
    return this.current;
  }

  get context(): C {
    return this.ctx;
  }

  get done(): boolean {
    return !!this.config.states[this.current].final;
  }

  /** True if `event` would cause a transition right now. */
  can(event: E, payload?: unknown): boolean {
    return this.pick(event, payload) !== undefined;
  }

  /** Sends an event. Returns true if a transition happened; unknown/blocked events are ignored. */
  send(event: E, payload?: unknown): boolean {
    if (this.busy) throw new Error("send() called re-entrantly from a hook or action");
    if (this.done) return false;
    const t = this.pick(event, payload);
    if (!t) return false;
    this.busy = true;
    try {
      const from = this.config.states[this.current];
      from.onExit?.(this.ctx);
      const next = t.action?.(this.ctx, payload);
      if (next !== undefined) this.ctx = next;
      this.current = t.target;
      this.config.states[this.current].onEnter?.(this.ctx);
    } finally {
      this.busy = false;
    }
    for (const l of this.listeners) l(this.current, event, this.ctx);
    return true;
  }

  subscribe(listener: Listener<S, E, C>): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  /** Events that have at least one transition defined from the current state (guards not evaluated). */
  availableEvents(): E[] {
    return Object.keys(this.config.states[this.current].on ?? {}) as E[];
  }

  private pick(event: E, payload: unknown): Transition<S, C> | undefined {
    const spec = this.config.states[this.current].on?.[event];
    if (spec === undefined) return undefined;
    return toList(spec).find((t) => !t.guard || t.guard(this.ctx, payload));
  }
}

function toList<S extends string, C>(t: S | Transition<S, C> | Transition<S, C>[]): Transition<S, C>[] {
  if (typeof t === "string") return [{ target: t }];
  return Array.isArray(t) ? t : [t];
}

/** Reachable states via BFS over the transition graph (ignoring guards); handy for spotting dead states. */
export function reachableStates<S extends string, E extends string, C>(config: MachineConfig<S, E, C>): Set<S> {
  const seen = new Set<S>([config.initial]);
  const queue: S[] = [config.initial];
  while (queue.length) {
    const s = queue.shift()!;
    for (const t of Object.values(config.states[s].on ?? {}) as (S | Transition<S, C> | Transition<S, C>[])[]) {
      for (const { target } of toList(t)) {
        if (!seen.has(target)) {
          seen.add(target);
          queue.push(target);
        }
      }
    }
  }
  return seen;
}
