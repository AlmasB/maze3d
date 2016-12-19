package com.almasb.maze;

import java.util.Arrays;
import java.util.Collections;

/**
 * Modified and adapted for general use version of
 * recursive backtracking algorithm
 * shamelessly borrowed from the ruby at
 * http://weblog.jamisbuck.org/2010/12/27/maze-generation-recursive-backtracking
 *
 * @author Almas Baimagambetov (almaslvl@gmail.com)
 */
public class MazeGenerator {
    private final int x;
    private final int y;
    private final int[][] maze;

    private MazeCell[][] theMaze;

    public MazeGenerator(int x, int y) {
        this.x = x;
        this.y = y;
        maze = new int[this.x][this.y];
        generateMaze(0, 0);
        adapt();
    }

    public MazeCell[][] getMaze() {
        return theMaze;
    }

    private void adapt() {
        theMaze = new MazeCell[x][y];

        for (int i = 0; i < y; ++i) {
            for (int j = 0; j < x; ++j) {
                MazeCell cell = new MazeCell();
                if ((maze[j][i] & 1) == 0) cell.topWall = true;
                if ((maze[j][i] & 8) == 0) cell.leftWall = true;

                theMaze[j][i] = cell;
            }
        }
    }

    private void generateMaze(int cx, int cy) {
        DIR[] dirs = DIR.values();
        Collections.shuffle(Arrays.asList(dirs));
        for (DIR dir : dirs) {
            int nx = cx + dir.dx;
            int ny = cy + dir.dy;
            if (between(nx, x) && between(ny, y) && (maze[nx][ny] == 0)) {
                maze[cx][cy] |= dir.bit;
                maze[nx][ny] |= dir.opposite.bit;
                generateMaze(nx, ny);
            }
        }
    }

    private static boolean between(int v, int upper) {
        return (v >= 0) && (v < upper);
    }

    private enum DIR {
        N(1, 0, -1), S(2, 0, 1), E(4, 1, 0), W(8, -1, 0);
        private final int bit;
        private final int dx;
        private final int dy;
        private DIR opposite;

        // use the static initializer to resolve forward references
        static {
            N.opposite = S;
            S.opposite = N;
            E.opposite = W;
            W.opposite = E;
        }

        private DIR(int bit, int dx, int dy) {
            this.bit = bit;
            this.dx = dx;
            this.dy = dy;
        }
    }

    public static class MazeCell {
        public boolean topWall = false, leftWall = false;
    }
}
