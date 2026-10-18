import java.util.ArrayList;
import java.util.List;

/**
 * AVL tree: a BST where every node's subtrees differ in height by at most 1.
 * Insert and delete rebalance with rotations on the way back up the recursion.
 */
class AvlTree<K extends Comparable<K>> {
    private static final class Node<K> {
        K key;
        Node<K> left, right;
        int height = 1;
        int size = 1; // subtree size, for rank/select

        Node(K key) {
            this.key = key;
        }
    }

    private Node<K> root;

    int size() {
        return size(root);
    }

    int height() {
        return height(root);
    }

    boolean contains(K key) {
        Node<K> n = root;
        while (n != null) {
            int c = key.compareTo(n.key);
            if (c == 0) return true;
            n = c < 0 ? n.left : n.right;
        }
        return false;
    }

    /** Returns false if the key was already present. */
    boolean insert(K key) {
        int before = size();
        root = insert(root, key);
        return size() > before;
    }

    boolean remove(K key) {
        int before = size();
        root = remove(root, key);
        return size() < before;
    }

    /** k-th smallest key, 0-based. */
    K select(int k) {
        if (k < 0 || k >= size()) throw new IndexOutOfBoundsException("k = " + k);
        Node<K> n = root;
        while (true) {
            int leftSize = size(n.left);
            if (k < leftSize) n = n.left;
            else if (k > leftSize) {
                k -= leftSize + 1;
                n = n.right;
            } else return n.key;
        }
    }

    /** Number of keys strictly less than key. */
    int rank(K key) {
        int r = 0;
        Node<K> n = root;
        while (n != null) {
            int c = key.compareTo(n.key);
            if (c <= 0) n = n.left;
            else {
                r += size(n.left) + 1;
                n = n.right;
            }
        }
        return r;
    }

    List<K> inOrder() {
        List<K> out = new ArrayList<>();
        inOrder(root, out);
        return out;
    }

    /** Verifies BST order, AVL balance, and cached heights/sizes. Used by tests. */
    boolean isValid() {
        return check(root, null, null) >= 0;
    }

    // ---- recursive helpers ----

    private Node<K> insert(Node<K> n, K key) {
        if (n == null) return new Node<>(key);
        int c = key.compareTo(n.key);
        if (c < 0) n.left = insert(n.left, key);
        else if (c > 0) n.right = insert(n.right, key);
        else return n; // duplicate
        return rebalance(n);
    }

    private Node<K> remove(Node<K> n, K key) {
        if (n == null) return null;
        int c = key.compareTo(n.key);
        if (c < 0) n.left = remove(n.left, key);
        else if (c > 0) n.right = remove(n.right, key);
        else {
            if (n.left == null) return n.right;
            if (n.right == null) return n.left;
            // Two children: replace with the in-order successor, then delete it from the right.
            Node<K> succ = n.right;
            while (succ.left != null) succ = succ.left;
            n.key = succ.key;
            n.right = remove(n.right, succ.key);
        }
        return rebalance(n);
    }

    private Node<K> rebalance(Node<K> n) {
        update(n);
        int bf = balance(n);
        if (bf > 1) { // left heavy
            if (balance(n.left) < 0) n.left = rotateLeft(n.left); // left-right case
            return rotateRight(n);
        }
        if (bf < -1) { // right heavy
            if (balance(n.right) > 0) n.right = rotateRight(n.right); // right-left case
            return rotateLeft(n);
        }
        return n;
    }

    //     n            l
    //    / \          / \
    //   l   c   ->   a   n
    //  / \              / \
    // a   b            b   c
    private Node<K> rotateRight(Node<K> n) {
        Node<K> l = n.left;
        n.left = l.right;
        l.right = n;
        update(n);
        update(l);
        return l;
    }

    private Node<K> rotateLeft(Node<K> n) {
        Node<K> r = n.right;
        n.right = r.left;
        r.left = n;
        update(n);
        update(r);
        return r;
    }

    private void update(Node<K> n) {
        n.height = 1 + Math.max(height(n.left), height(n.right));
        n.size = 1 + size(n.left) + size(n.right);
    }

    private int balance(Node<K> n) {
        return height(n.left) - height(n.right);
    }

    private static int height(Node<?> n) {
        return n == null ? 0 : n.height;
    }

    private static int size(Node<?> n) {
        return n == null ? 0 : n.size;
    }

    private void inOrder(Node<K> n, List<K> out) {
        if (n == null) return;
        inOrder(n.left, out);
        out.add(n.key);
        inOrder(n.right, out);
    }

    /** Returns subtree height, or -1 if any invariant is broken. */
    private int check(Node<K> n, K lo, K hi) {
        if (n == null) return 0;
        if ((lo != null && n.key.compareTo(lo) <= 0) || (hi != null && n.key.compareTo(hi) >= 0)) return -1;
        int lh = check(n.left, lo, n.key), rh = check(n.right, n.key, hi);
        if (lh < 0 || rh < 0 || Math.abs(lh - rh) > 1) return -1;
        int h = 1 + Math.max(lh, rh);
        if (h != n.height || n.size != 1 + size(n.left) + size(n.right)) return -1;
        return h;
    }
}
