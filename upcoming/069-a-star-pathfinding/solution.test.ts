import { test } from "node:test";
import assert from "node:assert/strict";
import { aStar, type Point } from "./solution.ts";

function bfsDistance(grid: string[], s: Point, t: Point): number {
  const rows = grid.length, cols = grid[0].length;
  if (grid[s[0]][s[1]] === "#" || grid[t[0]][t[1]] === "#") return Infinity;
  const dist = new Map<string, number>([[`${s}`, 0]]);
  const q: Point[] = [s];
  for (let i = 0; i < q.length; i++) {
    const [r, c] = q[i];
    if (r === t[0] && c === t[1]) return dist.get(`${q[i]}`)!;
    for (const [dr, dc] of [[0, 1], [1, 0], [0, -1], [-1, 0]]) {
      const nr = r + dr, nc = c + dc;
      if (nr < 0 || nc < 0 || nr >= rows || nc >= cols || grid[nr][nc] === "#" || dist.has(`${nr},${nc}`)) continue;
      dist.set(`${nr},${nc}`, dist.get(`${r},${c}`)! + 1);
      q.push([nr, nc]);
    }
  }
  return Infinity;
}

function assertValidPath(grid: string[], path: Point[], diagonal = false) {
  for (const [r, c] of path) assert.notEqual(grid[r][c], "#");
  for (let i = 1; i < path.length; i++) {
    const dr = Math.abs(path[i][0] - path[i - 1][0]), dc = Math.abs(path[i][1] - path[i - 1][1]);
    assert.ok(diagonal ? Math.max(dr, dc) === 1 : dr + dc === 1, "steps must be adjacent");
  }
}

const maze = [
  "S...#....",
  ".##.#.##.",
  ".#..#..#.",
  ".#.###.#.",
  ".#.....#G",
];

test("finds shortest path through a maze", () => {
  const res = aStar(maze, [0, 0], [4, 8]);
  assert.equal(res.cost, bfsDistance(maze, [0, 0], [4, 8]));
  assert.deepEqual(res.path[0], [0, 0]);
  assert.deepEqual(res.path.at(-1), [4, 8]);
  assert.equal(res.path.length, res.cost + 1);
  assertValidPath(maze, res.path);
});

test("start equals goal", () => {
  assert.deepEqual(aStar(["."], [0, 0], [0, 0]), { path: [[0, 0]], cost: 0, expanded: 1 });
});

test("unreachable goal and blocked endpoints", () => {
  const g = ["..#..", "..#..", "..#.."];
  assert.equal(aStar(g, [0, 0], [0, 4]).cost, Infinity);
  assert.deepEqual(aStar(g, [0, 0], [0, 4]).path, []);
  assert.equal(aStar(g, [0, 2], [0, 0]).cost, Infinity);
});

test("diagonal moves use octile cost and don't cut corners", () => {
  const open = [".....", ".....", "....."];
  const r = aStar(open, [0, 0], [2, 4], { diagonal: true });
  assert.ok(Math.abs(r.cost - (2 * Math.SQRT2 + 2)) < 1e-9);
  assertValidPath(open, r.path, true);
  const corner = [".#", "#."];
  assert.equal(aStar(corner, [0, 0], [1, 1], { diagonal: true }).cost, Infinity);
});

test("heuristic guides search: fewer expansions than the open area", () => {
  const big = Array.from({ length: 50 }, () => ".".repeat(50));
  const r = aStar(big, [0, 0], [0, 49]);
  assert.equal(r.cost, 49);
  assert.ok(r.expanded < 200, `expanded ${r.expanded}`);
});

test("randomized grids: cost matches BFS and path is valid", () => {
  let seed = 101;
  const rand = (n: number) => (seed = (seed * 48271) % 2147483647) % n;
  for (let t = 0; t < 200; t++) {
    const rows = 2 + rand(8), cols = 2 + rand(8);
    const grid = Array.from({ length: rows }, () =>
      Array.from({ length: cols }, () => (rand(10) < 3 ? "#" : ".")).join(""),
    );
    const s: Point = [rand(rows), rand(cols)], g: Point = [rand(rows), rand(cols)];
    const res = aStar(grid, s, g);
    assert.equal(res.cost, bfsDistance(grid, s, g));
    if (res.cost !== Infinity) {
      assert.equal(res.path.length, res.cost + 1);
      assertValidPath(grid, res.path);
    }
  }
});
