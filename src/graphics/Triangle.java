package graphics;

import java.awt.*;

public class Triangle {
    //public Point p1, p2, p3;
    public Point[] pts;
    public Color c; //TODO: change color storage method?

    public Triangle(Point p1, Point p2, Point p3) {
//        this.p1 = p1;
//        this.p2 = p2;
//        this.p3 = p3;
        pts = new Point[]{p1, p2, p3};
        c = new Color(0, 0, 0);
    }

    public double avgZ() {
        double avg = 0;
        for (int i = 0; i < pts.length; i++) {
            avg += pts[i].z;
        }
        return avg/ pts.length;
    }

    public String toString() { //for debugging!
        return "p1 - x: " + pts[0].x + " y: " + pts[0].y + " z: " + pts[0].z +
                "\n p2 - x: " + pts[1].x + " y: " + pts[1].y + " z: " + pts[1].z +
                "\n p3 - x: " + pts[2].x + " y: " + pts[2].y + " z: " + pts[2].z;
    }
}
