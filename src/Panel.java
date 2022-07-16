import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.io.*;

public class Panel extends JPanel {
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
    private Point lookDir;
    private double yaw; //basically left-right rotation
    private double pitch; //up-down rotations
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

        time = 0;
        betweenTime = 0;

        fov = 90;
        projectionMatrix = matProjection(fov,(double) HEIGHT/WIDTH, 0.5, 1000);

        camera = new Point(0, 0, 0);
        lookDir = new Point(0, 0, 1);
        yaw = 0;
        pitch = 0;

        mouseX = 0;
        mouseY = 0;
        firstMove = true;

        //ArrayList<Triangle> t = new ArrayList<Triangle>();
        //t.add(new Triangle(new Point(10, 10, 10), new Point(20, 20, 10), new Point(30, 10, 10)));
        //meshCube = new Mesh(t);
        // for debugging (literally one triangle)

        ArrayList<Triangle> t = new ArrayList<>();
        //always draw triangles in a clockwise fashion so that the normals aren't all wack

        //South
        t.add(new Triangle(new Point(0.0, 0.0, 0.0), new Point(0.0, 1.0, 0.0), new Point(1.0, 1.0, 0.0)));
        t.add(new Triangle(new Point(0.0, 0.0, 0.0), new Point(1.0, 1.0, 0.0), new Point(1.0, 0.0, 0.0)));
        //East
        t.add(new Triangle(new Point(1.0, 0.0, 0.0), new Point(1.0, 1.0, 0.0), new Point(1.0, 1.0, 1.0)));
        t.add(new Triangle(new Point(1.0, 0.0, 0.0), new Point(1.0, 1.0, 1.0), new Point(1.0, 0.0, 1.0)));
        //North
        t.add(new Triangle(new Point(1.0, 0.0, 1.0), new Point(1.0, 1.0, 1.0), new Point(0.0, 1.0, 1.0)));
        t.add(new Triangle(new Point(1.0, 0.0, 1.0), new Point(0.0, 1.0, 1.0), new Point(0.0, 0.0, 1.0)));
        //West
        t.add(new Triangle(new Point(0.0, 0.0, 1.0), new Point(0.0, 1.0, 1.0), new Point(0.0, 1.0, 0.0)));
        t.add(new Triangle(new Point(0.0, 0.0, 1.0), new Point(0.0, 1.0, 0.0), new Point(0.0, 0.0, 0.0)));
        //Top
        t.add(new Triangle(new Point(0.0, 1.0, 0.0), new Point(0.0, 1.0, 1.0), new Point(1.0, 1.0, 1.0)));
        t.add(new Triangle(new Point(0.0, 1.0, 0.0), new Point(1.0, 1.0, 1.0), new Point(1.0, 1.0, 0.0)));
        //Bottom
        t.add(new Triangle(new Point(0.0, 0.0, 1.0), new Point(0.0, 0.0, 0.0), new Point(1.0, 0.0, 0.0)));
        t.add(new Triangle(new Point(0.0, 0.0, 1.0), new Point(1.0, 0.0, 0.0), new Point(1.0, 0.0, 1.0)));

        //each face will just have the entire texture thing
        for (Triangle tri : t) {
            tri.tPoints[0] = new TexPoint(0, 1);
            tri.tPoints[1] = new TexPoint(0, 0);
            tri.tPoints[2] = new TexPoint(1, 0);
        }

        //for testing (literally one cube)
        meshCube = new Mesh(t);

