// An event emitter whose event names and payload types are checked by the
// compiler: Events maps each event name to its listener's argument tuple.

type EventMap = Record<string, unknown[]>;
type Listener<Args extends unknown[]> = (...args: Args) => void;

export class TypedEmitter<Events extends EventMap> {
  private listeners = new Map<keyof Events, Listener<any>[]>();

  /** Subscribe. Returns an unsubscribe function. */
  on<K extends keyof Events>(event: K, listener: Listener<Events[K]>): () => void {
    const list = this.listeners.get(event) ?? [];
    list.push(listener);
    this.listeners.set(event, list);
    return () => this.off(event, listener);
  }

  /** Subscribe for a single emission. */
  once<K extends keyof Events>(event: K, listener: Listener<Events[K]>): () => void {
    const wrapper: Listener<Events[K]> = (...args) => {
      off();
      listener(...args);
    };
    const off = this.on(event, wrapper);
    return off;
  }

  /** Removes one registration of `listener` (the most recent, like Node's emitter). */
  off<K extends keyof Events>(event: K, listener: Listener<Events[K]>): void {
    const list = this.listeners.get(event);
    if (!list) return;
    const i = list.lastIndexOf(listener);
    if (i !== -1) list.splice(i, 1);
    if (list.length === 0) this.listeners.delete(event);
  }

  /**
   * Calls listeners synchronously in registration order. Iterates over a
   * snapshot so listeners can safely subscribe/unsubscribe during emit.
   * Returns whether anyone was listening.
   */
  emit<K extends keyof Events>(event: K, ...args: Events[K]): boolean {
    const list = this.listeners.get(event);
    if (!list || list.length === 0) return false;
    for (const fn of [...list]) fn(...args);
    return true;
  }

  listenerCount<K extends keyof Events>(event: K): number {
    return this.listeners.get(event)?.length ?? 0;
  }

  removeAllListeners<K extends keyof Events>(event?: K): void {
    if (event === undefined) this.listeners.clear();
    else this.listeners.delete(event);
  }

  /** Resolves with the args of the next emission of `event`. */
  waitFor<K extends keyof Events>(event: K): Promise<Events[K]> {
    return new Promise((resolve) => {
      this.once(event, (...args) => resolve(args));
    });
  }
}
