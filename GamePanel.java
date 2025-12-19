import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    final int WIDTH = 400;
    final int HEIGHT = 600;

    final int CAR_WIDTH = 50;
    final int CAR_HEIGHT = 80;

    // Lane X positions
    int[] lanes = {70, 170, 270};

    // Player
    int carLane = 1;
    int targetLane = 1;
    double carX = lanes[1];
    int carY = 450;

    // Road
    int lineY1 = 0;
    int lineY2 = 300;

    // Enemies
    int ENEMY_COUNT = 4;
    int[] enemyLane = new int[ENEMY_COUNT];
    int[] enemyY = new int[ENEMY_COUNT];

    // Game state
    int score = 0;
    boolean gameOver = false;

    // Speed
    int roadSpeed = 5;
    int enemySpeed = 6;
    int nitroBoost = 0;
    boolean nitroOn = false;

    Timer timer;
    Random rand = new Random();

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        startGame();
    }

    void startGame() {
        score = 0;
        gameOver = false;

        roadSpeed = 5;
        enemySpeed = 6;
        nitroBoost = 0;

        carLane = 1;
        targetLane = 1;
        carX = lanes[1];

        for (int i = 0; i < ENEMY_COUNT; i++) {
            enemyLane[i] = rand.nextInt(3);
            enemyY[i] = -200 * i;
        }

        timer = new Timer(16, this); // smooth ~60fps
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.GRAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.fillRect(195, lineY1, 10, 100);
        g.fillRect(195, lineY2, 10, 100);

        // Player
        g.setColor(Color.RED);
        g.fillRect((int) carX, carY, CAR_WIDTH, CAR_HEIGHT);

        // Enemies
        g.setColor(Color.BLUE);
        for (int i = 0; i < ENEMY_COUNT; i++) {
            g.fillRect(lanes[enemyLane[i]], enemyY[i], CAR_WIDTH, CAR_HEIGHT);
        }

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 25);
        g.drawString("Speed: " + (enemySpeed + nitroBoost), 300, 25);

        if (nitroOn) {
            g.setColor(Color.YELLOW);
            g.drawString("NITRO", 185, 25);
        }

        if (gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("GAME OVER", 90, 260);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press R to Restart", 115, 300);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        // 🔥 SMOOTH LANE MOVEMENT
        double targetX = lanes[targetLane];
        if (carX < targetX) carX += 8;
        if (carX > targetX) carX -= 8;

        if (Math.abs(carX - targetX) < 4) {
            carX = targetX;
            carLane = targetLane;
        }

        int totalSpeed = enemySpeed + nitroBoost;

        // Road movement
        lineY1 += roadSpeed + nitroBoost;
        lineY2 += roadSpeed + nitroBoost;

        if (lineY1 > HEIGHT) lineY1 = -100;
        if (lineY2 > HEIGHT) lineY2 = -100;

        // Enemies
        for (int i = 0; i < ENEMY_COUNT; i++) {
            enemyY[i] += totalSpeed;

            if (enemyY[i] > HEIGHT) {
                enemyY[i] = -200;
                enemyLane[i] = rand.nextInt(3);
                score++;

                if (score % 6 == 0 && enemySpeed < 12) {
                    enemySpeed++;
                    roadSpeed++;
                }
            }

            Rectangle player = new Rectangle((int) carX, carY, CAR_WIDTH, CAR_HEIGHT);
            Rectangle enemy = new Rectangle(lanes[enemyLane[i]], enemyY[i], CAR_WIDTH, CAR_HEIGHT);

            if (player.intersects(enemy)) {
                gameOver = true;
                timer.stop();
            }
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT && targetLane > 0) {
            targetLane--;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT && targetLane < 2) {
            targetLane++;
        }

        // Nitro
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            nitroOn = true;
            nitroBoost = 4;
        }

        if (gameOver && e.getKeyCode() == KeyEvent.VK_R) {
            startGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            nitroOn = false;
            nitroBoost = 0;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}
}
