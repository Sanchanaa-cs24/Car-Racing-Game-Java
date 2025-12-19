import javax.swing.JFrame;

public class CarGame {

    public static void main(String[] args) {

        JFrame frame = new JFrame("2D Car Racing Game");
        GamePanel panel = new GamePanel();

        frame.add(panel);
        frame.setSize(400, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}
