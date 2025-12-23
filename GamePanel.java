import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Screen & road
    final int WIDTH = 400, HEIGHT = 600;
    final int ROAD_X = 40, ROAD_WIDTH = 320;
    final int LANE_WIDTH = ROAD_WIDTH / 3;

    // Cars
    final int CAR_W = 80, CAR_H = 100;
    final int PLAYER_Y = 420;

    // Player
    int playerLane = 1;
    double playerX;

    // 🚗 Enemies (4 = tougher but fair)
    final int ENEMY_COUNT = 4;
    double[] enemyX = new double[ENEMY_COUNT];
    int[] enemyY = new int[ENEMY_COUNT];
    int[] targetLane = new int[ENEMY_COUNT];

    // Game state
    int score = 0;
    int speedLevel = 1;
    boolean gameOver = false;

    // Speed (tougher curve)
    int roadSpeed = 6;
    int enemySpeed = 6;
    int roadOffset = 0;

    // Nitro
    int nitro = 100;
    boolean nitroOn = false;

    // Explosion
    boolean exploding = false;
    int explosionFrame = 0;

    // 🚘 Skid marks
    class Skid {
        int x, y, life = 25;
    }
    ArrayList<Skid> skids = new ArrayList<>();

    // Images
    Image playerCar, enemyCar;

    Timer timer;
    Random rand = new Random();

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);

        playerCar = loadImage("/images/player.png");
        enemyCar  = loadImage("/images/enemy.png");

        playerX = laneCenter(playerLane) - CAR_W / 2;

        for (int i = 0; i < ENEMY_COUNT; i++) {
            targetLane[i] = i % 3;
            enemyX[i] = laneCenter(targetLane[i]) - CAR_W / 2;
            enemyY[i] = -220 * (i + 1);
        }

        timer = new Timer(16, this);
        timer.start();
    }

    Image loadImage(String path) {
        try {
            return new ImageIcon(getClass().getResource(path)).getImage();
        } catch (Exception e) {
            return null;
        }
    }

    int laneCenter(int lane) {
        return ROAD_X + lane * LANE_WIDTH + LANE_WIDTH / 2;
    }

    boolean enemyTooClose(int lane, int y, int self) {
        for (int i = 0; i < ENEMY_COUNT; i++) {
            if (i == self) continue;
            if (targetLane[i] == lane && Math.abs(enemyY[i] - y) < 150)
                return true;
        }
        return false;
    }

    // ================= GAME LOOP =================
    @Override
    public void actionPerformed(ActionEvent e) {

        if (gameOver && !exploding) return;

        // Player movement (snappy = harder)
        double targetX = laneCenter(playerLane) - CAR_W / 2;
        double dx = targetX - playerX;
        playerX += dx * 0.22;

        // 🚘 Skid marks when turning hard or nitro
        if (Math.abs(dx) > 6 || nitroOn) {
            addSkid((int)playerX + 18);
            addSkid((int)playerX + CAR_W - 22);
        }

        updateSkids();

        // Nitro
        int boost = 0;
        if (nitroOn && nitro > 0) {
            boost = 4;
            nitro--;
        } else if (!nitroOn && nitro < 100) {
            nitro++;
        }

        roadOffset = (roadOffset + roadSpeed + boost) % 60;

        // 🔥 Increase difficulty every 5 score
        int newLevel = score / 5 + 1;
        if (newLevel > speedLevel) {
            speedLevel = newLevel;
            roadSpeed++;
            enemySpeed++;
        }

        // Enemies
        for (int i = 0; i < ENEMY_COUNT; i++) {

            enemyY[i] += enemySpeed + boost;

            double exTarget = laneCenter(targetLane[i]) - CAR_W / 2;
            enemyX[i] += (exTarget - enemyX[i]) * 0.08; // faster switching

            // More frequent lane switching = harder
            if (enemyY[i] < 250 && rand.nextInt(80) == 0) {
                int dir = rand.nextBoolean() ? 1 : -1;
                int newLane = Math.max(0, Math.min(2, targetLane[i] + dir));
                if (!enemyTooClose(newLane, enemyY[i], i))
                    targetLane[i] = newLane;
            }

            if (enemyY[i] > HEIGHT) {
                int lane;
                do {
                    lane = rand.nextInt(3);
                } while (enemyTooClose(lane, -300, i));

                targetLane[i] = lane;
                enemyX[i] = laneCenter(lane) - CAR_W / 2;
                enemyY[i] = -300;
                score++;
            }

            Rectangle player = new Rectangle((int)playerX + 15, PLAYER_Y + 20, 50, 60);
            Rectangle enemy  = new Rectangle((int)enemyX[i] + 15, enemyY[i] + 20, 50, 60);

            if (!exploding && player.intersects(enemy)) {
                exploding = true;
                explosionFrame = 1;
            }
        }

        // Explosion animation
        if (exploding) {
            explosionFrame++;
            if (explosionFrame > 20) {
                exploding = false;
                gameOver = true;
                timer.stop();
            }
        }

        repaint();
    }

    // ================= DRAW =================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(new Color(50,50,50));
        g2.fillRect(ROAD_X, 0, ROAD_WIDTH, HEIGHT);

        g2.setColor(Color.WHITE);
        for (int y = roadOffset; y < HEIGHT; y += 60) {
            g2.fillRect(laneCenter(1)-LANE_WIDTH/2, y, 4, 40);
            g2.fillRect(laneCenter(2)-LANE_WIDTH/2, y, 4, 40);
        }

        // Skids
        g2.setColor(new Color(60,60,60,180));
        for (Skid s : skids)
            g2.fillRect(s.x, s.y, 4, 14);

        // Player
        drawCar(g2, playerCar, (int)playerX, PLAYER_Y, Color.CYAN);

        // Enemies
        for (int i = 0; i < ENEMY_COUNT; i++)
            drawCar(g2, enemyCar, (int)enemyX[i], enemyY[i], Color.RED);

        // Explosion
        if (exploding) {
            int size = explosionFrame * 16;
            int cx = (int)playerX + CAR_W / 2;
            int cy = PLAYER_Y + CAR_H / 2;
            g2.setColor(new Color(255, 80, 0, 180));
            g2.fillOval(cx-size/2, cy-size/2, size, size);
            g2.setColor(new Color(255, 230, 150, 180));
            g2.fillOval(cx-size/4, cy-size/4, size/2, size/2);
        }

        // HUD
        g2.setColor(Color.WHITE);
        g2.drawString("Score: " + score, 10, 20);
        g2.drawString("Speed Lv: " + speedLevel, 290, 20);

        // Nitro bar
        g2.setColor(Color.GRAY);
        g2.fillRect(120, 10, 160, 10);
        g2.setColor(Color.CYAN);
        g2.fillRect(120, 10, nitro * 160 / 100, 10);
        g2.drawRect(120, 10, 160, 10);

        if (gameOver) {
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.drawString("GAME OVER", 90, 260);
            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            g2.drawString("Press R to Restart", 115, 300);
        }
    }

    // ===== SKID HELPERS =====
    void addSkid(int x) {
        Skid s = new Skid();
        s.x = x;
        s.y = PLAYER_Y + CAR_H;
        skids.add(s);
    }

    void updateSkids() {
        skids.removeIf(s -> s.life-- <= 0);
        for (Skid s : skids) s.y += 4;
    }

    // Draw image or fallback
    void drawCar(Graphics2D g2, Image img, int x, int y, Color fallback) {
        if (img != null && img.getWidth(null) > 0) {
            g2.drawImage(img, x, y, CAR_W, CAR_H, this);
        } else {
            g2.setColor(fallback);
            g2.fillRoundRect(x, y, CAR_W, CAR_H, 20, 20);
        }
    }

    // ================= INPUT =================
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT && playerLane > 0) playerLane--;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT && playerLane < 2) playerLane++;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) nitroOn = true;
        if (gameOver && e.getKeyCode() == KeyEvent.VK_R) restart();
    }

    void restart() {
        score = 0;
        speedLevel = 1;
        roadSpeed = 6;
        enemySpeed = 6;
        nitro = 100;
        gameOver = false;
        exploding = false;

        playerLane = 1;
        playerX = laneCenter(playerLane) - CAR_W / 2;

        skids.clear();

        for (int i = 0; i < ENEMY_COUNT; i++) {
            targetLane[i] = i % 3;
            enemyX[i] = laneCenter(targetLane[i]) - CAR_W / 2;
            enemyY[i] = -220 * (i + 1);
        }

        timer.start();
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) nitroOn = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}



