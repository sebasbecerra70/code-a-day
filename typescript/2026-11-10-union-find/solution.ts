// Disjoint-set union with path compression and union by size:
// near-constant amortized time per operation (inverse Ackermann).

export class UnionFind {
  private parent: Int32Array;
  private size: Int32Array;
  private _components: number;

  constructor(n: number) {
    if (!Number.isInteger(n) || n < 0) throw new RangeError("n must be a non-negative integer");
    this.parent = new Int32Array(n);
    this.size = new Int32Array(n).fill(1);
    for (let i = 0; i < n; i++) this.parent[i] = i;
    this._components = n;
  }

  get components(): number {
    return this._components;
  }

  find(x: number): number {
    this.check(x);
    let root = x;
    while (this.parent[root] !== root) root = this.parent[root];
    // Path compression: point every node on the path straight at the root.
    // Iterative so long chains can't overflow the stack.
    while (this.parent[x] !== root) {
      const next = this.parent[x];
      this.parent[x] = root;
      x = next;
    }
    return root;
  }

  /** Merges the sets containing a and b. Returns false if already joined. */
  union(a: number, b: number): boolean {
    let ra = this.find(a);
    let rb = this.find(b);
    if (ra === rb) return false;
    // Union by size: hang the smaller tree under the larger.
    if (this.size[ra] < this.size[rb]) [ra, rb] = [rb, ra];
    this.parent[rb] = ra;
    this.size[ra] += this.size[rb];
    this._components--;
    return true;
  }

  connected(a: number, b: number): boolean {
    return this.find(a) === this.find(b);
  }

  sizeOf(x: number): number {
    return this.size[this.find(x)];
  }

  private check(x: number): void {
    if (!Number.isInteger(x) || x < 0 || x >= this.parent.length) throw new RangeError(`bad element ${x}`);
  }
}

/** Example application: number of islands of '1' cells in a grid. */
export function countIslands(grid: string[]): number {
  const rows = grid.length;
  const cols = rows ? grid[0].length : 0;
  const uf = new UnionFind(rows * cols);
  let water = 0;
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      if (grid[r][c] !== "1") {
        water++;
        continue;
      }
      if (r + 1 < rows && grid[r + 1][c] === "1") uf.union(r * cols + c, (r + 1) * cols + c);
      if (c + 1 < cols && grid[r][c + 1] === "1") uf.union(r * cols + c, r * cols + c + 1);
    }
  }
  return uf.components - water;
}
