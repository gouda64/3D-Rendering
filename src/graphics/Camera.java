package graphics;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Camera { //TODO: work on textures
    private int width;
    private int height;
    private double fov; //scaling factor is 1/tan(theta/2) for x and y
    //zfar/(zfar-znear), aka scaling factor for z
    //offset factor for z -(zfar*znear)/(zfar-znear)
    //x, y, z -> aspectRatio*x*scaleFactor/z, y*scaleFactor/z, zScale*z - (zfar*znear)/(zfar-znear)
    //x' = x/z and y' = y/z (inverse proportionality)
    //input vector [x, y, z, 1]
    //projection matrix (multiply with input vector) [[xCoeff, 0, 0, 0], [0, yCoeff, 0, 0], [0, 0, zCoeff, zDispl], [0, 0, 1, 0]]
    private final Mesh meshCube;
    private double[][] projectionMatrix; //multiply by the input point/vector to normalize it into the screen space!
    private graphics.Point camera;
    private graphics.Point lookDir;
    private double yaw; //basically left-right rotation
    private double pitch; //up-down rotations

    public Camera(int width, int height, String mapFile) {
        this.width = width;
        this.height = height;

        fov = 90;
        projectionMatrix = matProjection(fov,(double) this.height/this.width, 0.5, 1000);

        camera = new graphics.Point(0, 0, 0);
        lookDir = new graphics.Point(0, 0, 1);
        yaw = 0;
        pitch = 0;

        //ArrayList<graphics.Triangle> t = new ArrayList<graphics.Triangle>();
        //t.add(new graphics.Triangle(new graphics.Point(10, 10, 10), new graphics.Point(20, 20, 10), new graphics.Point(30, 10, 10)));
        //meshCube = new graphics.Mesh(t);
        // for debugging (literally one triangle)

        meshCube = new Mesh(new ArrayList<>());
        meshCube.readObj(mapFile);
    }


    public List<Triangle> view() { //later - convert to pixel by pixel system (somehow?)
        //later - optimize - preserve info with small/no view changes - might be in other class
        ArrayList<Triangle> trisToDraw = new ArrayList<>();

        //TODO: clean up sometime (what does this even mean??)
        double[][] worldMat = Matrix.multiplyMat(Matrix.matRotZ(0), Matrix.matRotX(0));
        worldMat = Matrix.multiplyMat(worldMat, Matrix.matTranslation(0, 0, 10));

        lookDir = Matrix.multiplyVectMat(new graphics.Point(0, 0, 1), Matrix.matRotY(yaw));
        //TODO: fix rotation bug
        lookDir = Matrix.multiplyVectMat(lookDir, Matrix.matRotX(pitch));
        double[][] camMat = pointAt(camera, camera.add(lookDir), new graphics.Point(0, 1, 0))[1];
        //we need the inverse!! not the og, emphasis on the [1]

        //draw triangles
        for (Triangle t : meshCube.tris) {

            Triangle tTransformed = new Triangle(Matrix.multiplyVectMat(t.p1, worldMat),
                    Matrix.multiplyVectMat(t.p2, worldMat), Matrix.multiplyVectMat(t.p3, worldMat));

            //triangle culling
            graphics.Point normal, line1, line2; //actually 3D vectors but I'm too lazy to make a new class
            line1 = tTransformed.p2.sub(tTransformed.p1);
            line2 = tTransformed.p3.sub(tTransformed.p1);
            normal = line1.crossProduct(line2).normalize();

            //if(true) {
            if (normal.dotProduct(tTransformed.p1.sub(camera)) < 0) { //takes into account perspective w/ dot product

                //add lighting
                graphics.Point light_direction = new graphics.Point(0, 0, -1).normalize(); //single direction, very simple because it's just a huge plane
                //emitting consistent rays of light which is great because it's easy

                double dp = Math.max(0.25, light_direction.dotProduct(normal));
                Color c = new Color((float)dp, (float)dp, (float)dp);

                //converting world space to view space
                Triangle tView = new Triangle(Matrix.multiplyVectMat(tTransformed.p1, camMat),
                        Matrix.multiplyVectMat(tTransformed.p2, camMat), Matrix.multiplyVectMat(tTransformed.p3, camMat));

                Triangle[] clippedTris = triClipToPlane(new graphics.Point(0, 0, 0.2), new graphics.Point(0, 0, 1), tView);
                for (Triangle tris : clippedTris) {

                    Triangle tProjected = new Triangle(Matrix.multiplyVectMat(tris.p1, projectionMatrix),
                            Matrix.multiplyVectMat(tris.p2, projectionMatrix), Matrix.multiplyVectMat(tris.p3, projectionMatrix));

                    //offset and scale
                    graphics.Point addP = new graphics.Point(1, 1, 0);
                    tProjected.p1 = tProjected.p1.add(addP);
                    tProjected.p2 = tProjected.p2.add(addP);
                    tProjected.p3 = tProjected.p3.add(addP);

                    tProjected.p1.x *= 0.5 * width;
                    tProjected.p1.y *= 0.5 * height;
                    tProjected.p2.x *= 0.5 * width;
                    tProjected.p2.y *= 0.5 * height;
                    tProjected.p3.x *= 0.5 * width;
                    tProjected.p3.y *= 0.5 * height;

                    tProjected.c = c;

                    trisToDraw.add(tProjected);
                }
            }
        }

        Comparator<Triangle> compareByZ = (o1, o2) -> {
            double z1 = (o1.p1.z + o1.p2.z + o1.p3.z)/3;
            double z2 = (o2.p1.z + o2.p2.z + o2.p3.z)/3;
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
        trisToDraw.sort(compareByZ);

        List<Triangle> view = new ArrayList<>(); //later - optimize
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
                                triClipToPlane(new graphics.Point(0, 0, 0), new graphics.Point(0, 1, 0), test);
                        case 1 -> //bottom
                                triClipToPlane(new graphics.Point(0, height - 1, 0), new graphics.Point(0, -1, 0), test);
                        case 2 -> //left
                                triClipToPlane(new graphics.Point(0, 0, 0), new graphics.Point(1, 0, 0), test);
                        case 3 -> //right
                                triClipToPlane(new graphics.Point(width - 1, 0, 0), new graphics.Point(-1, 0, 0), test);
                        default -> new Triangle[1];
                    };

                    Collections.addAll(trisToClip, clippedTris);
                }
                newTris = trisToClip.size();
            }

            //just to clarify it's called trisToClip but they're done clipping at this point
            view.addAll(trisToClip);
        }

        return view;
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

    private static graphics.Point pointIntersectPlane(graphics.Point pPoint, graphics.Point pNormal, graphics.Point lStart, graphics.Point lEnd) {
        pNormal = pNormal.normalize();
        double t = (pNormal.dotProduct(pPoint) - lStart.dotProduct(pNormal)) / (lEnd.dotProduct(pNormal) - lStart.dotProduct(pNormal));
        //just something I found on stack overflow idk how it does it either
        return lStart.add(lEnd.sub(lStart).mult(t));
    }

    private static Triangle[] triClipToPlane(graphics.Point pPoint, graphics.Point pNormal, Triangle t) {
        pNormal = pNormal.normalize();
        //signed shortest distance from point to plane
        graphics.Point temp = t.p1;
        //DON'T normalize temp messes everything up I know from the week I spent debugging this thing
        double dist1 = pNormal.x*temp.x + pNormal.y*temp.y + pNormal.z*temp.z - pNormal.dotProduct(pPoint);
        temp = t.p2;
        double dist2 = pNormal.dotProduct(temp) - pNormal.dotProduct(pPoint);
        temp = t.p3;
        double dist3 = pNormal.dotProduct(temp)- pNormal.dotProduct(pPoint);
        graphics.Point[] inside = new graphics.Point[3]; int inNum = 0;
        graphics.Point[] outside = new graphics.Point[3]; int outNum = 0;

        if (dist1 >= 0) {
            inside[inNum] = t.p1;
            inNum++;
        }
        else {
            outside[outNum] = t.p1;
            outNum++;
        }
        if (dist2 >= 0) {
            inside[inNum] = t.p2;
            inNum++;
        }
        else {
            outside[outNum] = t.p2;
            outNum++;
        }
        if (dist3 >= 0) {
            inside[inNum] = t.p3;
            inNum++;
        }
        else {
            outside[outNum] = t.p3;
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
            newT.p1 = inside[0];
            newT.p2 = pointIntersectPlane(pPoint, pNormal, inside[0], outside[0]);
            newT.p3 = pointIntersectPlane(pPoint, pNormal, inside[0], outside[1]);

            newT.c = t.c; //you can set this and the two below to different colors for a nice demonstration of clipping

            return new Triangle[] {newT};
        }
        else {
            Triangle newT1 = new Triangle(null, null, null);
            Triangle newT2 = new Triangle(null, null, null);

            newT1.p1 = inside[0];
            newT1.p2 = inside[1];
            newT1.p3 = pointIntersectPlane(pPoint, pNormal, inside[0], outside[0]);

            newT2.p1 = inside[1];
            newT2.p2 = pointIntersectPlane(pPoint, pNormal, inside[0], outside[0]);
            newT2.p3 = pointIntersectPlane(pPoint, pNormal, inside[1], outside[0]);

            newT1.c = t.c;
            newT2.c = t.c;

            return new Triangle[] {newT1, newT2};
        }
    }

    private static double[][][] pointAt(graphics.Point pos, graphics.Point target, graphics.Point up) { //camera info!!
        graphics.Point newForward = target.sub(pos).normalize();
        graphics.Point newUp = up.sub(newForward.mult(up.dotProduct(newForward))).normalize(); //how much does newForward affect up?
        //just visualize the things graphically and it works out, if my linear-algebra-averse brain can do it, you can definitely do it!
        graphics.Point newRight = newUp.crossProduct(newForward);

        double[][] mat = new double[4][4]; //transformation matrix for looking at stuff
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

        return new double[][][] {mat, inverseMat};
    }

    public void moveY(double amt) {
        camera.y += amt;
    }
    public void moveForBack(double amt) {
        camera = camera.add(new graphics.Point(lookDir.x, 0, lookDir.z).mult(amt));
    }
    public void moveRightLeft(double amt) {
        camera = camera.add(lookDir.crossProduct(new Point(0, 1, 0)).mult(amt));
    }
    public void turnRightLeft(double amt) {
        yaw += amt;
    }
    public void turnUpDown(double amt) {
        pitch += amt;
    }
}