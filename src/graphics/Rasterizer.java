package graphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class Rasterizer {
    //sep class because it's also gonna handle depth buffer, perspective stuff
    public static void drawTexTriangle(Graphics g, Triangle tri, int width, int height) {
        BufferedImage bi;
        try {
            bi = ImageIO.read(new File(tri.texFile)); //TODO: convert to tri stores the bi for efficiency
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("image read failed");
            return;
        }
        //TODO: clean up someday, somehow, because it's just awful rn DRY is crying screaming throwing up

        graphics.Point[] ptsClone = tri.pts.clone();
        int[] sortedIndices = IntStream.range(0, ptsClone.length)
                .boxed().sorted(Comparator.comparingDouble(i -> ptsClone[i].y))
                .mapToInt(ele -> ele).toArray();
        Arrays.sort(ptsClone, Comparator.comparingDouble((p) -> p.y));
        //yeah, sorting twice, but it's literally 3 elements and it's more concise + extensible
        graphics.Point[] texPtsClone = new graphics.Point[ptsClone.length];
        for (int i = 0; i < texPtsClone.length; i++) {texPtsClone[i] = tri.texPts[sortedIndices[i]];}

        int x1 = (int)ptsClone[0].x; int x2 = (int)ptsClone[1].x; int x3 = (int)ptsClone[2].x;
        int y1 = (int)ptsClone[0].y; int y2 = (int)ptsClone[1].y; int y3 = (int)ptsClone[2].y;
        int u1 = (int)texPtsClone[0].x; int u2 = (int)texPtsClone[1].x; int u3 = (int)texPtsClone[2].x;
        int v1 = (int)texPtsClone[0].y; int v2 = (int)texPtsClone[1].y; int v3 = (int)texPtsClone[2].y;
        //System.out.println(Arrays.toString(texPtsClone));

        int dx1 = x2 - x1; int dy1 = y2 - y1;
        double du1 = u2 - u1; double dv1 = v2 - v1;

        int dx2 = x3 - x1; int dy2 = y3 - y1;
        double du2 = u3 - u1; double dv2 = v3 - v1;

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
        //System.out.println(dU1Step);

        for (int z = 0; z < 2; z++) {
            if (dy1 != 0) { //if the line is not flat, draw the triangle from the top to the point in between
                for (int i = (z==0 ? y1 : y2); i <= (z==0 ? y2: y3); i++) {
                    int startX = z==0 ? (x1 + (int) ((i - y1) * dStartXStep)) :
                            (x2 + (int) ((i - y2) * dStartXStep));
                    int endX = x1 + (int) ((i - y1) * dEndXStep);


                    double startU = z==0 ? (u1 + ((i-y1)*dU1Step)) :
                            (u2 + ((i - y2) * dU1Step));
                    double startV = z==0 ? (v1 + ((i-y1)*dV1Step)) :
                            (v2 + ((i - y2) * dV1Step));
                    double endU = u1 + ((i - y1) * dU2Step);
                    double endV = v1 + ((i - y1) * dV2Step);

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

                    double u, v;
                    double tStep = 1.0 / (endX - startX);
                    double t = 0;
                    //System.out.println("start and end U " + startU + " " + endU);
//                    System.out.println("start and end V " + startV + " " + endV);

                    for (int j = startX; j < endX; j++) {
                        //linear interpolation, weighted average version
                        //System.out.println(t);
                        u = (1 - t) * startU + t * endU;
                        v = (1 - t) * startV + t * endV;

                        int finU = Math.max(Math.min((int) (u * bi.getWidth()), bi.getWidth() - 1), 0);
                        int finV = Math.max(Math.min((int) (v * bi.getHeight()), bi.getHeight() - 1), 0);
                        int color = bi.getRGB(bi.getWidth()-finU-1, bi.getHeight()-finV-1);
                        //bitwise rgb color value
                        Color c = new Color((color & 0xff0000) >> 16, (color & 0xff00) >> 8, color & 0xff);

                        //System.out.println(u + " " + v + " " + c);

                        g.setColor(c);
                        g.drawRect(width-j, height-i, 1, 1);

                        //System.out.println(j + " " + i);

                        t += tStep;
                    }

                }
            }

            dx1 = x3 - x2; dy1 = y3 - y2;
            du1 = u3 - u2; dv1 = v3 - v2;

            dU1Step = 0; dV1Step = 0;

            if (dy1 != 0) {
                dStartXStep = (double)dx1/Math.abs(dy1);
                dU1Step = du1/Math.abs(dy1);
                dV1Step = dv1/Math.abs(dy1);
            }
            if (dy2 != 0) {
                dEndXStep = (double)dx2/Math.abs(dy2);
            }
        }

    }
}
