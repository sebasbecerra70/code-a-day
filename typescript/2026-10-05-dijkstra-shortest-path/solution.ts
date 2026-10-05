// Dijkstra's single-source shortest paths over a weighted directed graph
// with non-negative edge weights, using a hand-rolled binary min-heap.

export type Edge = [to: number, weight: number];
export type Graph = Edge[][]; // adjacency list indexed by node id

class MinHeap {
  private items: [dist: number, node: number][] = [];

  get size(): number {
    return this.items.length;
  }

  push(item: [number, number]): void {
    const a = this.items;
    a.push(item);
    let i = a.length - 1;
    while (i > 0) {
      const p = (i - 1) >> 1;
      if (a[p][0] <= a[i][0]) break;
      [a[p], a[i]] = [a[i], a[p]];
      i = p;
    }
  }

  pop(): [number, number] | undefined {
    const a = this.items;
    if (a.length === 0) return undefined;
    const top = a[0];
    const last = a.pop()!;
    if (a.length > 0) {
      a[0] = last;
      let i = 0;
      for (;;) {
        const l = 2 * i + 1;
        const r = l + 1;
        let m = i;
        if (l < a.length && a[l][0] < a[m][0]) m = l;
        if (r < a.length && a[r][0] < a[m][0]) m = r;
        if (m === i) break;
        [a[m], a[i]] = [a[i], a[m]];
        i = m;
      }
    }
    return top;
  }
}

export interface ShortestPaths {
  dist: number[]; // Infinity when unreachable
  prev: number[]; // -1 when no predecessor
}

export function dijkstra(graph: Graph, source: number): ShortestPaths {
  const n = graph.length;
  if (source < 0 || source >= n) throw new RangeError("source out of range");
  const dist = new Array<number>(n).fill(Infinity);
  const prev = new Array<number>(n).fill(-1);
  dist[source] = 0;
  const heap = new MinHeap();
  heap.push([0, source]);

  while (heap.size > 0) {
    const [d, u] = heap.pop()!;
    if (d > dist[u]) continue; // stale entry (lazy deletion)
    for (const [v, w] of graph[u]) {
      if (w < 0) throw new RangeError("negative edge weight");
      const nd = d + w;
      if (nd < dist[v]) {
        dist[v] = nd;
        prev[v] = u;
        heap.push([nd, v]);
      }
    }
  }
  return { dist, prev };
}

/** Rebuilds the path source -> target from `prev`, or [] if unreachable. */
export function pathTo(paths: ShortestPaths, target: number): number[] {
  if (paths.dist[target] === Infinity) return [];
  const path: number[] = [];
  for (let v = target; v !== -1; v = paths.prev[v]) path.push(v);
  return path.reverse();
}
