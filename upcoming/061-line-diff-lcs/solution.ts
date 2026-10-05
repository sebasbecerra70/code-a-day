// Line-based diff using a longest-common-subsequence table, plus a
// unified-style renderer.

export type DiffOp =
  | { kind: "equal"; line: string }
  | { kind: "delete"; line: string }
  | { kind: "insert"; line: string };

/** LCS length table: t[i][j] = LCS of a[i:] and b[j:] (suffix form makes the walk forward). */
function lcsTable(a: readonly string[], b: readonly string[]): Uint32Array[] {
  const t = Array.from({ length: a.length + 1 }, () => new Uint32Array(b.length + 1));
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      t[i][j] = a[i] === b[j] ? t[i + 1][j + 1] + 1 : Math.max(t[i + 1][j], t[i][j + 1]);
    }
  }
  return t;
}

export function lcs(a: readonly string[], b: readonly string[]): string[] {
  return diffLines(a, b)
    .filter((op) => op.kind === "equal")
    .map((op) => op.line);
}

export function diffLines(a: readonly string[], b: readonly string[]): DiffOp[] {
  // Trim common prefix/suffix first: cheap, and typical diffs are small edits.
  let start = 0;
  while (start < a.length && start < b.length && a[start] === b[start]) start++;
  let endA = a.length, endB = b.length;
  while (endA > start && endB > start && a[endA - 1] === b[endB - 1]) {
    endA--;
    endB--;
  }
  const midA = a.slice(start, endA);
  const midB = b.slice(start, endB);
  const t = lcsTable(midA, midB);

  const ops: DiffOp[] = a.slice(0, start).map((line) => ({ kind: "equal", line }));
  let i = 0, j = 0;
  while (i < midA.length && j < midB.length) {
    if (midA[i] === midB[j]) {
      ops.push({ kind: "equal", line: midA[i] });
      i++;
      j++;
    } else if (t[i + 1][j] >= t[i][j + 1]) {
      ops.push({ kind: "delete", line: midA[i++] }); // prefer deletions before insertions
    } else {
      ops.push({ kind: "insert", line: midB[j++] });
    }
  }
  while (i < midA.length) ops.push({ kind: "delete", line: midA[i++] });
  while (j < midB.length) ops.push({ kind: "insert", line: midB[j++] });
  for (const line of a.slice(endA)) ops.push({ kind: "equal", line });
  return ops;
}

/** Renders ops as lines prefixed with ' ', '-', '+'. */
export function formatDiff(ops: readonly DiffOp[]): string {
  const sign = { equal: " ", delete: "-", insert: "+" } as const;
  return ops.map((op) => sign[op.kind] + op.line).join("\n");
}

/** Applies a diff to the old lines, verifying it matches. Returns the new lines. */
export function applyDiff(a: readonly string[], ops: readonly DiffOp[]): string[] {
  const out: string[] = [];
  let i = 0;
  for (const op of ops) {
    if (op.kind !== "insert") {
      if (a[i] !== op.line) throw new Error(`diff does not apply at line ${i + 1}`);
      i++;
    }
    if (op.kind !== "delete") out.push(op.line);
  }
  if (i !== a.length) throw new Error("diff does not consume all lines");
  return out;
}
