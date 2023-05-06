package graphics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Mesh { //if this is still useless later on get rid of it
    //also convert to list sometime or smth
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
            e.printStackTrace();
            System.out.println("readObj failed");
            return false;
        }
    }
}
