// Sliding window maximum/minimum in O(n) with a monotonic deque of indices.
// The deque keeps candidates in decreasing value order; anything smaller than
// a newer element can never be a window max again, so it is dropped for good.

/** Max of every length-k window. Returns n - k + 1 values. */
export function slidingWindowMax(nums: readonly number[], k: number): number[] {
  return slidingWindow(nums, k, (a, b) => a >= b);
}

export function slidingWindowMin(nums: readonly number[], k: number): number[] {
  return slidingWindow(nums, k, (a, b) => a <= b);
}

/** `dominates(a, b)`: true when a newer `a` makes an older `b` useless. */
function slidingWindow(nums: readonly number[], k: number, dominates: (a: number, b: number) => boolean): number[] {
  if (!Number.isInteger(k) || k < 1) throw new RangeError("k must be a positive integer");
  if (k > nums.length) return [];
  // Array-backed deque with a moving head index: O(1) pops from both ends.
  const dq = new Array<number>(nums.length);
  let head = 0, tail = 0;
  const out: number[] = [];
  for (let i = 0; i < nums.length; i++) {
    if (head < tail && dq[head] <= i - k) head++; // front index slid out of the window
    while (head < tail && dominates(nums[i], nums[dq[tail - 1]])) tail--;
    dq[tail++] = i;
    if (i >= k - 1) out.push(nums[dq[head]]);
  }
  return out;
}

/**
 * Streaming variant: push values one at a time and query the max of the last k.
 * Useful for metrics like "max latency over the last 60 samples".
 */
export class MaxWindow {
  private dq: { i: number; v: number }[] = [];
  private head = 0;
  private count = 0;

  constructor(private readonly k: number) {
    if (!Number.isInteger(k) || k < 1) throw new RangeError("k must be a positive integer");
  }

  push(v: number): void {
    const i = this.count++;
    while (this.dq.length > this.head && this.dq[this.dq.length - 1].v <= v) this.dq.pop();
    this.dq.push({ i, v });
    while (this.dq[this.head].i <= i - this.k) this.head++;
    // Compact occasionally so the backing array doesn't grow forever.
    if (this.head > 1024 && this.head * 2 > this.dq.length) {
      this.dq = this.dq.slice(this.head);
      this.head = 0;
    }
  }

  /** Max of the most recent min(k, pushed) values, or undefined if empty. */
  max(): number | undefined {
    return this.count === 0 ? undefined : this.dq[this.head].v;
  }
}

/** Longest subarray where max - min <= limit, via two deques (a common follow-up). */
export function longestWithinLimit(nums: readonly number[], limit: number): number {
  const maxQ: number[] = [], minQ: number[] = [];
  let maxHead = 0, minHead = 0, left = 0, best = 0;
  for (let right = 0; right < nums.length; right++) {
    while (maxQ.length > maxHead && nums[maxQ.at(-1)!] <= nums[right]) maxQ.pop();
    while (minQ.length > minHead && nums[minQ.at(-1)!] >= nums[right]) minQ.pop();
    maxQ.push(right);
    minQ.push(right);
    while (nums[maxQ[maxHead]] - nums[minQ[minHead]] > limit) {
      left++;
      if (maxQ[maxHead] < left) maxHead++;
      if (minQ[minHead] < left) minHead++;
    }
    best = Math.max(best, right - left + 1);
  }
  return best;
}
