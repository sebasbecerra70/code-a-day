// Interval utilities over closed intervals [start, end].

export type Interval = [start: number, end: number];

/** Merge overlapping or touching intervals. Input may be unsorted. */
export function merge(intervals: readonly Interval[]): Interval[] {
  for (const [s, e] of intervals) if (s > e) throw new RangeError(`invalid interval [${s}, ${e}]`);
  const sorted = [...intervals].sort((a, b) => a[0] - b[0]);
  const out: Interval[] = [];
  for (const [s, e] of sorted) {
    const last = out[out.length - 1];
    if (last && s <= last[1]) last[1] = Math.max(last[1], e); // overlaps (or touches)
    else out.push([s, e]);
  }
  return out;
}

/** Insert into a sorted, non-overlapping list, merging as needed. O(n). */
export function insert(sorted: readonly Interval[], add: Interval): Interval[] {
  const out: Interval[] = [];
  let [s, e] = add;
  let i = 0;
  while (i < sorted.length && sorted[i][1] < s) out.push([...sorted[i++]]); // entirely before
  while (i < sorted.length && sorted[i][0] <= e) {
    // overlapping: absorb
    s = Math.min(s, sorted[i][0]);
    e = Math.max(e, sorted[i][1]);
    i++;
  }
  out.push([s, e]);
  while (i < sorted.length) out.push([...sorted[i++]]); // entirely after
  return out;
}

/** Intersection of two sorted, non-overlapping lists (two pointers). */
export function intersect(a: readonly Interval[], b: readonly Interval[]): Interval[] {
  const out: Interval[] = [];
  let i = 0, j = 0;
  while (i < a.length && j < b.length) {
    const lo = Math.max(a[i][0], b[j][0]);
    const hi = Math.min(a[i][1], b[j][1]);
    if (lo <= hi) out.push([lo, hi]);
    if (a[i][1] < b[j][1]) i++; // the one ending first can't meet anything else
    else j++;
  }
  return out;
}

/** Minimum number of intervals to remove so the rest don't overlap (touching is fine). */
export function minRemovalsToNonOverlap(intervals: readonly Interval[]): number {
  // Greedy: keep the interval that ends earliest, leaving the most room.
  const sorted = [...intervals].sort((a, b) => a[1] - b[1]);
  let removed = 0;
  let end = -Infinity;
  for (const [s, e] of sorted) {
    if (s >= end) end = e;
    else removed++;
  }
  return removed;
}
