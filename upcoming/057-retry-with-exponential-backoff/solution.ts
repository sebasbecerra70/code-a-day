// Retry an async operation with capped exponential backoff and "full jitter".
// Sleep and randomness are injectable so tests run instantly and deterministically.

export interface RetryOptions {
  retries?: number; // extra attempts after the first (default 3)
  baseMs?: number; // first backoff ceiling (default 100)
  maxMs?: number; // cap on any single delay (default 10_000)
  jitter?: boolean; // full jitter: random in [0, ceiling] (default true)
  shouldRetry?: (err: unknown, attempt: number) => boolean;
  onRetry?: (err: unknown, attempt: number, delayMs: number) => void;
  signal?: AbortSignal;
  sleep?: (ms: number, signal?: AbortSignal) => Promise<void>;
  random?: () => number;
}

export class RetryError extends Error {
  constructor(readonly attempts: number, readonly lastError: unknown) {
    super(`failed after ${attempts} attempts`, { cause: lastError });
  }
}

/** Delay before retry number `attempt` (1-based): min(maxMs, baseMs * 2^(attempt-1)), optionally jittered. */
export function backoffDelay(
  attempt: number,
  { baseMs = 100, maxMs = 10_000, jitter = true, random = Math.random }: RetryOptions = {},
): number {
  const ceiling = Math.min(maxMs, baseMs * 2 ** (attempt - 1));
  return jitter ? Math.floor(random() * (ceiling + 1)) : ceiling;
}

const defaultSleep = (ms: number, signal?: AbortSignal) =>
  new Promise<void>((resolve, reject) => {
    if (signal?.aborted) return reject(signal.reason);
    const t = setTimeout(() => {
      signal?.removeEventListener("abort", onAbort);
      resolve();
    }, ms);
    const onAbort = () => {
      clearTimeout(t);
      reject(signal!.reason);
    };
    signal?.addEventListener("abort", onAbort, { once: true });
  });

export async function retry<T>(fn: (attempt: number) => Promise<T>, opts: RetryOptions = {}): Promise<T> {
  const { retries = 3, shouldRetry = () => true, onRetry, signal, sleep = defaultSleep } = opts;
  if (!Number.isInteger(retries) || retries < 0) throw new RangeError("retries must be a non-negative integer");
  for (let attempt = 1; ; attempt++) {
    signal?.throwIfAborted();
    try {
      return await fn(attempt);
    } catch (err) {
      if (signal?.aborted) throw signal.reason;
      if (attempt > retries) throw new RetryError(attempt, err);
      if (!shouldRetry(err, attempt)) throw err; // non-retryable: surface the original error
      const delay = backoffDelay(attempt, opts);
      onRetry?.(err, attempt, delay);
      await sleep(delay, signal);
    }
  }
}
