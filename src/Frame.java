import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Frame extends JFrame {
    public Frame() throws Exception {
        this.setTitle("3D");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        Panel p = new Panel();
        this.add(p);

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        while (true) {
            p.repaint();
        }
    }

    public static void main(String[] args) throws Exception {
        new Frame();
    }
}
