// K-way merge: combine k sorted sequences into one sorted stream using a
// min-heap that holds the current head of each source. Works lazily over
// any iterables, so it also merges generators or huge files line by line.

export type Comparator<T> = (a: T, b: T) => number;

interface Head<T> {
  value: T;
  source: number; // index of the source it came from (tie-breaker => stable)
  it: Iterator<T>;
}

class MinHeap<T> {
  private a: Head<T>[] = [];
  constructor(private readonly cmp: Comparator<T>) {}

  get size(): number {
    return this.a.length;
  }

  private less(i: number, j: number): boolean {
    const c = this.cmp(this.a[i].value, this.a[j].value);
    return c < 0 || (c === 0 && this.a[i].source < this.a[j].source);
  }

  private swap(i: number, j: number): void {
    [this.a[i], this.a[j]] = [this.a[j], this.a[i]];
  }

  push(h: Head<T>): void {
    this.a.push(h);
    let i = this.a.length - 1;
    while (i > 0) {
      const p = (i - 1) >> 1;
      if (!this.less(i, p)) break;
      this.swap(i, p);
      i = p;
    }
  }

  /** Replaces the top with `h` (or removes it if `h` is undefined) and sifts down. */
  replaceTop(h: Head<T> | undefined): void {
    const last = this.a.pop()!;
    if (h) {
      if (this.a.length === 0) this.a.push(h);
      else {
        this.a.push(last);
        this.a[0] = h;
      }
    } else if (this.a.length > 0) this.a[0] = last;
    else return;
    let i = 0;
    for (;;) {
      const l = 2 * i + 1, r = l + 1;
      let m = i;
      if (l < this.a.length && this.less(l, m)) m = l;
      if (r < this.a.length && this.less(r, m)) m = r;
      if (m === i) return;
      this.swap(i, m);
      i = m;
    }
  }

  top(): Head<T> {
    return this.a[0];
  }
}

const defaultCompare = <T>(a: T, b: T): number => (a < b ? -1 : a > b ? 1 : 0);

/**
 * Lazily merges sorted iterables. Stable: equal elements come out in source order.
 * O(N log k) time for N total elements, O(k) extra space.
 */
export function* mergeK<T>(sources: Iterable<T>[], compare: Comparator<T> = defaultCompare): Generator<T> {
  const heap = new MinHeap<T>(compare);
  sources.forEach((src, source) => {
    const it = src[Symbol.iterator]();
    const r = it.next();
    if (!r.done) heap.push({ value: r.value, source, it });
  });
  while (heap.size > 0) {
    const top = heap.top();
    yield top.value;
    const r = top.it.next();
    heap.replaceTop(r.done ? undefined : { value: r.value, source: top.source, it: top.it });
  }
}

/** Eager convenience wrapper. */
export function mergeKArrays<T>(arrays: T[][], compare?: Comparator<T>): T[] {
  return [...mergeK(arrays, compare)];
}

/** Classic interview variant: smallest range [lo, hi] containing at least one element from each list. */
export function smallestRange(lists: number[][]): [number, number] {
  if (lists.length === 0 || lists.some((l) => l.length === 0)) throw new Error("every list must be non-empty");
  // Heap entries: [value, listIndex, elementIndex]; track current max separately.
  const heap: [number, number, number][] = [];
  const less = (i: number, j: number) => heap[i][0] < heap[j][0];
  const up = (i: number) => {
    while (i > 0) {
      const p = (i - 1) >> 1;
      if (!less(i, p)) break;
      [heap[i], heap[p]] = [heap[p], heap[i]];
      i = p;
    }
  };
  const down = (i: number) => {
    for (;;) {
      const l = 2 * i + 1, r = l + 1;
      let m = i;
      if (l < heap.length && less(l, m)) m = l;
      if (r < heap.length && less(r, m)) m = r;
      if (m === i) return;
      [heap[i], heap[m]] = [heap[m], heap[i]];
      i = m;
    }
  };
  let max = -Infinity;
  lists.forEach((l, i) => {
    heap.push([l[0], i, 0]);
    up(heap.length - 1);
    max = Math.max(max, l[0]);
  });
  let best: [number, number] = [heap[0][0], max];
  for (;;) {
    const [v, li, ei] = heap[0];
    if (max - v < best[1] - best[0]) best = [v, max];
    if (ei + 1 === lists[li].length) return best; // one list exhausted: no range can cover it anymore
    const next = lists[li][ei + 1];
    heap[0] = [next, li, ei + 1];
    down(0);
    max = Math.max(max, next);
  }
}
