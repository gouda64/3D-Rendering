package graphics;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Camera {
    private final int width;
    private final int height;
    private final double fov; //scaling factor is 1/tan(theta/2) for x and y
    //zfar/(zfar-znear), aka scaling factor for z
    //offset factor for z -(zfar*znear)/(zfar-znear)
    //x, y, z -> aspectRatio*x*scaleFactor/z, y*scaleFactor/z, zScale*z - (zfar*znear)/(zfar-znear)
    //x' = x/z and y' = y/z (inverse proportionality)
    //input vector [x, y, z, 1]
    //projection matrix (multiply with input vector) [[xCoeff, 0, 0, 0], [0, yCoeff, 0, 0], [0, 0, zCoeff, zDispl], [0, 0, 1, 0]]
    private final Mesh mesh;
    private final double[][] projectionMatrix; //multiply by the input point/vector to normalize it into the screen space!
    private Point camera;
    private Point lookDir;
    private double yaw; //basically left-right rotation
    private double pitch; //up-down rotations

    public Camera(int width, int height, String mapFile) {
        this.width = width;
        this.height = height;

        fov = 90;
        projectionMatrix = matProjection(fov,(double) this.height/this.width, 0.5, 1000);

        camera = new Point(0, 0, 0);
        lookDir = new Point(0, 0, 1);
        yaw = 0;
        pitch = 0;

        List<Triangle> testTri = new ArrayList<>();
        testTri.add(new Triangle(new Point(10, 10, 10), new Point(20, 20, 10), new Point(30, 10, 10)));
        mesh = new Mesh(testTri);
        // for debugging (literally one triangle)

//        mesh = new Mesh(new ArrayList<>());
//        mesh.readObj("./assets/cube.txt");
    }

    public List<Triangle> view() {
        //10??
        double[][] worldMat = Matrix.translation(0, 0, 10);

        lookDir = Matrix.multiplyVecMat(new Point(0, 0, 1), Matrix.rotY(yaw));

        //TODO: fix rotation bug - something to do with pitch (not that easy?)
        //lookDir = Matrix.multiplyVecMat(lookDir, Matrix.rotX(pitch));
        //System.out.println(lookDir);

        double[][] camMat = pointAt(camera, camera.add(lookDir), new Point(0, 1, 0))[1];
        //we need the inverse!! not the og, emphasis on the [1]

        //find drawing triangles
        List<Triangle> trisToDraw = cullAndProject(worldMat, camMat);
        sortByAvgZ(trisToDraw);

        List<Triangle> view = new ArrayList<>();
        for (Triangle t : trisToDraw) {
            //clip triangles against screen edges
            view.addAll(getClippedTris(t));
        }

        return view;
    }

    private List<Triangle> cullAndProject(double[][] worldMat, double[][] camMat) {
        List<Triangle> trisToDraw = new ArrayList<>();

        for (Triangle t : mesh.tris) {
            //converting to world space
            Triangle tTransformed = transformTriByMat(t, worldMat);

            //triangle culling
            Point normal, line1, line2; //actually 3D vectors but I'm too lazy to make a new class
            line1 = tTransformed.pts[1].sub(tTransformed.pts[0]);
            line2 = tTransformed.pts[2].sub(tTransformed.pts[0]);
            normal = line1.crossProduct(line2).normalize();

            if (normal.dotProduct(tTransformed.pts[0].sub(camera)) < 0) { //takes into account perspective w/ dot product

                //add lighting
                Point lightDirection = new Point(0, 0, -1).normalize();
                //single direction, very simple because it's just a huge plane
                //emitting consistent rays of light which is great because it's easy
                Color c = triLighting(t, normal, lightDirection);

                //converting world space to view space
                Triangle tView = transformTriByMat(tTransformed, camMat);

                Triangle[] clippedTris = triClipToPlane(new Point(0, 0, 0.2), new Point(0, 0, 1), tView);
                for (Triangle tri : clippedTris) {
                    //view space to projected screen space
                    Triangle tProjected = transformTriByMat(tri, projectionMatrix);

                    //offset and scale
                    Point addP = new Point(1, 1, 0);
                    for (int i = 0; i < tProjected.pts.length; i++) {
                        tProjected.pts[i] = tProjected.pts[i].add(addP);
                    }
                    for (int i = 0; i < tProjected.pts.length; i++) {
                        tProjected.pts[i].x *= 0.5 * width;
                        tProjected.pts[i].y *= 0.5 * height;
                    }

                    tProjected.c = c;

                    trisToDraw.add(tProjected);
                }
            }
        }
        return trisToDraw;
    }

    private List<Triangle> getClippedTris(Triangle t) {
        List<Triangle> clippedTris = new ArrayList<>();
        clippedTris.add(t);

        int newTris = 1;
        for (int i = 0; i < 4; i++) {

            while (newTris > 0) {
                Triangle test = clippedTris.remove(0);
                //could be slightly more efficient as a queue but meh
                newTris--;
                Triangle[] clippedTemp = switch (i) {
                    case 0 -> //top
                            triClipToPlane(new Point(0, 0, 0), new Point(0, 1, 0), test);
                    case 1 -> //bottom
                            triClipToPlane(new Point(0, height - 1, 0), new Point(0, -1, 0), test);
                    case 2 -> //left
                            triClipToPlane(new Point(0, 0, 0), new Point(1, 0, 0), test);
                    case 3 -> //right
                            triClipToPlane(new Point(width - 1, 0, 0), new Point(-1, 0, 0), test);
                    default -> new Triangle[0];
                };

                Collections.addAll(clippedTris, clippedTemp);
            }
            newTris = clippedTris.size();
        }

        return clippedTris;
    }

    private void sortByAvgZ(List<Triangle> tris) {
        Comparator<Triangle> compareByZ = (t1, t2) -> {
            double z1 = t1.avgZ();
            double z2 = t2.avgZ();
            if (z1-z2 < 0) {
                return 1;
            }
            if (z1-z2 > 0){
                return -1;
            }
            return 0;
        };
        tris.sort(compareByZ);
    }
    private Color triLighting(Triangle t, Point triNormal, Point lightDirection) {
        double dp = Math.max(0.25, lightDirection.dotProduct(triNormal));
        return new Color((float) dp, (float) dp, (float) dp);
    }
    private Triangle transformTriByMat(Triangle t, double[][] mat) {
        return new Triangle(Matrix.multiplyVecMat(t.pts[0], mat), //TODO: see if transfer method is fine
                Matrix.multiplyVecMat(t.pts[1], mat), Matrix.multiplyVecMat(t.pts[2], mat), t.texPts, t.texFile);
    }

    private static double[][] matProjection(double fovDeg, double aspectRatio, double zNear, double zFar) {
        double scaleFactor = 1/Math.tan(Math.toRadians(fovDeg)/2);
        double[][] projMat = new double[4][4];
        projMat[0][0] = aspectRatio*scaleFactor;
        projMat[1][1] = scaleFactor;
        projMat[2][2] = zFar/(zFar-zNear);
        projMat[3][2] = -1*(zFar*zNear)/(zFar-zNear);
        projMat[2][3] = 1;
        return projMat;
    }

    private static Point pointIntersectPlane(Point pPoint, Point pNormal, Point lStart, Point lEnd) {
        pNormal = pNormal.normalize();
        double t = (pNormal.dotProduct(pPoint) - lStart.dotProduct(pNormal)) /
                (lEnd.dotProduct(pNormal) - lStart.dotProduct(pNormal));
        //just something I found on stack overflow idk how it does it either
        return lStart.add(lEnd.sub(lStart).mult(t));
    }

    private static Triangle[] triClipToPlane(Point pPoint, Point pNormal, Triangle t) {
        //TODO: doesn't transfer texture, fix + perspective
        pNormal = pNormal.normalize();
        //signed shortest distance from point to plane
        //DON'T normalize temp messes everything up I know from the week I spent debugging this thing
        //or maybe it doesn't?
        double[] dists = new double[3];
        for (int i = 0; i < 3; i++) {
            Point temp = t.pts[i];//.normalize();
            dists[i] = pNormal.dotProduct(temp) - pNormal.dotProduct(pPoint);
        }

        Point[] inside = new Point[3]; int inNum = 0;
        Point[] outside = new Point[3]; int outNum = 0;
        Point[] insideTex = new Point[3];
        Point[] outsideTex = new Point[3];

        for (int i = 0; i < dists.length; i++) {
            if (dists[i] >= 0) {
                inside[inNum] = t.pts[i];
                insideTex[inNum] = t.texPts[i];
                inNum++;
            }
            else {
                outside[outNum] = t.pts[i];
                outsideTex[outNum] = t.texPts[i];
                outNum++;
            }
        }

//        int i = 1;
//        if (i == 1) return new Triangle[]{t};

        switch (inNum) {
            case 0:
                return new Triangle[]{};
            case 3:
                return new Triangle[]{t};
            case 1:
                Triangle newT = new Triangle(null, null, null);
                newT.texFile = t.texFile;

                newT.pts[0] = inside[0];
                newT.pts[1] = pointIntersectPlane(pPoint, pNormal, inside[0], outside[0]);
                newT.pts[2] = pointIntersectPlane(pPoint, pNormal, inside[0], outside[1]);

                newT.texPts[0] = insideTex[0];
                double t1 = (pNormal.dotProduct(pPoint) - inside[0].dotProduct(pNormal)) /
                        (outside[0].dotProduct(pNormal) - inside[0].dotProduct(pNormal)); //same thing as inside pIP
//                if (Double.isNaN(t1)) {
//                    System.out.println("no! " + Arrays.toString(t.pts));
//                }

                newT.texPts[1] = new Point(t1*(outsideTex[0].x - insideTex[0].x) + insideTex[0].x,
                t1*(outsideTex[0].y - insideTex[0].y) + insideTex[0].y);

                t1 = (pNormal.dotProduct(pPoint) - inside[0].dotProduct(pNormal)) /
                        (outside[1].dotProduct(pNormal) - inside[0].dotProduct(pNormal));
                newT.texPts[2]= new Point(t1*(outsideTex[1].x - insideTex[0].x) + insideTex[0].x,
                        t1*(outsideTex[1].y - insideTex[0].y) + insideTex[0].y);

                newT.c = t.c; //you can set this and the two below to different colors for a nice demonstration of clipping

                //System.out.println(Arrays.toString(newT.texPts));

                return new Triangle[]{newT};
            default: //quad case
                Triangle newT1 = new Triangle(null, null, null);
                Triangle newT2 = new Triangle(null, null, null);

                newT1.texFile = t.texFile;
                newT2.texFile = t.texFile;

                newT1.pts[0] = inside[0];
                newT1.pts[1] = inside[1];
                newT1.pts[2] = pointIntersectPlane(pPoint, pNormal, inside[0], outside[0]);

                newT1.texPts[0] = insideTex[0];
                newT1.texPts[1] = insideTex[1];
                t1 = (pNormal.dotProduct(pPoint) - inside[0].dotProduct(pNormal)) /
                        (outside[0].dotProduct(pNormal) - inside[0].dotProduct(pNormal));
                newT1.texPts[2] = new Point(t1*(outsideTex[0].x - insideTex[0].x) + insideTex[0].x,
                        t1*(outsideTex[0].y - insideTex[0].y) + insideTex[0].y);

                newT2.pts[0] = inside[1];
                newT2.pts[1] = newT1.pts[2];
                newT2.pts[2] = pointIntersectPlane(pPoint, pNormal, inside[1], outside[0]);

                newT2.texPts[0] = insideTex[1];
                newT2.texPts[1] = newT1.texPts[2];
                t1 = (pNormal.dotProduct(pPoint) - inside[1].dotProduct(pNormal)) /
                        (outside[0].dotProduct(pNormal) - inside[1].dotProduct(pNormal));                
                newT2.texPts[2] = new Point(t1*(outsideTex[0].x - insideTex[1].x) + insideTex[1].x,
                            t1*(outsideTex[0].y - insideTex[1].y) + insideTex[1].y);

                newT1.c = t.c; newT2.c = t.c;

                //System.out.println(Arrays.toString(newT1.texPts));
                //System.out.println(Arrays.toString(newT2.texPts));

                return new Triangle[]{newT1, newT2};
        }
    }

    private static double[][][] pointAt(Point pos, Point target, Point up) { //camera info!!
        Point newForward = target.sub(pos).normalize();
        Point newUp = up.sub(newForward.mult(up.dotProduct(newForward))).normalize(); //how much does newForward affect up?
        //just visualize the things graphically and it works out, if my linear-algebra-averse brain can do it, you can definitely do it!
        Point newRight = newUp.crossProduct(newForward);

        double[][] mat = new double[4][4]; //transformation matrix for looking at stuff
        //tbh actually cleaning this up would require too much finangling, sometimes you just gotta accept it
        mat[0][0] = newRight.x; mat[0][1] = newRight.y; mat[0][2] = newRight.z;
        mat[1][0] = newUp.x; mat[1][1] = newUp.y; mat[1][2] = newUp.z;
        mat[2][0] = newForward.x; mat[2][1] = newForward.y; mat[2][2] = newForward.z;
        mat[3][0] = pos.x; mat[3][1] = pos.y; mat[3][2] = pos.z; mat[3][3] = 1;

        double[][] inverseMat = new double[4][4];
        inverseMat[0][0] = newRight.x; inverseMat[1][0] = newRight.y; inverseMat[2][0] = newRight.z;
        inverseMat[0][1] = newUp.x; inverseMat[1][1] = newUp.y; inverseMat[2][1] = newUp.z;
        inverseMat[0][2] = newForward.x; inverseMat[1][2] = newForward.y; inverseMat[2][2] = newForward.z;
        inverseMat[3][0] = -pos.dotProduct(newRight); inverseMat[3][1] = -pos.dotProduct(newUp);
        inverseMat[3][2] = -pos.dotProduct(newForward); inverseMat[3][3] = 1;

        return new double[][][] {mat, inverseMat}; //can change to just mat + make function to invert matrix
    }

    public void moveY(double amt) {
        camera.y += amt;
    }
    public void moveForBack(double amt) {
        camera = camera.add(new Point(lookDir.x, 0, lookDir.z).mult(amt));
    }
    public void moveRightLeft(double amt) {
        camera = camera.add(lookDir.crossProduct(new Point(0, 1, 0)).mult(amt));
    }
    public void turnRightLeft(double amt) {
        yaw += amt;
        yaw %= 2*Math.PI;
    }
//    public void turnUpDown(double amt) {
//        pitch += amt;
//        if (pitch >= Math.PI/2) {
//            pitch = Math.PI/2 - 0.01;
//        }
//        else if (pitch <= -Math.PI/2) {
//            pitch = -Math.PI/2 + 0.01;
//        }
//    }
}