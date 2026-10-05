// Fixed-capacity ring buffer. Writes either reject when full or overwrite
// the oldest element, depending on `overwrite`.

export class CircularBuffer<T> implements Iterable<T> {
  private buf: (T | undefined)[];
  private head = 0; // index of the oldest element
  private _size = 0;

  constructor(readonly capacity: number, private readonly overwrite = false) {
    if (!Number.isInteger(capacity) || capacity < 1) throw new RangeError("capacity must be a positive integer");
    this.buf = new Array(capacity);
  }

  get size(): number {
    return this._size;
  }

  get isFull(): boolean {
    return this._size === this.capacity;
  }

  get isEmpty(): boolean {
    return this._size === 0;
  }

  /** Appends at the back. Returns the evicted element when overwriting a full buffer. */
  push(x: T): T | undefined {
    if (this.isFull) {
      if (!this.overwrite) throw new RangeError("buffer is full");
      const evicted = this.buf[this.head];
      this.buf[this.head] = x;
      this.head = (this.head + 1) % this.capacity;
      return evicted;
    }
    this.buf[(this.head + this._size) % this.capacity] = x;
    this._size++;
    return undefined;
  }

  /** Removes and returns the oldest element. */
  shift(): T {
    if (this.isEmpty) throw new RangeError("buffer is empty");
    const x = this.buf[this.head] as T;
    this.buf[this.head] = undefined; // let GC reclaim it
    this.head = (this.head + 1) % this.capacity;
    this._size--;
    return x;
  }

  /** Removes and returns the newest element. */
  pop(): T {
    if (this.isEmpty) throw new RangeError("buffer is empty");
    const i = (this.head + this._size - 1) % this.capacity;
    const x = this.buf[i] as T;
    this.buf[i] = undefined;
    this._size--;
    return x;
  }

  /** i-th element from the oldest (negative i counts from the newest). */
  at(i: number): T | undefined {
    if (i < 0) i += this._size;
    if (i < 0 || i >= this._size) return undefined;
    return this.buf[(this.head + i) % this.capacity];
  }

  peekFront(): T | undefined {
    return this.at(0);
  }

  peekBack(): T | undefined {
    return this.at(-1);
  }

  clear(): void {
    this.buf = new Array(this.capacity);
    this.head = 0;
    this._size = 0;
  }

  *[Symbol.iterator](): Iterator<T> {
    for (let i = 0; i < this._size; i++) yield this.buf[(this.head + i) % this.capacity] as T;
  }

  toArray(): T[] {
    return [...this];
  }
}

/** Example use: moving average over the last `window` samples in O(1) per sample. */
export class MovingAverage {
  private buf: CircularBuffer<number>;
  private sum = 0;

  constructor(window: number) {
    this.buf = new CircularBuffer(window, true);
  }

  next(x: number): number {
    const evicted = this.buf.push(x) ?? 0; // push returns the evicted value once full
    this.sum += x - evicted;
    return this.sum / this.buf.size;
  }
}
