import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener {

    // Screen & road
    final int WIDTH = 400, HEIGHT = 600;
    final int ROAD_X = 40, ROAD_WIDTH = 320;
    final int LANE_WIDTH = ROAD_WIDTH / 3;

    // Cars
    final int CAR_W = 80, CAR_H = 100;
    final int HIT_W = 45, HIT_H = 60;

    // Player
    int targetLane = 1;
    double carX;
    final int carY = 420;

    // Enemies
    final int MAX_ENEMIES = 6;
    int enemyCount = 3;
    double[] enemyX = new double[MAX_ENEMIES];
    int[] enemyY = new int[MAX_ENEMIES];
    int[] enemyLane = new int[MAX_ENEMIES];

    // Game state
    int score = 0;
    int level = 1;
    int lastLevelScore = 0;
    boolean gameOver = false;

    // Speed
    int roadSpeed = 5;
    int enemySpeed = 6;
    int roadOffset = 0;

    // Nitro
    int nitro = 100;
    boolean nitroOn = false;

    // 🚓 Police chase
    boolean policeActive = false;
    double policeX;
    double policeY;
    int lastPoliceScore = 0;
    int policePressure = 0;     // NEW
    final int MAX_PRESSURE = 120;

    // Assets
    Image playerCar, enemyCar, policeCar;
    Timer timer;
    Random rand = new Random();

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);

        playerCar = load("images/player.png");
        enemyCar  = load("images/enemy.png");
        policeCar = load("images/police.png");

        startGame();
    }

    Image load(String path) {
        var url = getClass().getClassLoader().getResource(path);
        if (url == null)
            return new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        return new ImageIcon(url).getImage();
    }

    int laneCenter(int lane) {
        return ROAD_X + lane * LANE_WIDTH + LANE_WIDTH / 2;
    }

    void startGame() {
        score = 0;
        level = 1;
        lastLevelScore = 0;
        lastPoliceScore = 0;
        policePressure = 0;
        policeActive = false;
        enemyCount = 3;
        enemySpeed = 6;
        nitro = 100;
        gameOver = false;

        carX = laneCenter(targetLane) - CAR_W / 2;

        for (int i = 0; i < MAX_ENEMIES; i++) {
            enemyLane[i] = rand.nextInt(3);
            enemyX[i] = laneCenter(enemyLane[i]) - CAR_W / 2;
            enemyY[i] = -300 * (i + 1);
        }

        timer = new Timer(16, this);
        timer.start();
    }

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

        g2.drawImage(playerCar, (int)carX, carY, CAR_W, CAR_H, this);

        for (int i = 0; i < enemyCount; i++)
            g2.drawImage(enemyCar, (int)enemyX[i], enemyY[i], CAR_W, CAR_H, this);

        if (policeActive)
            g2.drawImage(policeCar, (int)policeX, (int)policeY, CAR_W, CAR_H, this);

        // HUD
        g2.setColor(Color.WHITE);
        g2.drawString("Score: " + score, 10, 20);
        g2.drawString("LEVEL " + level, 300, 20);

        // Nitro
        g2.setColor(Color.GRAY);
        g2.fillRect(120, 10, 160, 10);
        g2.setColor(Color.CYAN);
        g2.fillRect(120, 10, nitro * 160 / 100, 10);
        g2.drawRect(120, 10, 160, 10);

        // 🚓 Police pressure bar
        if (policeActive) {
            g2.setColor(Color.RED);
            g2.fillRect(120, 26, policePressure * 160 / MAX_PRESSURE, 6);
            g2.drawRect(120, 26, 160, 6);
        }

        if (gameOver) {
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.drawString("BUSTED!", 120, 260);
            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            g2.drawString("Press R to Restart", 120, 300);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver) return;

        // Player move
        carX += (laneCenter(targetLane) - CAR_W/2 - carX) * 0.15;

        // Nitro
        int boost = (nitroOn && nitro > 0) ? 4 : 0;
        if (nitroOn && nitro > 0) nitro--;
        else nitro = Math.min(100, nitro + 1);

        roadOffset = (roadOffset + roadSpeed + boost) % 60;

        // Level up
        if (score >= lastLevelScore + 10) {
            level++;
            lastLevelScore = score;
            enemySpeed++;
        }

        // Spawn police every 20 score
        if (score >= lastPoliceScore + 20) {
            lastPoliceScore = score;
            policeActive = true;
            policePressure = 0;
            policeY = carY + 250;   // FAR behind
            policeX = laneCenter(targetLane) - CAR_W/2;
        }

        // Enemies
        for (int i = 0; i < enemyCount; i++) {
            enemyY[i] += enemySpeed + boost;
            if (enemyY[i] > HEIGHT) {
                enemyY[i] = -300;
                enemyLane[i] = rand.nextInt(3);
                enemyX[i] = laneCenter(enemyLane[i]) - CAR_W/2;
                score++;
            }
        }

        // 🚓 Police chase logic (NO instant death)
        if (policeActive) {
            policeX += (laneCenter(targetLane) - CAR_W/2 - policeX) * 0.08;

            // Police tries to close gap slowly
            policeY -= (enemySpeed - boost);

            int gap = (int)(policeY - carY);

            if (gap < 120) policePressure++;
            else policePressure = Math.max(0, policePressure - 2);

            // Escape condition
            if (gap > 300) policeActive = false;

            if (policePressure >= MAX_PRESSURE) gameOver = true;
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_LEFT && targetLane>0) targetLane--;
        if (e.getKeyCode()==KeyEvent.VK_RIGHT && targetLane<2) targetLane++;
        if (e.getKeyCode()==KeyEvent.VK_SHIFT) nitroOn = true;
        if (gameOver && e.getKeyCode()==KeyEvent.VK_R) startGame();
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_SHIFT) nitroOn = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}


    void startGame() {
        score = 0;
        gameOver = false;
        exploding = false;
        explosionFrame = 0;

        nitro = 100;
        roadSpeed = 5;
        enemySpeed = 6;

        targetLane = 1;
        carX = getLaneCenter(targetLane) - CAR_WIDTH / 2;

        skids.clear();

        for (int i = 0; i < ENEMY_COUNT; i++) {
            enemyLane[i] = rand.nextInt(3);
            enemyY[i] = -300 * (i + 1);
        }

        timer = new Timer(16, this);
        timer.start();
    }

    int getLaneCenter(int lane) {
        return ROAD_X + lane * LANE_WIDTH + LANE_WIDTH / 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Vertical shake
        if ((nitroOn && nitro > 0) || exploding) {
            shakeY = rand.nextInt(6) - 3;
        } else {
            shakeY = 0;
        }
        g2.translate(0, shakeY);

        // Background
        g2.setColor(new Color(20, 20, 20));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Road
        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(ROAD_X, 0, ROAD_WIDTH, HEIGHT);

        // Borders
        g2.setColor(Color.WHITE);
        g2.fillRect(ROAD_X - 4, 0, 4, HEIGHT);
        g2.fillRect(ROAD_X + ROAD_WIDTH, 0, 4, HEIGHT);

        // Lane markings
        g2.setColor(Color.WHITE);
        for (int y = roadOffset; y < HEIGHT; y += 60) {
            g2.fillRect(getLaneCenter(1) - LANE_WIDTH / 2, y, 4, 40);
            g2.fillRect(getLaneCenter(2) - LANE_WIDTH / 2, y, 4, 40);
        }

        // Skid marks
        g2.setColor(new Color(70, 70, 70, 180));
        for (Skid s : skids) {
            g2.fillRect(s.x, s.y, 4, 14);
        }

        // Player
        g2.drawImage(playerCar, (int) carX, carY,
                CAR_WIDTH, CAR_HEIGHT, this);

        // Enemies
        for (int i = 0; i < ENEMY_COUNT; i++) {
            int ex = getLaneCenter(enemyLane[i]) - CAR_WIDTH / 2;
            g2.drawImage(enemyCar, ex, enemyY[i],
                    CAR_WIDTH, CAR_HEIGHT, this);
        }

        // 💥 BOMB BLAST EXPLOSION
        if (exploding) {
            int size = explosionFrame * 14;
            int cx = (int) carX + CAR_WIDTH / 2;
            int cy = carY + CAR_HEIGHT / 2;

            int alpha = Math.max(0, 220 - explosionFrame * 12);

            // Smoke
            g2.setColor(new Color(60, 60, 60, alpha / 2));
            g2.fillOval(cx - size / 2, cy - size / 2, size, size);

            // Fire ring
            g2.setColor(new Color(255, 80, 0, alpha));
            g2.fillOval(cx - size / 3, cy - size / 3,
                    size * 2 / 3, size * 2 / 3);

            // Core flash
            g2.setColor(new Color(255, 230, 150, alpha));
            g2.fillOval(cx - size / 6, cy - size / 6,
                    size / 3, size / 3);
        }

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("Score: " + score, 10, 25);

        // Nitro bar
        g2.setColor(Color.GRAY);
        g2.fillRect(120, 10, 160, 10);
        g2.setColor(Color.CYAN);
        g2.fillRect(120, 10, nitro * 160 / 100, 10);
        g2.setColor(Color.WHITE);
        g2.drawRect(120, 10, 160, 10);

        if (gameOver) {
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.drawString("GAME OVER", 90, 260);
            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            g2.drawString("Press R to Restart", 115, 300);
        }

        g2.translate(0, -shakeY);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (gameOver && !exploding) return;

        double targetX = getLaneCenter(targetLane) - CAR_WIDTH / 2;
        double dx = targetX - carX;
        carX += dx * 0.15;

        if (Math.abs(dx) > 5) {
            addSkid((int) carX + 18);
            addSkid((int) carX + CAR_WIDTH - 22);
        }

        updateSkids();

        int speedBoost = 0;
        if (nitroOn && nitro > 0) {
            speedBoost = 4;
            nitro--;
        } else if (!nitroOn && nitro < 100) {
            nitro++;
        }

        roadOffset += roadSpeed + speedBoost;
        if (roadOffset > 60) roadOffset = 0;

        for (int i = 0; i < ENEMY_COUNT; i++) {
            enemyY[i] += enemySpeed + speedBoost;

            if (enemyY[i] > HEIGHT) {
                enemyY[i] = -300;
                enemyLane[i] = rand.nextInt(3);
                score++;
            }

            Rectangle player = new Rectangle(
                    (int) carX + 18, carY + 20,
                    HITBOX_WIDTH, HITBOX_HEIGHT
            );

            Rectangle enemy = new Rectangle(
                    getLaneCenter(enemyLane[i]) - HITBOX_WIDTH / 2,
                    enemyY[i] + 20,
                    HITBOX_WIDTH, HITBOX_HEIGHT
            );

            if (!exploding && player.intersects(enemy)) {
                exploding = true;
                explosionFrame = 1;
            }
        }

        if (exploding) {
            explosionFrame++;
            if (explosionFrame > 18) {
                exploding = false;
                gameOver = true;
                timer.stop();
            }
        }

        repaint();
    }

    void addSkid(int x) {
        Skid s = new Skid();
        s.x = x;
        s.y = carY + CAR_HEIGHT;
        skids.add(s);
    }

    void updateSkids() {
        skids.removeIf(s -> s.life-- <= 0);
        for (Skid s : skids) s.y += 4;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT && targetLane > 0) targetLane--;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT && targetLane < 2) targetLane++;
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) nitroOn = true;
        if (gameOver && e.getKeyCode() == KeyEvent.VK_R) startGame();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) nitroOn = false;
    }

    @Override public void keyTyped(KeyEvent e) {}
}

