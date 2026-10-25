// Run async tasks with at most `limit` in flight at once.

/**
 * Maps `items` through async `fn`, never running more than `limit` calls at a
 * time. Results keep input order. Rejects on the first failure (like
 * Promise.all) and stops starting new tasks.
 */
export async function mapWithLimit<T, R>(
  items: readonly T[],
  limit: number,
  fn: (item: T, index: number) => Promise<R>,
): Promise<R[]> {
  if (!Number.isInteger(limit) || limit < 1) throw new RangeError("limit must be a positive integer");
  const results = new Array<R>(items.length);
  let next = 0;
  let failed = false;

  // Each worker pulls the next index until the queue is drained.
  // JS is single-threaded, so `next++` needs no lock.
  const worker = async () => {
    while (!failed && next < items.length) {
      const i = next++;
      try {
        results[i] = await fn(items[i], i);
      } catch (err) {
        failed = true;
        throw err;
      }
    }
  };

  const workers = Array.from({ length: Math.min(limit, items.length) }, worker);
  await Promise.all(workers);
  return results;
}

/** Like mapWithLimit but never rejects; reports each outcome. */
export async function settleWithLimit<T, R>(
  items: readonly T[],
  limit: number,
  fn: (item: T, index: number) => Promise<R>,
): Promise<PromiseSettledResult<R>[]> {
  return mapWithLimit(items, limit, (item, i) =>
    fn(item, i).then(
      (value): PromiseSettledResult<R> => ({ status: "fulfilled", value }),
      (reason): PromiseSettledResult<R> => ({ status: "rejected", reason }),
    ),
  );
}

/** A reusable limiter: wrap any async call so at most `limit` run concurrently. */
export function createLimiter(limit: number) {
  if (!Number.isInteger(limit) || limit < 1) throw new RangeError("limit must be a positive integer");
  let active = 0;
  const queue: (() => void)[] = [];

  const release = () => {
    active--;
    queue.shift()?.();
  };

  return async function run<R>(task: () => Promise<R>): Promise<R> {
    if (active >= limit) await new Promise<void>((resolve) => queue.push(resolve));
    active++;
    try {
      return await task();
    } finally {
      release();
    }
  };
}
