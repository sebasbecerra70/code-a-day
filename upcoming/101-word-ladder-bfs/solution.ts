// Word ladder: transform `begin` into `end` one letter at a time, where every
// intermediate word must be in the dictionary. Unweighted shortest path => BFS.
// Neighbors are found through wildcard buckets ("h*t" -> hot, hit, hat) so
// each word costs O(L) bucket lookups instead of scanning the whole list.

function buildBuckets(words: Iterable<string>): Map<string, string[]> {
  const buckets = new Map<string, string[]>();
  for (const w of words) {
    for (let i = 0; i < w.length; i++) {
      const key = w.slice(0, i) + "*" + w.slice(i + 1);
      const list = buckets.get(key);
      if (list) list.push(w);
      else buckets.set(key, [w]);
    }
  }
  return buckets;
}

function* neighbors(word: string, buckets: Map<string, string[]>): Generator<string> {
  for (let i = 0; i < word.length; i++) {
    const key = word.slice(0, i) + "*" + word.slice(i + 1);
    for (const n of buckets.get(key) ?? []) if (n !== word) yield n;
  }
}

/** Number of words in the shortest ladder (including both ends), or 0 if impossible. */
export function ladderLength(begin: string, end: string, wordList: string[]): number {
  const path = shortestLadder(begin, end, wordList);
  return path ? path.length : 0;
}

/** One shortest ladder, or null. Bidirectional BFS: always expand the smaller frontier. */
export function shortestLadder(begin: string, end: string, wordList: string[]): string[] | null {
  const dict = new Set(wordList);
  if (!dict.has(end) || begin.length !== end.length) return null;
  if (begin === end) return [begin];
  dict.add(begin);
  const buckets = buildBuckets(dict);

  // parent maps record how each side reached a word; they double as visited sets.
  const fromBegin = new Map<string, string | null>([[begin, null]]);
  const fromEnd = new Map<string, string | null>([[end, null]]);
  let front = [begin], back = [end];

  const build = (meet: string): string[] => {
    const left: string[] = [];
    for (let w: string | null = meet; w !== null; w = fromBegin.get(w)!) left.push(w);
    left.reverse();
    for (let w = fromEnd.get(meet)!; w !== null; w = fromEnd.get(w)!) left.push(w);
    return left;
  };

  while (front.length && back.length) {
    const forward = front.length <= back.length;
    const [frontier, mine, other] = forward ? [front, fromBegin, fromEnd] : [back, fromEnd, fromBegin];
    const next: string[] = [];
    for (const w of frontier) {
      for (const n of neighbors(w, buckets)) {
        if (mine.has(n)) continue;
        mine.set(n, w);
        if (other.has(n)) return build(n);
        next.push(n);
      }
    }
    if (forward) front = next;
    else back = next;
  }
  return null;
}

/** All shortest ladders (Word Ladder II): BFS by levels building a parent DAG, then DFS back from `end`. */
export function allShortestLadders(begin: string, end: string, wordList: string[]): string[][] {
  const dict = new Set(wordList);
  if (!dict.has(end) || begin.length !== end.length) return [];
  if (begin === end) return [[begin]];
  dict.add(begin);
  const buckets = buildBuckets(dict);

  const parents = new Map<string, string[]>();
  const depth = new Map<string, number>([[begin, 0]]);
  let level = [begin];
  while (level.length && !depth.has(end)) {
    const next: string[] = [];
    const d = depth.get(level[0])! + 1;
    for (const w of level) {
      for (const n of neighbors(w, buckets)) {
        const dn = depth.get(n);
        if (dn === undefined) {
          depth.set(n, d);
          parents.set(n, [w]);
          next.push(n);
        } else if (dn === d) {
          parents.get(n)!.push(w); // another shortest way into n
        }
      }
    }
    level = next;
  }
  if (!depth.has(end)) return [];

  const out: string[][] = [];
  const path = [end];
  const dfs = (w: string): void => {
    if (w === begin) {
      out.push([...path].reverse());
      return;
    }
    for (const p of parents.get(w)!) {
      path.push(p);
      dfs(p);
      path.pop();
    }
  };
  dfs(end);
  return out.sort((a, b) => a.join().localeCompare(b.join()));
}
