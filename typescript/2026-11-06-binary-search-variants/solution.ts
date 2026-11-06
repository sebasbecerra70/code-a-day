// Binary search variants, all written with the half-open [lo, hi) invariant.

/** First index i with a[i] >= x (a.length if none). */
export function lowerBound(a: readonly number[], x: number): number {
  let lo = 0, hi = a.length;
  while (lo < hi) {
    const mid = (lo + hi) >>> 1;
    if (a[mid] < x) lo = mid + 1;
    else hi = mid;
  }
  return lo;
}

/** First index i with a[i] > x (a.length if none). */
export function upperBound(a: readonly number[], x: number): number {
  let lo = 0, hi = a.length;
  while (lo < hi) {
    const mid = (lo + hi) >>> 1;
    if (a[mid] <= x) lo = mid + 1;
    else hi = mid;
  }
  return lo;
}

/** Index of x in sorted a, or -1. */
export function indexOf(a: readonly number[], x: number): number {
  const i = lowerBound(a, x);
  return i < a.length && a[i] === x ? i : -1;
}

/** [first, last] indices of x, or [-1, -1]. */
export function searchRange(a: readonly number[], x: number): [number, number] {
  const lo = lowerBound(a, x);
  if (lo === a.length || a[lo] !== x) return [-1, -1];
  return [lo, upperBound(a, x) - 1];
}

/** Search in a sorted array of distinct values that was rotated at an unknown pivot. */
export function searchRotated(a: readonly number[], x: number): number {
  let lo = 0, hi = a.length - 1;
  while (lo <= hi) {
    const mid = (lo + hi) >>> 1;
    if (a[mid] === x) return mid;
    if (a[lo] <= a[mid]) {
      // left half [lo, mid] is sorted
      if (a[lo] <= x && x < a[mid]) hi = mid - 1;
      else lo = mid + 1;
    } else {
      // right half [mid, hi] is sorted
      if (a[mid] < x && x <= a[hi]) lo = mid + 1;
      else hi = mid - 1;
    }
  }
  return -1;
}

/** Index of the minimum in a rotated sorted array of distinct values. */
export function findRotationPoint(a: readonly number[]): number {
  if (a.length === 0) return -1;
  let lo = 0, hi = a.length - 1;
  while (lo < hi) {
    const mid = (lo + hi) >>> 1;
    if (a[mid] > a[hi]) lo = mid + 1;
    else hi = mid;
  }
  return lo;
}

/**
 * "Binary search on the answer": smallest integer in [lo, hi] where the
 * monotone predicate becomes true, or hi + 1 if it never does.
 */
export function firstTrue(lo: number, hi: number, pred: (n: number) => boolean): number {
  let l = lo, h = hi + 1;
  while (l < h) {
    const mid = l + Math.floor((h - l) / 2);
    if (pred(mid)) h = mid;
    else l = mid + 1;
  }
  return l;
}

/** Integer square root via firstTrue. */
export function isqrt(n: number): number {
  if (n < 0) throw new RangeError("negative");
  return firstTrue(0, n, (m) => m * m > n) - 1;
}
