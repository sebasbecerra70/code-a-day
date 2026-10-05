import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** Plain binary tree node. */
class TreeNode {
    int val;
    TreeNode left, right;

    TreeNode(int val) {
        this.val = val;
    }

    /** Inserts into a BST rooted at {@code root}, returning the (possibly new) root. Duplicates are ignored. */
    static TreeNode insert(TreeNode root, int val) {
        if (root == null) return new TreeNode(val);
        if (val < root.val) root.left = insert(root.left, val);
        else if (val > root.val) root.right = insert(root.right, val);
        return root;
    }
}

/**
 * In-order iterator using an explicit stack of the "left spine".
 * next() is amortized O(1); memory is O(h).
 */
class InorderIterator implements Iterator<Integer> {
    private final Deque<TreeNode> stack = new ArrayDeque<>();
    private final boolean reverse;

    InorderIterator(TreeNode root) {
        this(root, false);
    }

    /** With {@code reverse} set, yields values in descending order (right spine first). */
    InorderIterator(TreeNode root, boolean reverse) {
        this.reverse = reverse;
        pushSpine(root);
    }

    private void pushSpine(TreeNode node) {
        while (node != null) {
            stack.push(node);
            node = reverse ? node.right : node.left;
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    /** Returns the next value without consuming it. */
    int peek() {
        if (stack.isEmpty()) throw new NoSuchElementException();
        return stack.peek().val;
    }

    @Override
    public Integer next() {
        if (stack.isEmpty()) throw new NoSuchElementException();
        TreeNode node = stack.pop();
        pushSpine(reverse ? node.left : node.right);
        return node.val;
    }
}

/** Pre-order iterator: visit node, then left, then right. */
class PreorderIterator implements Iterator<Integer> {
    private final Deque<TreeNode> stack = new ArrayDeque<>();

    PreorderIterator(TreeNode root) {
        if (root != null) stack.push(root);
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public Integer next() {
        if (stack.isEmpty()) throw new NoSuchElementException();
        TreeNode node = stack.pop();
        // Push right first so left is processed first.
        if (node.right != null) stack.push(node.right);
        if (node.left != null) stack.push(node.left);
        return node.val;
    }
}

/**
 * Post-order iterator with a single stack: descend to the next leaf-most node,
 * preferring left children, and emit a node once both subtrees are done.
 */
class PostorderIterator implements Iterator<Integer> {
    private final Deque<TreeNode> stack = new ArrayDeque<>();

    PostorderIterator(TreeNode root) {
        descend(root);
    }

    // Push the path to the first node in post-order of this subtree.
    private void descend(TreeNode node) {
        while (node != null) {
            stack.push(node);
            node = node.left != null ? node.left : node.right;
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public Integer next() {
        if (stack.isEmpty()) throw new NoSuchElementException();
        TreeNode node = stack.pop();
        // If we just finished the parent's left subtree, its right subtree comes next.
        if (!stack.isEmpty() && stack.peek().left == node) descend(stack.peek().right);
        return node.val;
    }
}

class BstAlgorithms {
    /** Two-sum on a BST in O(n) time and O(h) space using a forward and a reverse iterator. */
    static boolean hasPairWithSum(TreeNode root, int target) {
        InorderIterator lo = new InorderIterator(root, false);
        InorderIterator hi = new InorderIterator(root, true);
        if (!lo.hasNext()) return false;
        int a = lo.next(), b = hi.next();
        while (a < b) {
            long sum = (long) a + b;
            if (sum == target) return true;
            if (sum < target) a = lo.next();
            else b = hi.next();
        }
        return false;
    }

    /** k-th smallest (1-based) by advancing the in-order iterator k times. */
    static int kthSmallest(TreeNode root, int k) {
        InorderIterator it = new InorderIterator(root);
        for (int i = 1; i < k; i++) it.next();
        return it.next();
    }
}
