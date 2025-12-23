import javax.swing.*;

public class CarGame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Car Racing Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 600);
            frame.setResizable(false);

            frame.add(new GamePanel());

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

