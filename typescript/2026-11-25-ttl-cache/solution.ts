// A key-value cache where entries expire after a time-to-live, with an
// optional max size that evicts the least recently used entry.
// Expiry is lazy (checked on access) plus an explicit prune(); no timers.

export type Clock = () => number;

interface Entry<V> {
  value: V;
  expiresAt: number;
}

export class TTLCache<K, V> {
  // A Map iterates in insertion order; re-inserting on access makes the
  // first key the least recently used one.
  private map = new Map<K, Entry<V>>();

  constructor(
    private readonly defaultTtlMs: number,
    private readonly maxSize = Infinity,
    private readonly now: Clock = Date.now,
  ) {
    if (!(defaultTtlMs > 0)) throw new RangeError("ttl must be positive");
    if (!(maxSize >= 1)) throw new RangeError("maxSize must be >= 1");
  }

  set(key: K, value: V, ttlMs = this.defaultTtlMs): this {
    if (!(ttlMs > 0)) throw new RangeError("ttl must be positive");
    this.map.delete(key);
    this.map.set(key, { value, expiresAt: this.now() + ttlMs });
    if (this.map.size > this.maxSize) {
      // Prefer dropping expired entries before evicting live ones.
      this.prune();
      if (this.map.size > this.maxSize) this.map.delete(this.map.keys().next().value!);
    }
    return this;
  }

  get(key: K): V | undefined {
    const e = this.live(key);
    if (!e) return undefined;
    this.map.delete(key); // refresh recency
    this.map.set(key, e);
    return e.value;
  }

  /** Like get, but doesn't affect LRU order. */
  peek(key: K): V | undefined {
    return this.live(key)?.value;
  }

  has(key: K): boolean {
    return this.live(key) !== undefined;
  }

  delete(key: K): boolean {
    return this.map.delete(key);
  }

  /** Remaining time to live in ms, or undefined if missing/expired. */
  ttl(key: K): number | undefined {
    const e = this.live(key);
    return e && e.expiresAt - this.now();
  }

  /** Removes all expired entries. Returns how many were removed. */
  prune(): number {
    const t = this.now();
    let removed = 0;
    for (const [k, e] of this.map) {
      if (e.expiresAt <= t) {
        this.map.delete(k);
        removed++;
      }
    }
    return removed;
  }

  /** Count of stored entries, possibly including expired ones not yet pruned. */
  get size(): number {
    return this.map.size;
  }

  /** Returns cached value or computes, stores and returns it. */
  getOrSet(key: K, compute: () => V, ttlMs?: number): V {
    const e = this.live(key);
    if (e) return this.get(key)!;
    const v = compute();
    this.set(key, v, ttlMs);
    return v;
  }

  private live(key: K): Entry<V> | undefined {
    const e = this.map.get(key);
    if (!e) return undefined;
    if (e.expiresAt <= this.now()) {
      this.map.delete(key);
      return undefined;
    }
    return e;
  }
}
