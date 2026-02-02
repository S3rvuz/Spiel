
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class Spiel extends JPanel implements ActionListener, KeyListener {
    // Spielkonstanten
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_HEIGHT = 500;
    private static final int LANE_WIDTH = 150;
    private static final int NUM_LANES = 3;

    // Spieler
    private Player player;

    // Hindernisse und Münzen
    private ArrayList<Obstacle> obstacles;
    private ArrayList<Coin> coins;

    // Spielzustand
    private Timer timer;
    private boolean gameOver;
    private int score;
    private int distanceTraveled;
    private double gameSpeed;

    private Random random;

    public Spiel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // Himmelblau
        setFocusable(true);
        addKeyListener(this);

        initGame();

        timer = new Timer(20, this);
        timer.start();
    }

    private void initGame() {
        player = new Player();
        obstacles = new ArrayList<>();
        coins = new ArrayList<>();
        random = new Random();
        gameOver = false;
        score = 0;
        distanceTraveled = 0;
        gameSpeed = 5.0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // Spieler aktualisieren
        player.update();

        // Distanz und Geschwindigkeit erhöhen
        distanceTraveled++;
        if (distanceTraveled % 500 == 0) {
            gameSpeed += 0.5;
        }

        // Hindernisse spawnen
        if (random.nextInt(100) < 2) {
            int lane = random.nextInt(NUM_LANES);
            obstacles.add(new Obstacle(lane));
        }

        // Münzen spawnen
        if (random.nextInt(100) < 3) {
            int lane = random.nextInt(NUM_LANES);
            coins.add(new Coin(lane));
        }

        // Hindernisse aktualisieren
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obs = obstacles.get(i);
            obs.update(gameSpeed);

            // Kollision prüfen
            if (player.collidesWith(obs)) {
                gameOver = true;
            }

            // Entfernen wenn außerhalb des Bildschirms
            if (obs.y > HEIGHT) {
                obstacles.remove(i);
            }
        }

        // Münzen aktualisieren
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            coin.update(gameSpeed);

            // Münze einsammeln
            if (player.collidesWith(coin) && !coin.collected) {
                coin.collected = true;
                score += 10;
                coins.remove(i);
            } else if (coin.y > HEIGHT) {
                coins.remove(i);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Straße zeichnen
        drawRoad(g2d);

        // Münzen zeichnen
        for (Coin coin : coins) {
            coin.draw(g2d);
        }

        // Hindernisse zeichnen
        for (Obstacle obs : obstacles) {
            obs.draw(g2d);
        }

        // Spieler zeichnen
        player.draw(g2d);

        // UI zeichnen
        drawUI(g2d);

        // Game Over Bildschirm
        if (gameOver) {
            drawGameOver(g2d);
        }
    }

    private void drawRoad(Graphics2D g) {
        // Boden
        g.setColor(new Color(70, 70, 70));
        g.fillRect(0, GROUND_HEIGHT, WIDTH, HEIGHT - GROUND_HEIGHT);

        // Spurlinien
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{20}, 0));

        int startX = (WIDTH - NUM_LANES * LANE_WIDTH) / 2;
        for (int i = 1; i < NUM_LANES; i++) {
            int x = startX + i * LANE_WIDTH;
            g.drawLine(x, GROUND_HEIGHT, x, HEIGHT);
        }

        // Bürgersteig
        g.setColor(new Color(100, 100, 100));
        g.fillRect(0, GROUND_HEIGHT - 10, WIDTH, 10);
    }

    private void drawUI(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Score: " + score, 20, 30);
        g.drawString("Distance: " + distanceTraveled / 10 + "m", 20, 60);
        g.drawString("Speed: " + String.format("%.1f", gameSpeed), 20, 90);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        String gameOverText = "GAME OVER";
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(gameOverText);
        g.drawString(gameOverText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 50);

        g.setFont(new Font("Arial", Font.PLAIN, 24));
        String scoreText = "Final Score: " + score;
        textWidth = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, (WIDTH - textWidth) / 2, HEIGHT / 2);

        String restartText = "Press SPACE to restart";
        textWidth = g.getFontMetrics().stringWidth(restartText);
        g.drawString(restartText, (WIDTH - textWidth) / 2, HEIGHT / 2 + 50);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
            initGame();
            return;
        }

        if (!gameOver) {
            player.keyPressed(e);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!gameOver) {
            player.keyReleased(e);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // Spieler-Klasse
    class Player {
        private int lane;
        private int x, y;
        private int width = 40;
        private int height = 60;
        private boolean jumping;
        private double velocityY;
        private double gravity = 0.8;
        private int targetLane;
        private boolean movingLeft, movingRight;

        public Player() {
            lane = 1; // Mittlere Spur
            targetLane = lane;
            y = GROUND_HEIGHT - height;
            updateX();
        }

        private void updateX() {
            int startX = (WIDTH - NUM_LANES * LANE_WIDTH) / 2;
            x = startX + lane * LANE_WIDTH + LANE_WIDTH / 2 - width / 2;
        }

        public void update() {
            // Seitenbewegung
            if (movingLeft && lane > 0) {
                lane--;
                targetLane = lane;
                movingLeft = false;
                updateX();
            }
            if (movingRight && lane < NUM_LANES - 1) {
                lane++;
                targetLane = lane;
                movingRight = false;
                updateX();
            }

            // Sprung-Physik
            if (jumping) {
                velocityY += gravity;
                y += velocityY;

                if (y >= GROUND_HEIGHT - height) {
                    y = GROUND_HEIGHT - height;
                    jumping = false;
                    velocityY = 0;
                }
            }
        }

        public void jump() {
            if (!jumping) {
                jumping = true;
                velocityY = -15;
            }
        }

        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    movingLeft = true;
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    movingRight = true;
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                case KeyEvent.VK_SPACE:
                    jump();
                    break;
            }
        }

        public void keyReleased(KeyEvent e) {}

        public boolean collidesWith(Obstacle obs) {
            Rectangle playerRect = new Rectangle(x, y, width, height);
            Rectangle obsRect = new Rectangle(obs.x, obs.y, obs.width, obs.height);
            return playerRect.intersects(obsRect);
        }

        public boolean collidesWith(Coin coin) {
            Rectangle playerRect = new Rectangle(x, y, width, height);
            Rectangle coinRect = new Rectangle(coin.x, coin.y, coin.size, coin.size);
            return playerRect.intersects(coinRect);
        }

        public void draw(Graphics2D g) {
            // Körper
            g.setColor(new Color(255, 100, 100));
            g.fillRect(x, y, width, height);

            // Kopf
            g.setColor(new Color(255, 200, 150));
            g.fillOval(x + 5, y - 15, 30, 30);

            // Augen
            g.setColor(Color.BLACK);
            g.fillOval(x + 12, y - 8, 5, 5);
            g.fillOval(x + 23, y - 8, 5, 5);

            // Umrandung
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawRect(x, y, width, height);
            g.drawOval(x + 5, y - 15, 30, 30);
        }
    }

    // Hindernis-Klasse
    class Obstacle {
        int lane;
        int x, y;
        int width = 50;
        int height = 50;
        Color color;

        public Obstacle(int lane) {
            this.lane = lane;
            int startX = (WIDTH - NUM_LANES * LANE_WIDTH) / 2;
            x = startX + lane * LANE_WIDTH + LANE_WIDTH / 2 - width / 2;
            y = -height;
            color = new Color(random.nextInt(100) + 100, random.nextInt(50), random.nextInt(50));
        }

        public void update(double speed) {
            y += speed;
        }

        public void draw(Graphics2D g) {
            g.setColor(color);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawRect(x, y, width, height);

            // X-Muster für Gefahr
            g.drawLine(x, y, x + width, y + height);
            g.drawLine(x + width, y, x, y + height);
        }
    }

    // Münz-Klasse
    class Coin {
        int lane;
        int x, y;
        int size = 30;
        boolean collected = false;

        public Coin(int lane) {
            this.lane = lane;
            int startX = (WIDTH - NUM_LANES * LANE_WIDTH) / 2;
            x = startX + lane * LANE_WIDTH + LANE_WIDTH / 2 - size / 2;
            y = -size;
        }

        public void update(double speed) {
            y += speed;
        }

        public void draw(Graphics2D g) {
            g.setColor(new Color(255, 215, 0));
            g.fillOval(x, y, size, size);
            g.setColor(new Color(218, 165, 32));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x, y, size, size);

            // Dollar-Symbol
            g.setColor(new Color(218, 165, 32));
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("$", x + 9, y + 22);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Endless Runner");
            Spiel game = new Spiel();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
        });
    }
}