        //meshCube = new Mesh(new ArrayList<Triangle>());
        //meshCube.readObj("./src/Axis.txt");
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
        if (l != 0) {
            return new Point(p1.x/l, p1.y/l, p1.z/l);
        }
        else {
            return p1;
        }
    }
    public Point crossProduct(Point p1, Point p2) {
        Point cp = new Point(0, 0, 0);
        cp.x = p1.y*p2.z - p1.z*p2.y;
        cp.y = p1.z*p2.x - p1.x*p2.z;
        cp.z = p1.x*p2.y - p1.y*p2.x;
        return cp;
    }

    public Point pointIntersectPlane(Point pPoint, Point pNormal, Point lStart, Point lEnd) {
        pNormal = vecNormalise(pNormal);
        double t = (dotProduct(pNormal, pPoint) - dotProduct(lStart, pNormal)) / (dotProduct(lEnd, pNormal) - dotProduct(lStart, pNormal));
        //just something I found on stack overflow idk how it does it either
        return addVec(lStart, multVec(subVec(lEnd, lStart), t));
    }

    public Triangle[] triClipToPlane(Point pPoint, Point pNormal, Triangle t) {
        pNormal = vecNormalise(pNormal);
        //signed shortest distance from point to plane
        Point temp = t.point1;
        //DON'T normalize temp messes everything up I know from the week I spent debugging this thing
        double dist1 = dotProduct(pNormal, temp) - dotProduct(pNormal, pPoint);
        temp = t.point2;
        double dist2 = dotProduct(pNormal, temp) - dotProduct(pNormal, pPoint);
        temp = t.point3;
        double dist3 = dotProduct(pNormal, temp)- dotProduct(pNormal, pPoint);
        Point[] inside = new Point[3]; int inNum = 0;
        Point[] outside = new Point[3]; int outNum = 0;
        TexPoint[] insideTex = new TexPoint[3];
        TexPoint[] outsideTex = new TexPoint[3];

        if (dist1 >= 0) {
            inside[inNum] = t.point1;
            insideTex[inNum] = t.tPoints[0];
            inNum++;
        }
        else {
            outside[outNum] = t.point1;
            outsideTex[outNum] = t.tPoints[0];
            outNum++;
        }
        if (dist2 >= 0) {
            inside[inNum] = t.point2;
            insideTex[inNum] = t.tPoints[1];
            inNum++;
        }
        else {
            outside[outNum] = t.point2;
            outsideTex[outNum] = t.tPoints[1];
            outNum++;
        }
        if (dist3 >= 0) {
            inside[inNum] = t.point3;
            insideTex[inNum] = t.tPoints[2];
            inNum++;
        }
        else {
            outside[outNum] = t.point3;
            outsideTex[outNum] = t.tPoints[2];
            outNum++;
        }

        if (inNum == 0) {
            return new Triangle[] {};
        }
        else if (inNum == 3) {
            return new Triangle[] {t};
        }
        else if (inNum == 1) {
            Triangle newT = new Triangle(null, null, null);
            newT.point1 = inside[0]; newT.tPoints[0] = insideTex[0];
            newT.point2 = pointIntersectPlane(pPoint, pNormal, inside[0], outside[0]);
            newT.point3 = pointIntersectPlane(pPoint, pNormal, inside[0], outside[1]);

            double t1 = (dotProduct(pNormal, pPoint) - dotProduct(inside[0], pNormal)) / (dotProduct(outside[0], pNormal) - dotProduct(inside[0], pNormal));
            //the same thing as inside pointIntersectPlane but it's too annoying to try to access it from outside
            newT.tPoints[1].u = t1*(outsideTex[0].u - insideTex[0].u) + insideTex[0].u;
            newT.tPoints[1].v = t1*(outsideTex[0].v - insideTex[0].v) + insideTex[0].v;

            t1 = (dotProduct(pNormal, pPoint) - dotProduct(inside[0], pNormal)) / (dotProduct(outside[1], pNormal) - dotProduct(inside[0], pNormal));
            newT.tPoints[2].u = t1*(outsideTex[1].u - insideTex[0].u) + insideTex[0].u;
            newT.tPoints[2].v = t1*(outsideTex[1].v - insideTex[0].v) + insideTex[0].v;

            newT.c = t.c; //you can set this and the two below to different colors for a nice demonstration of clipping

            return new Triangle[] {newT};
        }
        else {
            Triangle newT1 = new Triangle(null, null, null);
            Triangle newT2 = new Triangle(null, null, null);

            newT1.point1 = inside[0];
            newT1.point2 = inside[1];
            newT1.point3 = pointIntersectPlane(pPoint, pNormal, inside[0], outside[0]);

            newT1.tPoints[0] = insideTex[0];
            newT1.tPoints[1] = insideTex[1];
            double t1 = (dotProduct(pNormal, pPoint) - dotProduct(inside[0], pNormal)) / (dotProduct(outside[0], pNormal) - dotProduct(inside[0], pNormal));
            newT1.tPoints[2].u = t1*(outsideTex[0].u - insideTex[0].u) + insideTex[0].u;
            newT1.tPoints[2].v = t1*(outsideTex[0].v - insideTex[0].v) + insideTex[0].v;

            newT2.point1 = inside[1];
            newT2.point2 = pointIntersectPlane(pPoint, pNormal, inside[0], outside[0]);
            newT2.point3 = pointIntersectPlane(pPoint, pNormal, inside[1], outside[0]);

            newT2.tPoints[0] = insideTex[0];
            newT2.tPoints[1] = insideTex[1];
            t1 = (dotProduct(pNormal, pPoint) - dotProduct(inside[1], pNormal)) / (dotProduct(outside[0], pNormal) - dotProduct(inside[1], pNormal));
            newT2.tPoints[2].u = t1*(outsideTex[0].u - insideTex[1].u) + insideTex[1].u;
            newT2.tPoints[2].v = t1*(outsideTex[0].v - insideTex[1].v) + insideTex[1].v;

            newT1.c = t.c;
            newT2.c = t.c;

            return new Triangle[] {newT1, newT2};
        }
    }

    public double[][][] pointAt(Point pos, Point target, Point up) { //camera info!!
        Point newForward = vecNormalise(subVec(target, pos));
        Point newUp = vecNormalise(subVec(up, multVec(newForward, dotProduct(up, newForward)))); //how much does newForward affect up?
        //just visualize the things graphically and it works out, if my linear-algebra-averse brain can do it, you can definitely do it!
        Point newRight = crossProduct(newUp, newForward);

        double[][] mat = new double[4][4]; //transformation matrix for looking at stuff
        mat[0][0] = newRight.x; mat[0][1] = newRight.y; mat[0][2] = newRight.z;
        mat[1][0] = newUp.x; mat[1][1] = newUp.y; mat[1][2] = newUp.z;
        mat[2][0] = newForward.x; mat[2][1] = newForward.y; mat[2][2] = newForward.z;
        mat[3][0] = pos.x; mat[3][1] = pos.y; mat[3][2] = pos.z; mat[3][3] = 1;

        double[][] inverseMat = new double[4][4];
        inverseMat[0][0] = newRight.x; inverseMat[1][0] = newRight.y; inverseMat[2][0] = newRight.z;
        inverseMat[0][1] = newUp.x; inverseMat[1][1] = newUp.y; inverseMat[2][1] = newUp.z;
        inverseMat[0][2] = newForward.x; inverseMat[1][2] = newForward.y; inverseMat[2][2] = newForward.z;
        inverseMat[3][0] = -dotProduct(pos, newRight); inverseMat[3][1] = -dotProduct(pos, newUp); inverseMat[3][2] = -dotProduct(pos, newForward); inverseMat[3][3] = 1;

        return new double[][][] {mat, inverseMat};
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        draw(g);
    }
    public void draw(Graphics g) {
        //betweenTime ++;
        if (betweenTime >= 10) {
            betweenTime = 0;
            time++;
            if (time == Double.MAX_VALUE) {
                time = 0;
            }
            repaint();
        }

        int zSpeed = 25;
        int xSpeed = 30;

        ArrayList<Triangle> trisToDraw = new ArrayList<>();

        double[][] worldMat = multiplyMat(matRotZ(time/zSpeed), matRotX(time/xSpeed));
        worldMat = multiplyMat(worldMat, matTranslation(0, 0, 10));

        lookDir = multiplyVectMat(new Point(0, 0, 1), multiplyMat(matRotX(pitch), matRotY(yaw)));
        double[][] camMat = pointAt(camera, addVec(camera, lookDir), new Point(0, 1, 0))[1];

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
                //emitting consistent rays of light which is great because it's easy

                double dp = Math.max(0.25, dotProduct(light_direction, normal));
                Color c = new Color((float)dp, (float)dp, (float)dp);

                //converting world space to view space
                Triangle tView = new Triangle(multiplyVectMat(tTransformed.point1, camMat),
                        multiplyVectMat(tTransformed.point2, camMat), multiplyVectMat(tTransformed.point3, camMat));
                tView.tPoints[0] = t.tPoints[0];
                tView.tPoints[1] = t.tPoints[1];
                tView.tPoints[2] = t.tPoints[2];

                Triangle[] clippedTris = triClipToPlane(new Point(0, 0, 0.2), new Point(0, 0, 1), tView);
                for (Triangle tris : clippedTris) {

                    Triangle tProjected = new Triangle(multiplyVectMat(tris.point1, projectionMatrix),
                            multiplyVectMat(tris.point2, projectionMatrix), multiplyVectMat(tris.point3, projectionMatrix));

                    //in the case of JPanel, x and y are inverted so...
                    //note that I didn't use multVec in this case because I didn't want to affect z
                    tProjected.point1.x *= -1;
                    tProjected.point1.y *= -1;
                    tProjected.point2.x *= -1;
                    tProjected.point2.y *= -1;
                    tProjected.point3.x *= -1;
                    tProjected.point3.y *= -1;

                    //offset and scale
                    Point addP = new Point(1, 1, 0);
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
                    tProjected.tPoints = tris.tPoints;

                    trisToDraw.add(tProjected);
                }
            }
        }

        Comparator<Triangle> compareByZ = (o1, o2) -> {
            double z1 = (o1.point1.z + o1.point2.z + o1.point3.z)/3;
            double z2 = (o2.point1.z + o2.point2.z + o2.point3.z)/3;
            if (z1-z2 < 0) {
                return 1;
            }
            if (z1-z2 == 0) {
                return 0;
            }
            if (z1-z2 > 0){
                return -1;
            }
            return 0;
        };
        trisToDraw.sort(compareByZ); //fun fact: although Collections.sort merely calls list.sort, it is still in use because Java places emphasis
        //on backwards compatibility so that's why it's still around, but it's more efficient to call list.sort just to avoid the extra method call

        for (Triangle t : trisToDraw) {
            //clip triangles against screen edges
            ArrayList<Triangle> trisToClip = new ArrayList<>();
            trisToClip.add(t);

            int newTris = 1;
            for (int i = 0; i < 4; i++) {

                while (newTris > 0) {
                    Triangle test = trisToClip.remove(0);
                    newTris--;
                    Triangle[] clippedTris = switch (i) {
                        case 0 -> //top
                                triClipToPlane(new Point(0, 0, 0), new Point(0, 1, 0), test);
                        case 1 -> //bottom
                                triClipToPlane(new Point(0, HEIGHT - 1, 0), new Point(0, -1, 0), test);
                        case 2 -> //left
                                triClipToPlane(new Point(0, 0, 0), new Point(1, 0, 0), test);
                        case 3 -> //right
                                triClipToPlane(new Point(WIDTH - 1, 0, 0), new Point(-1, 0, 0), test);
                        default -> new Triangle[1];
                    };

                    trisToClip.addAll(Arrays.asList(clippedTris));
                }
                newTris = trisToClip.size();
            }

            //just to clarify it's called trisToClip but they're done clipping at this point
            for (Triangle r : trisToClip) {
//                for (TexPoint p : r.tPoints) {
//                    System.out.println("u: " + p.u + " v: " + p.v);
//                }

                TexTriangle texT = new TexTriangle((int) r.point1.x, (int) r.point1.y, r.tPoints[0].u, r.tPoints[0].v,
                        (int) r.point2.x, (int) r.point2.y, r.tPoints[1].u, r.tPoints[1].v,
                        (int) r.point3.x, (int) r.point3.y, r.tPoints[2].u, r.tPoints[2].v);
                try {
                    texT.drawTexTri(g, ImageIO.read(new File("./src/rsz_cheese.jpg")));
                }
                catch (IOException e) {
                    System.out.println("image read failed");
                }

//                g.setColor(r.c);
//                fillTriangle(g, r);
                g.setColor(Color.WHITE);
                drawTriangle(g, r);

            }
        }
    }
    public void drawTriangle(Graphics g, Triangle t) {
        g.drawLine((int) t.point1.x, (int) t.point1.y, (int) t.point2.x, (int) t.point2.y);
        g.drawLine((int) t.point1.x, (int) t.point1.y, (int) t.point3.x, (int) t.point3.y);
        g.drawLine((int) t.point3.x, (int) t.point3.y, (int) t.point2.x, (int) t.point2.y);
    }
    public void fillTriangle(Graphics g, Triangle t) {
        int[] x = {WIDTH-(int) t.point1.x, WIDTH-(int) t.point2.x, WIDTH-(int) t.point3.x};
        int[] y = {HEIGHT-(int) t.point1.y, HEIGHT-(int) t.point2.y, HEIGHT-(int) t.point3.y};
        Polygon p = new Polygon(x, y, 3);
        g.fillPolygon(p);
    }

    public class GKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {

            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE -> camera.y += 1;
                case KeyEvent.VK_SHIFT -> camera.y -= 1;
                case KeyEvent.VK_D -> camera = addVec(camera, multVec(crossProduct(lookDir, new Point(0, 1, 0)), 2));
                case KeyEvent.VK_A -> camera = addVec(camera, multVec(crossProduct(lookDir, new Point(0, 1, 0)), -2));
                case KeyEvent.VK_W -> camera = addVec(camera, multVec(new Point(lookDir.x, 0, lookDir.z), 2));
                case KeyEvent.VK_S -> camera = subVec(camera, multVec(new Point(lookDir.x, 0, lookDir.z), 2));
                case KeyEvent.VK_LEFT -> yaw -= 0.1;
                case KeyEvent.VK_RIGHT -> yaw += 0.1;
                case KeyEvent.VK_UP -> pitch += 0.1;
                case KeyEvent.VK_DOWN -> pitch -= 0.1;
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
                pitch -= 0.005*(e.getYOnScreen() - mouseY);
                yaw += 0.005*(e.getXOnScreen() - mouseX);
            }
            else {
                firstMove = false;
            }
            mouseX = e.getXOnScreen();
            mouseY = e.getYOnScreen();

            repaint();
        }
    }

    public class Point {
        double x, y, z;
        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        //I've been told that a w component is necessary to distinguish from a point and a vector
        //and if you don't want it, you have to make two different sets of functions so...
        //whoops, this is definitely sloppy code
        //that said, for now I'm just keeping track of what's supposed to be a point
        //and what's supposed to be a vector manually, so for something small like this
        //it should be fine for the most part
    }
    public class TexPoint {
        double u;
        double v;
        public TexPoint(double u, double v) {
            this.u = u;
            this.v = v;
        }
    }
    public class Triangle {
        Point point1, point2, point3;
        TexPoint[] tPoints;
        Color c;
        public Triangle (Point p1, Point p2, Point p3) {
            point1 = p1;
            point2 = p2;
            point3 = p3;
            c = new Color(0, 0, 0);

            tPoints = new TexPoint[]{new TexPoint(0, 0), new TexPoint(0, 0), new TexPoint(0, 0)};
        }

        public String toString() { //for debugging!
            return "p1 - x: " + point1.x + " y: " + point1.y + " z: " + point1.z +
                    "\n p2 - x: " + point2.x + " y: " + point2.y + " z: " + point2.z +
                    "\n p3 - x: " + point3.x + " y: " + point3.y + " z: " + point3.z;
        }
    }
    public class TexTriangle {
        int x1, y1, x2, y2, x3, y3;
        double u1, v1, u2, v2, u3, v3;
        public TexTriangle (int x1, int y1, double u1, double v1,
                           int x2, int y2, double u2, double v2,
                           int x3, int y3, double u3, double v3) {
            //int and no z because this is after projection and all that but texPoints are still double

            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;

            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
            this.u3 = u3;
            this.v3 = v3;

            init();
        }
        public void init() {
            int temp1;
            double temp2;
            if (y2 < y1) {
                temp1 = x1;
                x1 = x2;
                x2 = temp1;
                temp1 = y1;
                y1 = y2;
                y2 = temp1;
                temp2 = u1;
                u1 = u2;
                u2 = temp2;
                temp2 = v1;
                v1 = v2;
                v2 = temp2;
            }
            if (y3 < y1) {
                temp1 = x1;
                x1 = x3;
                x3 = temp1;
                temp1 = y1;
                y1 = y3;
                y3 = temp1;
                temp2 = u1;
                u1 = u3;
                u3 = temp2;
                temp2 = v1;
                v1 = v3;
                v3 = temp2;
            }
            if (y3 < y2) {
                temp1 = x3;
                x3 = x2;
                x2 = temp1;
                temp1 = y3;
                y3 = y2;
                y2 = temp1;
                temp2 = u3;
                u3 = u2;
                u2 = temp2;
                temp2 = v3;
                v3 = v2;
                v2 = temp2;
            } //basically orders them by y value in increasing order
        } //this whole function could actually be placed in the initialized but I made a mistake and I'm too lazy to go through all that again

        public void drawTexTri(Graphics g, BufferedImage bi) {
            //the number of variables created in this one class is honestly frightening
            //I would like there to be less but the absolute unreadability would be even more frightening

            int dx1 = x2 - x1;
            int dy1 = y2 - y1;
            double du1 = u2 - u1;
            double dv1 = v2 - v1;

            int dx2 = x3 - x1;
            int dy2 = y3 - y1;
            double du2 = u3 - u1;
            double dv2 = v3 - v1;

            double dStartXStep = 0; double dEndXStep = 0;
            double dU1Step = 0; double dV1Step = 0; double dU2Step = 0; double dV2Step = 0;
            if (dy1 != 0) {
                dStartXStep = (double)dx1/Math.abs(dy1);
                dU1Step = du1/Math.abs(dy1);
                dV1Step = dv1/Math.abs(dy1);
            }
            if (dy2 != 0) {
                dEndXStep = (double)dx2/Math.abs(dy2);
                dU2Step = du2 /Math.abs(dy2);
                dV2Step = dv2 /Math.abs(dy2);
            }

            if (dy1 != 0) { //if the line is not flat, draw the triangle from the top to the point in between
                for (int i = y1; i <= y2; i++) {
                    int startX = x1 + (int) ((i-y1)*dStartXStep);
                    int endX = x1 + (int) ((i-y1)*dEndXStep);

                    double startU = u1 + (int) ((i-y1)*dU1Step);
                    double startV = v1 + (int) ((i-y1)*dV1Step);
                    double endU = u1 + (int) ((i-y1)*dU2Step);
                    double endV = v1 + (int) ((i-y1)*dV2Step);

                    //need to sort along x
                    if (startX > endX) {
                        int temp1 = startX;
                        startX = endX;
                        endX = temp1;
                        double temp2 = startU;
                        startU = endU;
                        endU = temp2;
                        temp2 = startV;
                        startV = endV;
                        endV = temp2;
                    }

                    double u;
                    double v;
                    double tStep = 1.0/(endX-startX);
                    double t = 0;
                    for (int j = startX; j < endX; j++) {
                        u = (1-t)*startU + t*endU; //linear interpolation, weighted average version
                        v = (1-t)*startV + t*endV;

                        int finU = Math.max(Math.min((int)(u*bi.getWidth()), bi.getWidth()-1), 0);
                        int finV = Math.max(Math.min((int)(v*bi.getHeight()), bi.getHeight()-1), 0);
                        int color = bi.getRGB(finU, finV);
                        System.out.println("u: " + u);
                        System.out.println("v: " + v);
                        if (u == 0 && v == 0) {
                            System.out.println(startU + " " + endU);
                            System.out.println(startV + " " + endV);
                        }
                        else {
                            System.out.println(finU);
                            System.out.println(finV);

                            //the getRGB function returns the RGB values bitwise so we need to convert them
                            int blue = color & 0xff; //basically isolates the blue value (rightmost 8)
                            // the 0xff is a hexadecimal number that represents a bunch of 1s and the and gets rid of the stuff we don't need
                            int green = (color & 0xff00) >> 8; //then gets the red value (the 8 bits after blue value)
                            //the extra 0s after the hexadecimal shift it 8 left so we can get the red values, and then the >> shifts the number back
                            int red = (color & 0xff0000) >> 16; //same thing as before but with 16
                            g.setColor(new Color(red, green, blue));
                            g.drawLine(j, i, j, i);

                            t += tStep;
                        }

                    }

                }
            }

            dx1 = x3 - x2;
            dy1 = y3 - y2;
            du1 = u3 - u2;
            dv1 = v3 - v2;

            dU1Step = 0; dV1Step = 0;
            if (dy1 != 0) {
                dStartXStep = (double)dx1/Math.abs(dy1);
                dU1Step = du1 /Math.abs(dy1);
                dV1Step = dv1 /Math.abs(dy1);
            }
            if (dy2 != 0) {
                dEndXStep = (double)dx2/Math.abs(dy2);
                dU2Step = du2 /Math.abs(dy2);
                dV2Step = dv2 /Math.abs(dy2);
            }

            if (dy1 != 0) {
                for (int i = y2; i <= y3; i++) {
                    int startX = x2 + (int) ((i-y2)*dStartXStep);
                    int endX = x1 + (int) ((i-y1)*dEndXStep);

                    double startU = u2 + (int) ((i-y2)*dU1Step);
                    double startV = v2 + (int) ((i-y2)*dV1Step);
                    double endU = u1 + (int) ((i-y1)*dU2Step);
                    double endV = v1 + (int) ((i-y1)*dV2Step);

                    //need to sort along x
                    if (startX > endX) {
                        int temp1 = startX;
                        startX = endX;
                        endX = temp1;
                        double temp2 = startU;
                        startU = endU;
                        endU = temp2;
                        temp2 = startV;
                        startV = endV;
                        endV = temp2;
                    }

                    double u;
                    double v;
                    double tStep = 1.0/(startX-endX);
                    double t = 0;
                    for (int j = startX; j < endX; j++) {
                        u = (1-t)*startU + t*endU; //linear interpolation, weighted average version
                        v = (1-t)*startV + t*endV;

                        int finU = Math.max(Math.min((int)(u*bi.getWidth()), bi.getWidth()-1), 0);
                        int finV = Math.max(Math.min((int)(v*bi.getHeight()), bi.getHeight()-1), 0);
                        int color = bi.getRGB(finU, finV);
                        //the getRGB function returns the RGB values bitwise so we need to convert them
                        int blue = color & 0xff; //basically isolates the blue value (rightmost 8)
                        // the 0xff is a hexadecimal number that represents a bunch of 1s and the and gets rid of the stuff we don't need
                        int green = (color & 0xff00) >> 8; //then gets the red value (the 8 bits after blue value)
                        //the extra 0s after the hexadecimal shift it 8 left so we can get the red values, and then the >> shifts the number back
                        int red = (color & 0xff0000) >> 16; //same thing as before but with 16
                        g.setColor(new Color(red, green, blue));
                        g.drawLine(j, i, j, i); //drawing a line with the same start and end draws a single pixel

                        t += tStep;

                    }

                }
            }
        }
    }
    public class Mesh {
        ArrayList<Triangle> tris;
        public Mesh(ArrayList<Triangle> t) {
            tris = t;
        }

        public boolean readObj(String fileName) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(fileName));
                String s = in.readLine();
                ArrayList<Point> pool = new ArrayList<>();
                while (s != null) {
                    StringTokenizer str = new StringTokenizer(s);
                    if (str.hasMoreTokens()) {
                        String start = str.nextToken();
                        if (start.equals("v")) {
                            double x = Double.parseDouble(str.nextToken());
                            double y = Double.parseDouble(str.nextToken());
                            double z = Double.parseDouble(str.nextToken());
                            pool.add(new Point(x, y, z));
                        } else if (start.equals("f")) { //assume is triangle or quad for sanity
                            int[] indices = new int[4];
                            int i = 0;
                            while (str.hasMoreTokens() && i < 4) {
                                indices[i] = Integer.parseInt(str.nextToken());
                                i++;
                            }
                            if (indices[3] == 0) {
                                tris.add(new Triangle(pool.get(indices[0] - 1), pool.get(indices[1] - 1), pool.get(indices[2] - 1)));
                            } else {
                                tris.add(new Triangle(pool.get(indices[0] - 1), pool.get(indices[1] - 1), pool.get(indices[2] - 1)));
                                tris.add(new Triangle(pool.get(indices[0] - 1), pool.get(indices[2] - 1), pool.get(indices[3] - 1)));
                            }
                        }
                    }
                    s = in.readLine();
                }
                in.close();
                return true;
            }
            catch (Exception e) {
                System.out.println("readObj failed");
                return false;
            }
        }
    }
}
