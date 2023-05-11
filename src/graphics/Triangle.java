package graphics;

import java.awt.*;

public class Triangle {
    //public Point p1, p2, p3;
    public Point[] pts;
    public Color c = new Color(0, 0, 0);
    public Point[] texPts;
    public String texFile;

    public Triangle(Point p1, Point p2, Point p3) {
        pts = new Point[]{p1, p2, p3};

        texPts = new Point[]{new Point(0, 0), new Point(0, 1),
                new Point(1, 0)};
        texFile = "./assets/rickroll.jpg";
    }
    public Triangle(Point p1, Point p2, Point p3, Point[] texPts, String texFile) {
        pts = new Point[]{p1, p2, p3};

        this.texPts = texPts;
        this.texFile = texFile;
    }

    public double avgZ() {
        double avg = 0;
        for (int i = 0; i < pts.length; i++) {
            avg += pts[i].z;
        }
        return avg / pts.length;
    }

    public String toString() {
        return "p1 - " + pts[0].toString() +
                "\n p2 - " + pts[1].toString() +
                "\n p3 - " + pts[2].toString();
    }
}
