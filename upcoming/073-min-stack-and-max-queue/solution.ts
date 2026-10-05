// Containers that report their minimum / maximum in O(1).

/** Stack with O(1) push, pop, top, and min. */
export class MinStack {
  private items: number[] = [];
  private mins: number[] = []; // mins[i] = min of items[0..i]

  push(x: number): void {
    this.items.push(x);
    this.mins.push(this.mins.length ? Math.min(x, this.mins[this.mins.length - 1]) : x);
  }

  pop(): number {
    if (!this.items.length) throw new RangeError("pop from empty stack");
    this.mins.pop();
    return this.items.pop()!;
  }

  top(): number {
    if (!this.items.length) throw new RangeError("empty stack");
    return this.items[this.items.length - 1];
  }

  min(): number {
    if (!this.mins.length) throw new RangeError("empty stack");
    return this.mins[this.mins.length - 1];
  }

  get size(): number {
    return this.items.length;
  }
}

/**
 * FIFO queue with O(1) amortized max, using a monotonic deque: `maxes` holds
 * candidates in non-increasing order; anything smaller than a newer element
 * can never be the max again, so it's dropped.
 */
export class MaxQueue {
  private items: number[] = [];
  private head = 0; // index of the front in `items` (avoids O(n) shift)
  private maxes: number[] = [];
  private maxHead = 0;

  enqueue(x: number): void {
    this.items.push(x);
    while (this.maxes.length > this.maxHead && this.maxes[this.maxes.length - 1] < x) this.maxes.pop();
    this.maxes.push(x);
  }

  dequeue(): number {
    if (this.size === 0) throw new RangeError("dequeue from empty queue");
    const x = this.items[this.head++];
    if (x === this.maxes[this.maxHead]) this.maxHead++;
    this.compact();
    return x;
  }

  front(): number {
    if (this.size === 0) throw new RangeError("empty queue");
    return this.items[this.head];
  }

  max(): number {
    if (this.size === 0) throw new RangeError("empty queue");
    return this.maxes[this.maxHead];
  }

  get size(): number {
    return this.items.length - this.head;
  }

  // Reclaim consumed prefix once it dominates, keeping memory O(size).
  private compact(): void {
    if (this.head > 32 && this.head * 2 > this.items.length) {
      this.items = this.items.slice(this.head);
      this.head = 0;
    }
    if (this.maxHead > 32 && this.maxHead * 2 > this.maxes.length) {
      this.maxes = this.maxes.slice(this.maxHead);
      this.maxHead = 0;
    }
  }
}

/** Alternative max queue from two max-tracking stacks (no deque needed). */
export class TwoStackMaxQueue {
  private inbox: [value: number, max: number][] = [];
  private outbox: [value: number, max: number][] = [];

  enqueue(x: number): void {
    const m = this.inbox.length ? Math.max(x, this.inbox[this.inbox.length - 1][1]) : x;
    this.inbox.push([x, m]);
  }

  dequeue(): number {
    if (!this.outbox.length) {
      // Move everything over once; each element is moved at most once => amortized O(1).
      while (this.inbox.length) {
        const [x] = this.inbox.pop()!;
        const m = this.outbox.length ? Math.max(x, this.outbox[this.outbox.length - 1][1]) : x;
        this.outbox.push([x, m]);
      }
    }
    if (!this.outbox.length) throw new RangeError("dequeue from empty queue");
    return this.outbox.pop()![0];
  }

  max(): number {
    const a = this.inbox.length ? this.inbox[this.inbox.length - 1][1] : -Infinity;
    const b = this.outbox.length ? this.outbox[this.outbox.length - 1][1] : -Infinity;
    if (!this.inbox.length && !this.outbox.length) throw new RangeError("empty queue");
    return Math.max(a, b);
  }

  get size(): number {
    return this.inbox.length + this.outbox.length;
  }
}
