package graphics;

public class Matrix {
    public static Point multiplyVectMat (Point i, double[][] m) {
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

    public static double[][] matIdentity() {
        double[][] m = new double[4][4];
        m[0][0] = 1;
        m[1][1] = 1;
        m[2][2] = 1;
        m[3][3] = 1;
        return m;
    }

    public static double[][] matRotX(double rad) {
        double[][] rotX = new double[4][4];
        rotX[0][0] = 1;
        rotX[1][1] = Math.cos(rad);
        rotX[1][2] = -1*Math.sin(rad);
        rotX[2][1] = Math.sin(rad);
        rotX[2][2] = Math.cos(rad);
        rotX[3][3] = 1;
        return rotX;
    }

    public static double[][] matRotY(double rad) {
        double[][] rotY = new double[4][4];
        rotY[0][0] = Math.cos(rad);
        rotY[0][2] = Math.sin(rad);
        rotY[2][0] = -1*Math.sin(rad);
        rotY[1][1] = 1;
        rotY[2][2] = Math.cos(rad);
        rotY[3][3] = 1;
        return rotY;
    }

    public static double[][] matRotZ(double rad) {
        double[][] rotZ = new double[4][4];
        rotZ[0][0] = Math.cos(rad);
        rotZ[0][1] = -1*Math.sin(rad);
        rotZ[1][0] = Math.sin(rad);
        rotZ[1][1] = Math.cos(rad);
        rotZ[2][2] = 1;
        rotZ[3][3] = 1;
        return rotZ;
    }

    public static double[][] matTranslation(double x, double y, double z) {
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

    public static double[][] multiplyMat(double[][] m1, double[][] m2) {
        double[][] mat = new double[m1.length][m2.length];
        for (int r = 0; r < m1.length; r++) {
            for (int c = 0; c < m2.length; c++) {
                mat[r][c] = m1[r][0]*m2[0][c] + m1[r][1]*m2[1][c] + m1[r][2]*m2[2][c] + m1[r][3]*m2[3][c];
            }
        }
        return mat;
    }
}
