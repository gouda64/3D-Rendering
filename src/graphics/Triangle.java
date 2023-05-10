package graphics;

import java.awt.*;

public class Triangle {
    //public Point p1, p2, p3;
    public Point[] pts;
    public Color c = new Color(0, 0, 0);

    public Triangle(Point p1, Point p2, Point p3) {
        pts = new Point[]{p1, p2, p3};
    }

    public double avgZ() {
        double avg = 0;
        for (int i = 0; i < pts.length; i++) {
            avg += pts[i].z;
        }
        return avg/ pts.length;
    }

    public String toString() { //for debugging!
        return "p1 - " + pts[0].toString() +
                "\n p2 - " + pts[1].toString() +
                "\n p3 - " + pts[2].toString();
    }
}
