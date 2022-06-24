import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class Panel extends JPanel implements ActionListener {
    static final int WIDTH = 1200;
    static final int HEIGHT = 600;
    double time;
    int betweenTime;
    private double fov; //scaling factor is 1/tan(theta/2) for x and y
    //zfar/(zfar-znear), aka scaling factor for z
    //offset factor for z -(zfar*znear)/(zfar-znear)
    //x, y, z -> aspectRatio*x*scaleFactor/z, y*scaleFactor/z, zScale*z - (zfar*znear)/(zfar-znear)
    //x' = x/z and y' = y/z (inverse proportionality)
    //input vector [x, y, z, 1]
    //projection matrix (multiply with input vector) [[xCoeff, 0, 0, 0], [0, yCoeff, 0, 0], [0, 0, zCoeff, zDispl], [0, 0, 1, 0]]
    private final Mesh meshCube;
    private double[][] projectionMatrix; //multiply by the input point/vector to normalize it into the screen space!
    private Point camera;

    public Panel() throws Exception {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setLayout(null);
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(new GKeyAdapter());

        time = 0;
        betweenTime = 0;

        projectionMatrix = matProjection(90,(double) HEIGHT/WIDTH, 0.1, 1000);

        camera = new Point(0, 0, 0);

        meshCube = new Mesh(new ArrayList<Triangle>());
        meshCube.readObj("C:\\Users\\eydon\\IdeaProjects\\3DRendering\\src\\Cow.txt");
    }

    public Point multiplyVectMat (Point i, double[][] m) {
        //m is a 4x4 2d array
        double x = i.x*m[0][0] + i.y*m[1][0] + i.z*m[2][0] + m[3][0]; //remember fourth imaginary vector part is 1!
        double y = i.x*m[0][1] + i.y*m[1][1] + i.z*m[2][1] + m[3][1];
        double z = i.x*m[0][2] + i.y*m[1][2] + i.z*m[2][2] + m[3][2];
        double w = i.x*m[0][3] + i.y*m[1][3] + i.z*m[2][3] + m[3][3]; //should be z, so we can divide

        if (w != 0) {
            x /= w;
            y /= w;
            z /= w;
        }
        return new Point(x, y, z);
    }

    public double[][] matIdentity() {
        double[][] m = new double[4][4];
        m[0][0] = 1;
        m[1][1] = 1;
        m[2][2] = 1;
        m[3][3] = 1;
        return m;
    }
    public double[][] matRotX(double rad) {
        double[][] rotX = new double[4][4];
        rotX[0][0] = 1;
        rotX[1][1] = Math.cos(rad);
        rotX[1][2] = -1*Math.sin(rad);
        rotX[2][1] = Math.sin(rad);
        rotX[2][2] = Math.cos(rad);
        rotX[3][3] = 1;
        return rotX;
    }
    public double[][] matRotY(double rad) {
        double[][] rotY = new double[4][4];
        rotY[0][0] = Math.cos(rad);
        rotY[0][2] = Math.sin(rad);
        rotY[2][0] = -1*Math.sin(rad);
        rotY[1][1] = 1;
        rotY[2][2] = Math.cos(rad);
        rotY[3][3] = 1;
        return rotY;
    }
    public double[][] matRotZ(double rad) {
        double[][] rotZ = new double[4][4];
        rotZ[0][0] = Math.cos(rad);
        rotZ[0][1] = -1*Math.sin(rad);
        rotZ[1][0] = Math.sin(rad);
        rotZ[1][1] = Math.cos(rad);
        rotZ[2][2] = 1;
        rotZ[3][3] = 1;
        return rotZ;
    }
    public double[][] matTranslation(double x, double y, double z) {
        double[][] mat = new double[4][4];
        mat[0][0] = 1;
        mat[1][1] = 1;
        mat[2][2] = 1;
        mat[3][3] = 1;
        mat[3][0] = x;
        mat[3][1] = y;
        mat[3][2] = z;
        return mat;
    }
    public double[][] matProjection(double fovDeg, double aspectRatio, double zNear, double zFar) {
        double scaleFactor = 1/Math.tan(Math.toRadians(fovDeg)/2);
        projectionMatrix = new double[4][4];
        projectionMatrix[0][0] = aspectRatio*scaleFactor;
        projectionMatrix[1][1] = scaleFactor;
        projectionMatrix[2][2] = zFar/(zFar-zNear);
        projectionMatrix[3][2] = -1*(zFar*zNear)/(zFar-zNear);
        projectionMatrix[2][3] = 1;
        return projectionMatrix;
    }
    public double[][] multiplyMat(double[][] m1, double[][] m2) {
        double[][] mat = new double[4][4];
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                mat[r][c] = m1[r][0]*m2[0][c] + m1[r][1]*m2[1][c] + m1[r][2]*m2[2][c] + m1[r][3]*m2[3][c];
            }
        }
        return mat;
    }

    public Point addVec(Point p1, Point p2) {
        return new Point(p1.x + p2.x, p1.y + p2.y, p1.z + p2.z);
    }
    public Point subVec(Point p1, Point p2) {
        return new Point(p1.x - p2.x, p1.y - p2.y, p1.z - p2.z);
    }

    public Point multVec(Point p1, double p2) {
        return new Point(p1.x * p2, p1.y * p2, p1.z * p2);
    }
    public Point divVec(Point p1, double p2) {
        return new Point(p1.x / p2, p1.y / p2, p1.z / p2);
    }

    public double dotProduct(Point p1, Point p2) {
        return p1.x*p2.x + p1.y*p2.y + p1.z*p2.z;
    }
    public double vecLength(Point p1) {
        return Math.sqrt(p1.x*p1.x + p1.y*p1.y + p1.z*p1.z);
    }
    public Point vecNormalise(Point p1) {
        double l = vecLength(p1);
        return new Point(p1.x/l, p1.y/l, p1.z/l);
    }
    public Point crossProduct(Point p1, Point p2) {
        Point cp = new Point(0, 0, 0);
        cp.x = p1.y*p2.z - p1.z*p2.y;
        cp.y = p1.z*p2.x - p1.x*p2.z;
        cp.z = p1.x*p2.y - p1.y*p2.x;
        return cp;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        draw(g);
    }
    public void draw(Graphics g) {
        betweenTime ++;
        if (betweenTime >= 10) {
            betweenTime = 0;
            time++;
            if (time == Double.MAX_VALUE) {
                time = 0;
            }
        }

        int zSpeed = 25;
        int xSpeed = 30;

        ArrayList<Triangle> trisToDraw = new ArrayList<Triangle>();

        double[][] worldMat = multiplyMat(matRotZ(time/zSpeed), matRotX(time/xSpeed));
        worldMat = multiplyMat(worldMat, matTranslation(0, 0, 10));

        //draw triangles
        for (Triangle t : meshCube.tris) {

            Triangle tTransformed = new Triangle(multiplyVectMat(t.point1, worldMat),
                    multiplyVectMat(t.point2, worldMat), multiplyVectMat(t.point3, worldMat));

            //triangle culling
            Point normal, line1, line2; //actually 3D vectors but I'm too lazy to make a new class
            line1 = subVec(tTransformed.point2, tTransformed.point1);
            line2 = subVec(tTransformed.point3, tTransformed.point1);
            normal = vecNormalise(crossProduct(line1, line2));

            //if(true) {
            if (dotProduct(normal, subVec(tTransformed.point1, camera)) < 0) { //takes into account perspective w/ dot product
                //add lighting
                Point light_direction = vecNormalise(new Point(0, 0, -1)); //single direction, very simple because it's just a huge plane
                //emitting consistent rays of light which is great because I small brain also IntelliJ I'm sorry but you'll never be Grammarly
                //also used normalise because it isn't always gonna be simple like that

                double dp = Math.max(0.1, dotProduct(light_direction, normal));
                Color c = new Color((float)dp, (float)dp, (float)dp);

                //projecting it!
                Triangle tProjected = new Triangle(multiplyVectMat(tTransformed.point1, projectionMatrix),
                        multiplyVectMat(tTransformed.point2, projectionMatrix), multiplyVectMat(tTransformed.point3, projectionMatrix));

                //offset and scale
                Point addP = new Point(1,1, 0);
                tProjected.point1 = addVec(tProjected.point1, addP);
                tProjected.point2 = addVec(tProjected.point2, addP);
                tProjected.point3 = addVec(tProjected.point3, addP);

                tProjected.point1.x *= 0.5 * WIDTH;
                tProjected.point1.y *= 0.5 * HEIGHT;
                tProjected.point2.x *= 0.5 * WIDTH;
                tProjected.point2.y *= 0.5 * HEIGHT;
                tProjected.point3.x *= 0.5 * WIDTH;
                tProjected.point3.y *= 0.5 * HEIGHT;

                tProjected.c = c;
                trisToDraw.add(tProjected);
            }
        }

        Comparator<Triangle> compareByZ = new Comparator<Triangle>() {
            @Override
            public int compare(Triangle o1, Triangle o2) {
                double z1 = (o1.point1.z + o1.point2.z + o1.point3.z)/3;
                double z2 = (o2.point1.z + o2.point2.z + o2.point3.z)/3;
                if (z1-z2 < 0) {
                    return 1;
                }
                else if (z1-z2 == 0) {
                    return 0;
                }
                else {
                    return -1;
                }
            }
        };
        Collections.sort(trisToDraw, compareByZ);

        for (Triangle t : trisToDraw) {
            g.setColor(t.c);
            fillTriangle(g, t);
//            g.setColor(Color.WHITE);
//            drawTriangle(g, t);
        }
    }
    public void drawTriangle(Graphics g, Triangle t) {
        g.drawLine((int) t.point1.x, (int) t.point1.y, (int) t.point2.x, (int) t.point2.y);
        g.drawLine((int) t.point1.x, (int) t.point1.y, (int) t.point3.x, (int) t.point3.y);
        g.drawLine((int) t.point3.x, (int) t.point3.y, (int) t.point2.x, (int) t.point2.y);
    }
    public void fillTriangle(Graphics g, Triangle t) {
        int[] x = {(int) t.point1.x, (int) t.point2.x, (int) t.point3.x};
        int[] y = {(int) t.point1.y, (int) t.point2.y, (int) t.point3.y};
        Polygon p = new Polygon(x, y, 3);
        g.fillPolygon(p);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    public class GKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {

        }
        @Override
        public void keyReleased(KeyEvent e) {

        }
    }
    public class Point {
        double x, y, z;
        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
    public class Triangle {
        Point point1, point2, point3;
        Color c;
        public Triangle (Point p1, Point p2, Point p3) {
            point1 = p1;
            point2 = p2;
            point3 = p3;
            c = new Color(0, 0, 0);
        }
    }
    public class Mesh {
        ArrayList<Triangle> tris;
        public Mesh(ArrayList<Triangle> t) {
            tris = t;
        }

        public boolean readObj(String fileName) throws Exception {
            //try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));
                String s = in.readLine();
                ArrayList<Point> pool = new ArrayList<Point>();
                while (s != null) {
                    StringTokenizer str = new StringTokenizer(s);
                    String start = str.nextToken();
                    if (start.equals("v")) {
                        double x = Double.parseDouble(str.nextToken());
                        double y = Double.parseDouble(str.nextToken());
                        double z = Double.parseDouble(str.nextToken());
                        pool.add(new Point(x, y, z));
                    }
                    else if (start.equals("f")) { //assume is triangle or quad for sanity
                        int[] indices = new int[4];
                        int i = 0;
                        while (str.hasMoreTokens() && i < 4) {
                            indices[i] = Integer.parseInt(str.nextToken());
                            i++;
                        }
                        if (indices[3] == 0) {
                            tris.add(new Triangle(pool.get(indices[0]-1), pool.get(indices[1]-1), pool.get(indices[2]-1)));
                        }
                        else {
                            tris.add(new Triangle(pool.get(indices[0]-1), pool.get(indices[1]-1), pool.get(indices[2]-1)));
                            tris.add(new Triangle(pool.get(indices[0]-1), pool.get(indices[2]-1), pool.get(indices[3]-1)));
                        }
                    }
                    s = in.readLine();
                }
                in.close();
                return true;
            //}
            //catch (Exception e) {
                //System.out.println(":(");
                //return false;
            //}
        }
    }
}
