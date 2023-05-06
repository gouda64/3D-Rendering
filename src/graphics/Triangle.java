package graphics;

import java.awt.*;

public class Triangle {
    public Point p1, p2, p3;
    public Color c; //TODO: change color storage method?

    public Triangle(Point p1, Point p2, Point p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        c = new Color(0, 0, 0);
    }

    public String toString() { //for debugging!
        return "p1 - x: " + p1.x + " y: " + p1.y + " z: " + p1.z +
                "\n p2 - x: " + p2.x + " y: " + p2.y + " z: " + p2.z +
                "\n p3 - x: " + p3.x + " y: " + p3.y + " z: " + p3.z;
    }
}
