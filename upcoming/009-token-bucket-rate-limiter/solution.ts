// Token bucket rate limiter. Tokens refill continuously at `refillPerSec`
// up to `capacity`; each request spends tokens or is rejected.
// The clock is injectable so behavior is deterministic in tests.

export type Clock = () => number; // milliseconds

export class TokenBucket {
  private tokens: number;
  private last: number;

  constructor(
    readonly capacity: number,
    readonly refillPerSec: number,
    private readonly now: Clock = Date.now,
  ) {
    if (!(capacity > 0)) throw new RangeError("capacity must be positive");
    if (!(refillPerSec >= 0)) throw new RangeError("refillPerSec must be >= 0");
    this.tokens = capacity; // start full so an idle client can burst
    this.last = now();
  }

  private refill(): void {
    const t = this.now();
    const elapsed = Math.max(0, t - this.last) / 1000; // ignore clocks going backward
    this.tokens = Math.min(this.capacity, this.tokens + elapsed * this.refillPerSec);
    this.last = t;
  }

  /** Spends `cost` tokens if available. Returns whether the request is allowed. */
  tryConsume(cost = 1): boolean {
    if (cost <= 0) throw new RangeError("cost must be positive");
    this.refill();
    if (this.tokens >= cost) {
      this.tokens -= cost;
      return true;
    }
    return false;
  }

  /** Milliseconds until `cost` tokens will be available (0 if now, Infinity if never). */
  waitTime(cost = 1): number {
    this.refill();
    if (cost > this.capacity) return Infinity;
    if (this.tokens >= cost) return 0;
    if (this.refillPerSec === 0) return Infinity;
    return Math.ceil(((cost - this.tokens) / this.refillPerSec) * 1000);
  }

  get available(): number {
    this.refill();
    return this.tokens;
  }
}

/** One bucket per key (e.g. user id or IP), created lazily. */
export class KeyedRateLimiter {
  private buckets = new Map<string, TokenBucket>();

  constructor(
    private readonly capacity: number,
    private readonly refillPerSec: number,
    private readonly now: Clock = Date.now,
  ) {}

  allow(key: string, cost = 1): boolean {
    let b = this.buckets.get(key);
    if (!b) this.buckets.set(key, (b = new TokenBucket(this.capacity, this.refillPerSec, this.now)));
    return b.tryConsume(cost);
  }
}
