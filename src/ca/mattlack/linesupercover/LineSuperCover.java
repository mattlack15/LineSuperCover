package ca.mattlack.linesupercover;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class LineSuperCover {

    JFrame frame = new JFrame("Line Super-cover");

    private volatile int mouseX = 0;
    private volatile int mouseY = 0;

    private volatile int startX = 0;
    private volatile int startY = 0;

    public static void main(String[] args) throws InterruptedException {
        new LineSuperCover();
    }

    public LineSuperCover() throws InterruptedException {
        frame.setVisible(true);
        frame.setSize(640, 480);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();
            }
        });

        frame.createBufferStrategy(2);

        while (true) {

            Graphics2D g2d = (Graphics2D) frame.getBufferStrategy().getDrawGraphics();

            render(g2d, frame.getWidth(), frame.getHeight());

            frame.getBufferStrategy().show();
            g2d.dispose();

            Thread.sleep(30);
        }
    }

    public void render(Graphics2D g, int width, int height) {
        // Draw white background.
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Draw grid.
        int cellSize = 20;
        int gridWidth = width / cellSize;
        int gridHeight = height / cellSize;

        g.setColor(Color.GRAY);
        for (int i = 0; i < gridWidth; i++) {
            g.drawLine(i * cellSize, 0, i * cellSize, height);
        }
        for (int i = 0; i < gridHeight; i++) {
            g.drawLine(0, i * cellSize, width, i * cellSize);
        }

        // Make a supercover from (0, 0) to the mouse position.
        long ns = System.nanoTime();
        int[][] supercover = lineSupercover(startX, startY, mouseX, mouseY, cellSize);
        ns = System.nanoTime() - ns;


        // Draw in all the cells as blue.
        g.setColor(Color.BLUE);
        for (int[] cell : supercover) {
            g.fillRect(cell[0], cell[1], cellSize, cellSize);
        }

        // Draw the line.
        g.setColor(Color.RED);
        g.drawLine(startX, startY, mouseX, mouseY);
    }

    public static int[][] lineSupercover(double p1x, double p1y, double p2x, double p2y, double squareSize) {
        int[][] result = lineSupercover2(p1x / squareSize, p1y / squareSize, p2x / squareSize, p2y / squareSize);
        for (int[] ints : result) {
            for (int i = 0; i < ints.length; i++) {
                ints[i] *= squareSize;
            }
        }
        return result;
    }

    public static int[][] lineSupercover(double p1x, double p1y, double p2x, double p2y) {

        List<int[]> points = new ArrayList<>();

        // Make sure we go from left to right.
        double startX, startY, endX, endY;

        if (p1x < p2x) {
            startX = p1x;
            startY = p1y;
            endX = p2x;
            endY = p2y;
        } else {
            startX = p2x;
            startY = p2y;
            endX = p1x;
            endY = p1y;
        }

        // Now the start variables are farther left (-x) and the end variables are farther right (+x).

        // Make some variables to store the floored versions of the start and finishes.
        int sqStartX = (int) startX;
        int sqStartY = (int) startY;
        int sqEndX = (int) endX;
        int sqEndY = (int) endY;

        // Find the step direction we need to go in horizontally and vertically.
        // stepX will always be positive or 0 (>=0) since above we made sure
        // that we were going from left to right. stepY can be negative.
        int stepX = Integer.compare(sqEndX - sqStartX, 0);
        int stepY = Integer.compare(sqEndY - sqStartY, 0);

        // Find the slope of the line we are supercovering.
        double slope = (endY - startY) / (endX - startX);

        // Our line will have a slope that generally goes towards one corner of a square it passes through.
        // That corner is always on the right side of the square since once again we are moving from left to right.
        // We must find whether it is the top or bottom corner.
        // If the slope is greater than 1, then it is the top corner, else it is the bottom one.
        int cornerOffsetY = slope > 0 ? 1 : 0;

        // Make variables to keep track of the current position.
        int currX = sqStartX, currY = sqStartY;

        while (currX < sqEndX || (sqEndY - sqStartY > 0 ? currY < sqEndY : currY > sqEndY)) {

            // Add the current point. (The last point will be added after the loop)
            points.add(new int[]{currX, currY});

            // Get the slope from our line's starting point to the corresponding corner that the line's slope is aimed at.
            int cornerX = currX + 1;
            int cornerY = currY + cornerOffsetY;
            double referenceSlope = (cornerY - startY) / (cornerX - startX);

            boolean horizontalStep, verticalStep;

            // Depending on how the line's slope compares to the slope of the line from the start to the corner,
            // we can deduce what type of step(s) we must take.
            if (slope > referenceSlope) {
                // Slope is greater (above) the reference slope, so if
                // the reference corner is the top corner, then it's a vertical step,
                // otherwise it's a horizontal step.
                horizontalStep = cornerOffsetY == 0;
                verticalStep = !horizontalStep;
            } else if (slope < referenceSlope) {
                // Slope is less (below) the reference slope, so if
                // the reference corner is the bottom corner, then it's a vertical step,
                // otherwise it's a horizontal step.
                horizontalStep = cornerOffsetY == 1;
                verticalStep = !horizontalStep;
            } else {
                // Line slope exactly matches the slope to the corner, therefore this will be both
                // a horizontal AND a vertical step.
                horizontalStep = true;
                verticalStep = true;
            }

            // Perform the steps.
            if (horizontalStep) {
                currX += stepX;
            }

            if (verticalStep) {
                currY += stepY;
            }

        }

        // Add the last point.
        points.add(new int[]{currX, currY});

        return points.toArray(new int[0][]);
    }

    public static int[][] lineSupercover2(double p1x, double p1y, double p2x, double p2y) {
        List<int[]> points = new ArrayList<>();

        // Make sure we go from left to right.
        double startX, startY, endX, endY;

        if (p1x < p2x) {
            startX = p1x;
            startY = p1y;
            endX = p2x;
            endY = p2y;
        } else {
            startX = p2x;
            startY = p2y;
            endX = p1x;
            endY = p1y;
        }

        // Calculate the slope.
        double slope = (endY - startY) / (endX - startX);

        // Rounded variables.
        int sqStartX = (int) startX;
        int sqStartY = (int) startY;
        int sqEndX = (int) endX;
        int sqEndY = (int) endY;

        // Calculate y-step, whether it's positive or negative.
        int stepY = Integer.compare(sqEndY - sqStartY, 0);

        double currX = startX, currY = startY;

        // These next variables will hold the "target" x and y values while the current x and y values are
        // incremented iteratively until they are within the same square as the nextX or nextY. Then,
        // the current x and y values will be set to the next ones.
        double nextY = currY;
        double nextX = currX;

        while (currX < sqEndX || (sqEndY - sqStartY > 0 ? (int) currY < sqEndY : (int) currY > sqEndY)) {

            // Boolean to store whether or not to take another horizontal step.
            boolean nextHorizontalStep = false;

            // If we've not reached our vertical goal, then keep stepping vertically.
            // If we have reached our vertical goal, then assign the next variables to
            // the current ones, and take another horizontal step.
            if ((int) currY != (int) nextY) {
                currY += stepY;
            } else {
                currY = nextY;
                // Finished moving vertically, move horizontally again.
                nextHorizontalStep = true;

                currX = nextX;
            }

            // Add the current square.
            points.add(new int[]{(int) currX, (int) currY});

            if (nextHorizontalStep) {

                // Get the horizontal distance to the next square.
                double horizontalDistanceToNextSquare = ((int) currX + 1) - currX;

                // The next Y will be the current Y + how far up or down the line goes when looking
                // ahead by horizontalDistanceToNextSquare.
                nextY = currY + slope * horizontalDistanceToNextSquare;

                // The next X is just the next square's border, ie, currX + horizontalDistanceToNextSquare.
                nextX = currX + horizontalDistanceToNextSquare;
            }

        }

        if (points.isEmpty()) {
            // Add the last point.
            points.add(new int[]{(int) currX, (int) currY});
        }

        return points.toArray(new int[0][]);
    }
}

