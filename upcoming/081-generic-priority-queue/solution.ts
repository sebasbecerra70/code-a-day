// A generic binary-heap priority queue with a custom comparator, plus
// handle-free updates: each value's heap position is tracked in a Map,
// so you can change a priority or remove an arbitrary value in O(log n).

export type Comparator<T> = (a: T, b: T) => number;

export class PriorityQueue<T> {
  private heap: T[] = [];
  private pos = new Map<T, number>(); // value -> index in heap

  /** `compare(a, b) < 0` means a comes out first. Values must be unique (by identity). */
  constructor(private readonly compare: Comparator<T>, items: Iterable<T> = []) {
    for (const x of items) {
      if (this.pos.has(x)) throw new Error("duplicate value");
      this.pos.set(x, this.heap.length);
      this.heap.push(x);
    }
    for (let i = (this.heap.length >> 1) - 1; i >= 0; i--) this.down(i);
  }

  get size(): number {
    return this.heap.length;
  }

  has(x: T): boolean {
    return this.pos.has(x);
  }

  peek(): T | undefined {
    return this.heap[0];
  }

  push(x: T): void {
    if (this.pos.has(x)) throw new Error("duplicate value; use update()");
    this.heap.push(x);
    this.pos.set(x, this.heap.length - 1);
    this.up(this.heap.length - 1);
  }

  pop(): T | undefined {
    if (this.heap.length === 0) return undefined;
    const top = this.heap[0];
    this.removeAt(0);
    return top;
  }

  /** Re-positions `x` after its priority (as seen by the comparator) changed. */
  update(x: T): void {
    const i = this.pos.get(x);
    if (i === undefined) throw new Error("value not in queue");
    this.down(this.up(i));
  }

  remove(x: T): boolean {
    const i = this.pos.get(x);
    if (i === undefined) return false;
    this.removeAt(i);
    return true;
  }

  /** Drains in priority order (destructive). */
  *drain(): Generator<T> {
    while (this.heap.length) yield this.pop()!;
  }

  private removeAt(i: number): void {
    const last = this.heap.length - 1;
    this.swap(i, last);
    this.pos.delete(this.heap.pop()!);
    if (i < this.heap.length) this.down(this.up(i)); // moved element may go either way
  }

  private up(i: number): number {
    while (i > 0) {
      const p = (i - 1) >> 1;
      if (this.compare(this.heap[i], this.heap[p]) >= 0) break;
      this.swap(i, p);
      i = p;
    }
    return i;
  }

  private down(i: number): number {
    const n = this.heap.length;
    for (;;) {
      const l = 2 * i + 1, r = l + 1;
      let m = i;
      if (l < n && this.compare(this.heap[l], this.heap[m]) < 0) m = l;
      if (r < n && this.compare(this.heap[r], this.heap[m]) < 0) m = r;
      if (m === i) return i;
      this.swap(i, m);
      i = m;
    }
  }

  private swap(i: number, j: number): void {
    const h = this.heap;
    [h[i], h[j]] = [h[j], h[i]];
    this.pos.set(h[i], i);
    this.pos.set(h[j], j);
  }
}
