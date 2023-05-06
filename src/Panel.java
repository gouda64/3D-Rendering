import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import graphics.Camera;
import graphics.Triangle;

public class Panel extends JPanel {
    static final int WIDTH = 1200;
    static final int HEIGHT = 600;

    private Camera camera;

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

        camera = new Camera(WIDTH, HEIGHT, "./assets/Mountains.txt");
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        draw(g);
    }
    public void draw(Graphics g) {
        for (Triangle t : camera.view()) {
            g.setColor(t.c);
            fillTriangle(g, t);
        }
    }
    public void drawTriangle(Graphics g, Triangle t) {
        g.drawLine(WIDTH-(int) t.p1.x, HEIGHT-(int) t.p1.y, WIDTH-(int) t.p2.x, HEIGHT-(int) t.p2.y);
        g.drawLine(WIDTH-(int) t.p1.x, HEIGHT-(int) t.p1.y, WIDTH-(int) t.p3.x, HEIGHT-(int) t.p3.y);
        g.drawLine(WIDTH-(int) t.p3.x, HEIGHT-(int) t.p3.y, WIDTH-(int) t.p2.x, HEIGHT-(int) t.p2.y);
    }
    public void fillTriangle(Graphics g, Triangle t) {
        int[] x = {WIDTH-(int) t.p1.x, WIDTH-(int) t.p2.x, WIDTH-(int) t.p3.x};
        int[] y = {HEIGHT-(int) t.p1.y, HEIGHT-(int) t.p2.y, HEIGHT-(int) t.p3.y};
        Polygon p = new Polygon(x, y, 3);
        g.fillPolygon(p);
    }

    public class GKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {

            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE -> camera.moveY(1);
                case KeyEvent.VK_SHIFT -> camera.moveY(-1);
                case KeyEvent.VK_D -> camera.moveRightLeft(1);
                case KeyEvent.VK_A -> camera.moveRightLeft(-1);
                case KeyEvent.VK_W -> camera.moveForBack(1);
                case KeyEvent.VK_S -> camera.moveForBack(-1);
                case KeyEvent.VK_LEFT -> camera.turnRightLeft(-0.2);
                case KeyEvent.VK_RIGHT -> camera.turnRightLeft(0.2);
                case KeyEvent.VK_UP -> camera.turnUpDown(0.2);
                case KeyEvent.VK_DOWN -> camera.turnUpDown(-0.2);
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
                camera.turnUpDown(-0.005*(e.getYOnScreen() - mouseY));
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
