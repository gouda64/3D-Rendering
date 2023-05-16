import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import graphics.Camera;
import graphics.Rasterizer;
import graphics.Triangle;

public class Panel extends JPanel {
    static final int WIDTH = 1200;
    static final int HEIGHT = 600;

    private final Camera camera;

    private int mouseX;
    private int mouseY;
    private boolean firstMove;

    public Panel() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setLayout(null);
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(new GKeyAdapter());
        this.addMouseMotionListener(new GMouseAdapter());

        mouseX = 0;
        mouseY = 0;
        firstMove = true;

        camera = new Camera(WIDTH, HEIGHT, "./assets/cube.txt");
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        draw(g);
    }
    public void draw(Graphics g) {
        for (Triangle t : camera.view()) {
            //g.setColor(t.c);
            Rasterizer.drawTexTriangle(g, t, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            drawTriangle(g, t);
        }
    }

    public static void drawTriangle(Graphics g, Triangle t) {
        g.drawLine(WIDTH-(int) t.pts[0].x, HEIGHT-(int) t.pts[0].y, WIDTH-(int) t.pts[1].x, HEIGHT-(int) t.pts[1].y);
        g.drawLine(WIDTH-(int) t.pts[0].x, HEIGHT-(int) t.pts[0].y, WIDTH-(int) t.pts[2].x, HEIGHT-(int) t.pts[2].y);
        g.drawLine(WIDTH-(int) t.pts[2].x, HEIGHT-(int) t.pts[2].y, WIDTH-(int) t.pts[1].x, HEIGHT-(int) t.pts[1].y);
    }
    public static void fillTriangle(Graphics g, Triangle t) {
        int[] x = {WIDTH-(int) t.pts[0].x, WIDTH-(int) t.pts[1].x, WIDTH-(int) t.pts[2].x};
        int[] y = {HEIGHT-(int) t.pts[0].y, HEIGHT-(int) t.pts[1].y, HEIGHT-(int) t.pts[2].y};
        Polygon p = new Polygon(x, y, 3);
        g.fillPolygon(p);
    }

    public class GKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            final double movement = 0.3;
            final double rotation = 0.2;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE -> camera.moveY(movement);
                case KeyEvent.VK_SHIFT -> camera.moveY(-movement);
                case KeyEvent.VK_D -> camera.moveRightLeft(movement);
                case KeyEvent.VK_A -> camera.moveRightLeft(-movement);
                case KeyEvent.VK_W -> camera.moveForBack(movement);
                case KeyEvent.VK_S -> camera.moveForBack(-movement);
                case KeyEvent.VK_LEFT -> camera.turnRightLeft(-rotation);
                case KeyEvent.VK_RIGHT -> camera.turnRightLeft(rotation);
                //case KeyEvent.VK_UP -> camera.turnUpDown(rotation);
                //case KeyEvent.VK_DOWN -> camera.turnUpDown(-rotation);
            }
            repaint();
        }
        @Override
        public void keyReleased(KeyEvent e) {

        }
    }

    public class GMouseAdapter extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            if (!firstMove) {
                //camera.turnUpDown(-0.005*(e.getYOnScreen() - mouseY));
                camera.turnRightLeft(.005*(e.getXOnScreen() - mouseX));
            }
            else {
                firstMove = false;
            }
            mouseX = e.getXOnScreen();
            mouseY = e.getYOnScreen();

            repaint();
        }
    }
}
