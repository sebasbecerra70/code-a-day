import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

class Islands {
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * Sizes of all 4-connected islands of '1' cells, largest first. BFS flood
     * fill with an explicit queue (recursion would overflow on a 1000x1000
     * all-land grid). The input grid is not modified.
     */
    static List<Integer> islandSizes(char[][] grid) {
        int rows = grid.length, cols = rows == 0 ? 0 : grid[0].length;
        boolean[][] seen = new boolean[rows][cols];
        List<Integer> sizes = new ArrayList<>();
        Deque<int[]> q = new ArrayDeque<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] != '1' || seen[r][c]) continue;
                int size = 0;
                seen[r][c] = true;
                q.add(new int[] {r, c});
                while (!q.isEmpty()) {
                    int[] cell = q.poll();
                    size++;
                    for (int[] d : DIRS) {
                        int nr = cell[0] + d[0], nc = cell[1] + d[1];
                        // Mark when enqueuing, not when dequeuing, so no cell is queued twice.
                        if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] == '1' && !seen[nr][nc]) {
                            seen[nr][nc] = true;
                            q.add(new int[] {nr, nc});
                        }
                    }
                }
                sizes.add(size);
            }
        }
        sizes.sort(Collections.reverseOrder());
        return sizes;
    }

    static int count(char[][] grid) {
        return islandSizes(grid).size();
    }

    static int maxArea(char[][] grid) {
        List<Integer> s = islandSizes(grid);
        return s.isEmpty() ? 0 : s.get(0);
    }
}

/**
 * "Number of islands II": cells turn from water to land one at a time, and we
 * report the island count after each addition. Union-find makes each step
 * nearly O(1) instead of re-scanning the grid.
 */
class DynamicIslands {
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final int rows, cols;
    private final int[] parent, rank;
    private int islands;

    DynamicIslands(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        parent = new int[rows * cols];
        rank = new int[rows * cols];
        Arrays.fill(parent, -1); // -1 = water
    }

    /** Turns (r, c) into land and returns the new island count. Re-adding land is a no-op. */
    int addLand(int r, int c) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) throw new IndexOutOfBoundsException(r + "," + c);
        int id = r * cols + c;
        if (parent[id] != -1) return islands;
        parent[id] = id;
        islands++; // a new island, until it merges with neighbors
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (nr < 0 || nr >= rows || nc < 0 || nc >= cols) continue;
            int nid = nr * cols + nc;
            if (parent[nid] != -1 && union(id, nid)) islands--;
        }
        return islands;
    }

    private int find(int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]]; // path halving
            x = parent[x];
        }
        return x;
    }

    /** Returns true if two separate islands were merged. */
    private boolean union(int a, int b) {
        int ra = find(a), rb = find(b);
        if (ra == rb) return false;
        if (rank[ra] < rank[rb]) {
            int t = ra;
            ra = rb;
            rb = t;
        }
        parent[rb] = ra;
        if (rank[ra] == rank[rb]) rank[ra]++;
        return true;
    }

    int islands() {
        return islands;
    }
}
