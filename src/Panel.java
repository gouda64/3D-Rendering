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

        fov = 90;
        double zNear = 0.1;
        double zFar = 1000;
        double aspectRatio = (double) HEIGHT/WIDTH;
        double scaleFactor = 1/Math.tan(Math.toRadians(fov)/2);
        projectionMatrix = new double[4][4];
        projectionMatrix[0][0] = aspectRatio*scaleFactor;
        projectionMatrix[1][1] = scaleFactor;
        projectionMatrix[2][2] = zFar/(zFar-zNear);
        projectionMatrix[3][2] = -1*(zFar*zNear)/(zFar-zNear);
        projectionMatrix[2][3] = 1;

        camera = new Point(0, 0, 0);

//        ArrayList<Triangle> t = new ArrayList<Triangle>();
//        //always draw triangles in a clockwise fashion so that the normals aren't all wack
//
//        //South
//        t.add(new Triangle(new Point(0.0, 0.0, 0.0), new Point(0.0, 1.0, 0.0), new Point(1.0, 1.0, 0.0)));
//        t.add(new Triangle(new Point(0.0, 0.0, 0.0), new Point(1.0, 1.0, 0.0), new Point(1.0, 0.0, 0.0)));
//        //East
//        t.add(new Triangle(new Point(1.0, 0.0, 0.0), new Point(1.0, 1.0, 0.0), new Point(1.0, 1.0, 1.0)));
//        t.add(new Triangle(new Point(1.0, 0.0, 0.0), new Point(1.0, 1.0, 1.0), new Point(1.0, 0.0, 1.0)));
//        //North
//        t.add(new Triangle(new Point(1.0, 0.0, 1.0), new Point(1.0, 1.0, 1.0), new Point(0.0, 1.0, 1.0)));
//        t.add(new Triangle(new Point(1.0, 0.0, 1.0), new Point(0.0, 1.0, 1.0), new Point(0.0, 0.0, 1.0)));
//        //West
//        t.add(new Triangle(new Point(0.0, 0.0, 1.0), new Point(0.0, 1.0, 1.0), new Point(0.0, 1.0, 0.0)));
//        t.add(new Triangle(new Point(0.0, 0.0, 1.0), new Point(0.0, 1.0, 0.0), new Point(0.0, 0.0, 0.0)));
//        //Top
//        t.add(new Triangle(new Point(0.0, 1.0, 0.0), new Point(0.0, 1.0, 1.0), new Point(1.0, 1.0, 1.0)));
//        t.add(new Triangle(new Point(0.0, 1.0, 0.0), new Point(1.0, 1.0, 1.0), new Point(1.0, 1.0, 0.0)));
//        //Bottom
//        t.add(new Triangle(new Point(0.0, 0.0, 1.0), new Point(0.0, 0.0, 0.0), new Point(1.0, 0.0, 0.0)));
//        t.add(new Triangle(new Point(0.0, 0.0, 1.0), new Point(1.0, 0.0, 0.0), new Point(1.0, 0.0, 1.0)));
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
        //rotation z
        double[][] rotZ = new double[4][4]; //usually 3x3 but can extend to 4x4 easily
        rotZ[0][0] = Math.cos(time/zSpeed);
        rotZ[0][1] = -1*Math.sin(time/zSpeed);
        rotZ[1][0] = Math.sin(time/zSpeed);
        rotZ[1][1] = Math.cos(time/zSpeed);
        rotZ[2][2] = 1;
        rotZ[3][3] = 1;
        //rotation x
        int xSpeed = 30;
        double[][] rotX = new double[4][4];
        rotX[0][0] = 1;
        rotX[1][1] = Math.cos(time/xSpeed); //1/2 to make the rotation rates different - avoid gimbal lock :(
        rotX[1][2] = -1*Math.sin(time/xSpeed);
        rotX[2][1] = Math.sin(time/xSpeed);
        rotX[2][2] = Math.cos(time/xSpeed);
        rotX[3][3] = 1;

        ArrayList<Triangle> trisToDraw = new ArrayList<Triangle>();

        //draw triangles
        for (Triangle t : meshCube.tris) {
            //rotate the triangle!
            Triangle tZRot = new Triangle(multiplyVectMat(t.point1, rotZ),
                    multiplyVectMat(t.point2, rotZ), multiplyVectMat(t.point3, rotZ));
            Triangle tZXRot = new Triangle(multiplyVectMat(tZRot.point1, rotX),
                    multiplyVectMat(tZRot.point2, rotX), multiplyVectMat(tZRot.point3, rotX));

            //translating it!
            Triangle tTranslated = new Triangle(tZXRot.point1, tZXRot.point2, tZXRot.point3);
            tTranslated.point1.z += 10;
            tTranslated.point2.z += 10;
            tTranslated.point3.z += 10;

            //triangle culling
            Point normal, line1, line2; //actually 3D vectors but I'm too lazy to make a new class
            line1 = new Point(tTranslated.point2.x - tTranslated.point1.x, tTranslated.point2.y - tTranslated.point1.y, tTranslated.point2.z - tTranslated.point1.z);
            line2 = new Point(tTranslated.point3.x - tTranslated.point1.x, tTranslated.point3.y - tTranslated.point1.y, tTranslated.point3.z - tTranslated.point1.z);
            normal = new Point(line1.y*line2.z - line1.z*line2.y, line1.z*line2.x - line1.x*line2.z, line1.x*line2.y - line1.y*line2.x);
            double normal_l = Math.sqrt(normal.x*normal.x + normal.y*normal.y + normal.z*normal.z);
            normal.x /= normal_l;
            normal.y /= normal_l;
            normal.z /= normal_l;

            //if(true) {
            if (normal.x*(tTranslated.point1.x - camera.x) + normal.y*(tTranslated.point1.y - camera.y) +
                    normal.z*(tTranslated.point1.z - camera.z) < 0) { //takes into account perspective w/ dot product
                //add lighting
                Point light_direction = new Point(0, 0, -1); //single direction, very simple because it's just a huge plane
                //emitting consistent rays of light which is great because I small brain also IntelliJ I'm sorry but you'll never be Grammarly
                double light_l = Math.sqrt(light_direction.x*normal.x + light_direction.y*light_direction.y + light_direction.z*light_direction.z);
                light_direction.x /= light_l;
                light_direction.y /= light_l;
                light_direction.z /= light_l;
                double dp = light_direction.x*normal.x + light_direction.y*normal.y + light_direction.z*normal.z;
                if (dp < 0) {
                    dp = 0;
                }
                Color c = new Color((float)dp, (float)dp, (float)dp);

                //projecting it!
                Triangle tProjected = new Triangle(multiplyVectMat(tTranslated.point1, projectionMatrix),
                        multiplyVectMat(tTranslated.point2, projectionMatrix), multiplyVectMat(tTranslated.point3, projectionMatrix));
                //scale!
                tProjected.point1.x += 1;
                tProjected.point1.y += 1;
                tProjected.point2.x += 1;
                tProjected.point2.y += 1;
                tProjected.point3.x += 1;
                tProjected.point3.y += 1;

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
            //g.setColor(Color.WHITE);
            //drawTriangle(g, tProjected);
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
