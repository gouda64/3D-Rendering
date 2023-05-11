package graphics;

public class Point {
    //maybe make some constants sometime (like unit vecs)

    public double x, y, z; //semantic meaning over slight efficiency improvements fr

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        z = 0;
    }

    public Point add(Point p2) {
        return new Point(x + p2.x, y + p2.y, z + p2.z);
    }
    public Point sub(Point p2) {
        return new Point(x - p2.x, y - p2.y, z - p2.z);
    }
    public Point mult(double p2) {
        return new Point(x * p2, y * p2, z * p2);
    }
    public Point div(double p2) {
        return new Point(x / p2, y / p2, z / p2);
    }

    public double dotProduct(Point p2) {
        return x*p2.x + y*p2.y + z*p2.z;
    }
    public Point crossProduct(Point p2) {
        return new Point(y*p2.z - z*p2.y, z*p2.x - x*p2.z, x*p2.y - y*p2.x);
    }

    public double length() {
        return Math.sqrt(x* x + y* y + z* z);
    }
    public Point normalize() {
        double l = length();
        if (l != 0) {
            return new Point(x/l, y/l, z/l);
        }
        else {
            //NOT this bc people expect a new point back
            return new Point(x, y, z); //or null/error?
        }
    }

    public String toString() {
        return "x: " + x + " y: " + y + " z: " + z;
    }
}
