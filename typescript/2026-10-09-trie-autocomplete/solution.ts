// Prefix tree supporting insert with frequency counts, exact lookup,
// deletion, and top-k autocomplete for a prefix.

class TrieNode {
  children = new Map<string, TrieNode>();
  count = 0; // > 0 marks the end of a stored word; value is its frequency
}

export class Trie {
  private root = new TrieNode();
  private _size = 0;

  /** Number of distinct words stored. */
  get size(): number {
    return this._size;
  }

  insert(word: string, times = 1): void {
    if (times <= 0) throw new RangeError("times must be positive");
    let node = this.root;
    for (const ch of word) {
      let next = node.children.get(ch);
      if (!next) node.children.set(ch, (next = new TrieNode()));
      node = next;
    }
    if (node.count === 0) this._size++;
    node.count += times;
  }

  private find(prefix: string): TrieNode | undefined {
    let node: TrieNode | undefined = this.root;
    for (const ch of prefix) {
      node = node.children.get(ch);
      if (!node) return undefined;
    }
    return node;
  }

  has(word: string): boolean {
    return (this.find(word)?.count ?? 0) > 0;
  }

  frequency(word: string): number {
    return this.find(word)?.count ?? 0;
  }

  startsWith(prefix: string): boolean {
    return this.find(prefix) !== undefined;
  }

  /** Removes a word entirely; prunes nodes that no longer lead anywhere. */
  delete(word: string): boolean {
    const chars = [...word];
    const path: TrieNode[] = [this.root];
    for (const ch of chars) {
      const next = path[path.length - 1].children.get(ch);
      if (!next) return false;
      path.push(next);
    }
    const end = path[path.length - 1];
    if (end.count === 0) return false;
    end.count = 0;
    this._size--;
    for (let i = chars.length; i > 0; i--) {
      const node = path[i];
      if (node.count > 0 || node.children.size > 0) break;
      path[i - 1].children.delete(chars[i - 1]);
    }
    return true;
  }

  /**
   * Up to `k` words starting with `prefix`, most frequent first;
   * ties broken alphabetically.
   */
  autocomplete(prefix: string, k = 5): string[] {
    const start = this.find(prefix);
    if (!start || k <= 0) return [];
    const found: [string, number][] = [];
    const stack: [TrieNode, string][] = [[start, prefix]];
    while (stack.length > 0) {
      const [node, word] = stack.pop()!;
      if (node.count > 0) found.push([word, node.count]);
      for (const [ch, child] of node.children) stack.push([child, word + ch]);
    }
    found.sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
    return found.slice(0, k).map(([w]) => w);
  }
}
