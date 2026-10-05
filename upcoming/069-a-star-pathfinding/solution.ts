// A* search on a 2-D grid ('#' = wall). 4-directional moves cost 1;
// with `diagonal`, 8-directional moves cost √2 and corner-cutting is disallowed.

export type Point = [row: number, col: number];

class MinHeap<T> {
  private a: { f: number; h: number; v: T }[] = [];
  get size() {
    return this.a.length;
  }
  // Ties on f broken by smaller h: prefer nodes closer to the goal.
  private less(i: number, j: number) {
    const x = this.a[i], y = this.a[j];
    return x.f < y.f || (x.f === y.f && x.h < y.h);
  }
  push(f: number, h: number, v: T) {
    this.a.push({ f, h, v });
    for (let i = this.a.length - 1; i > 0; ) {
      const p = (i - 1) >> 1;
      if (!this.less(i, p)) break;
      [this.a[i], this.a[p]] = [this.a[p], this.a[i]];
      i = p;
    }
  }
  pop(): T {
    const top = this.a[0].v;
    const last = this.a.pop()!;
    if (this.a.length) {
      this.a[0] = last;
      for (let i = 0; ; ) {
        const l = 2 * i + 1, r = l + 1;
        let m = i;
        if (l < this.a.length && this.less(l, m)) m = l;
        if (r < this.a.length && this.less(r, m)) m = r;
        if (m === i) break;
        [this.a[i], this.a[m]] = [this.a[m], this.a[i]];
        i = m;
      }
    }
    return top;
  }
}

export interface PathResult {
  path: Point[]; // start..goal inclusive, [] if unreachable
  cost: number; // Infinity if unreachable
  expanded: number; // nodes popped, to compare heuristics
}

export function aStar(grid: string[], start: Point, goal: Point, { diagonal = false } = {}): PathResult {
  const rows = grid.length;
  const cols = rows ? grid[0].length : 0;
  const open = (r: number, c: number) => r >= 0 && r < rows && c >= 0 && c < cols && grid[r][c] !== "#";
  if (!open(...start) || !open(...goal)) return { path: [], cost: Infinity, expanded: 0 };

  // Admissible heuristics: Manhattan for 4-way, octile for 8-way.
  const h = (r: number, c: number) => {
    const dr = Math.abs(r - goal[0]), dc = Math.abs(c - goal[1]);
    return diagonal ? Math.max(dr, dc) + (Math.SQRT2 - 1) * Math.min(dr, dc) : dr + dc;
  };
  const moves: [number, number, number][] = [[0, 1, 1], [1, 0, 1], [0, -1, 1], [-1, 0, 1]];
  if (diagonal) for (const dr of [-1, 1]) for (const dc of [-1, 1]) moves.push([dr, dc, Math.SQRT2]);

  const id = (r: number, c: number) => r * cols + c;
  const g = new Float64Array(rows * cols).fill(Infinity);
  const parent = new Int32Array(rows * cols).fill(-1);
  const closed = new Uint8Array(rows * cols);
  const heap = new MinHeap<number>();
  const s = id(...start), t = id(...goal);
  g[s] = 0;
  heap.push(h(...start), h(...start), s);
  let expanded = 0;

  while (heap.size) {
    const u = heap.pop();
    if (closed[u]) continue; // stale heap entry
    closed[u] = 1;
    expanded++;
    if (u === t) break;
    const ur = Math.floor(u / cols), uc = u % cols;
    for (const [dr, dc, w] of moves) {
      const vr = ur + dr, vc = uc + dc;
      if (!open(vr, vc)) continue;
      if (dr && dc && (!open(ur + dr, uc) || !open(ur, uc + dc))) continue; // no corner cutting
      const v = id(vr, vc);
      const ng = g[u] + w;
      if (ng < g[v] - 1e-12) {
        g[v] = ng;
        parent[v] = u;
        heap.push(ng + h(vr, vc), h(vr, vc), v);
      }
    }
  }

  if (g[t] === Infinity) return { path: [], cost: Infinity, expanded };
  const path: Point[] = [];
  for (let v = t; v !== -1; v = parent[v]) path.push([Math.floor(v / cols), v % cols]);
  return { path: path.reverse(), cost: g[t], expanded };
}
