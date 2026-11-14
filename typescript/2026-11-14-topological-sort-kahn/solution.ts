// Topological ordering of a DAG with Kahn's algorithm (BFS on in-degrees),
// with cycle detection and a deterministic, lexicographically smallest variant.

export class CycleError extends Error {
  constructor(readonly remaining: string[]) {
    super(`graph has a cycle among: ${remaining.join(", ")}`);
  }
}

/**
 * `deps` maps each node to the nodes it depends on (which must come first).
 * Nodes that only appear as dependencies are included automatically.
 */
export function topoSort(deps: Record<string, string[]>): string[] {
  const { nodes, indeg, out } = build(deps);
  const queue = nodes.filter((n) => indeg.get(n) === 0);
  const order: string[] = [];
  for (let head = 0; head < queue.length; head++) {
    const u = queue[head];
    order.push(u);
    for (const v of out.get(u)!) {
      const d = indeg.get(v)! - 1;
      indeg.set(v, d);
      if (d === 0) queue.push(v);
    }
  }
  if (order.length !== nodes.length) throw new CycleError(nodes.filter((n) => indeg.get(n)! > 0));
  return order;
}

/** Same, but always picks the alphabetically smallest ready node (uses a sorted ready list). */
export function topoSortLexicographic(deps: Record<string, string[]>): string[] {
  const { nodes, indeg, out } = build(deps);
  const ready = nodes.filter((n) => indeg.get(n) === 0).sort().reverse(); // pop() gives smallest
  const order: string[] = [];
  while (ready.length > 0) {
    const u = ready.pop()!;
    order.push(u);
    for (const v of out.get(u)!) {
      const d = indeg.get(v)! - 1;
      indeg.set(v, d);
      if (d === 0) {
        // Insert keeping descending order. O(n) per insert; a heap gives O(log n).
        let i = ready.findIndex((x) => x < v);
        if (i === -1) i = ready.length;
        ready.splice(i, 0, v);
      }
    }
  }
  if (order.length !== nodes.length) throw new CycleError(nodes.filter((n) => indeg.get(n)! > 0));
  return order;
}

/** Groups nodes into "waves" that can run in parallel; wave k depends only on earlier waves. */
export function parallelLevels(deps: Record<string, string[]>): string[][] {
  const { nodes, indeg, out } = build(deps);
  let level = nodes.filter((n) => indeg.get(n) === 0);
  const levels: string[][] = [];
  let seen = 0;
  while (level.length > 0) {
    levels.push(level);
    seen += level.length;
    const next: string[] = [];
    for (const u of level) {
      for (const v of out.get(u)!) {
        const d = indeg.get(v)! - 1;
        indeg.set(v, d);
        if (d === 0) next.push(v);
      }
    }
    level = next;
  }
  if (seen !== nodes.length) throw new CycleError(nodes.filter((n) => indeg.get(n)! > 0));
  return levels;
}

function build(deps: Record<string, string[]>) {
  const out = new Map<string, string[]>(); // dependency -> dependents
  const indeg = new Map<string, number>();
  const add = (n: string) => {
    if (!out.has(n)) {
      out.set(n, []);
      indeg.set(n, 0);
    }
  };
  for (const [node, reqs] of Object.entries(deps)) {
    add(node);
    for (const r of new Set(reqs)) {
      add(r);
      out.get(r)!.push(node);
      indeg.set(node, indeg.get(node)! + 1);
    }
  }
  return { nodes: [...out.keys()], indeg, out };
}
