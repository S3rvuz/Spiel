import javax.swing.*;  //GUI
import java.awt.*; //Grafik für Farben, rechtecke etc
import java.awt.event.*; //Tastatur inputs
import java.util.ArrayList; //Listen (Hindernisse und Coins)
import java.util.List;  //Gleiches
import java.util.Random; //Zufallzahlen für Spawnobjekte

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
           /* JFrame f = new JFrame("Jetpack Runner (Beginner)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);

            GamePanel panel = new GamePanel(900, 500);
            f.setContentPane(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            panel.start(); */

            //Fullscreen
            JFrame frame = new JFrame("Jetpack Spiel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(true); // Full

            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

            GamePanel panel = new GamePanel(screen.width, screen.height);
            frame.setContentPane(panel);

            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

            if (gd.isFullScreenSupported()) {
                gd.setFullScreenWindow(frame);
            } else {
                frame.setSize(screen);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }

            panel.start();
        });
    }

    static class GamePanel extends JPanel implements ActionListener, KeyListener {

        // ====== EASY TWEAK ZONE ======
        final int W, H;

        double gravity = 0.60;
        double thrust  = 1.10;
        double maxVy   = 10.0;

        double worldSpeed = 4.0;
        int groundMargin = 20;

        int obstacleEveryMsMin = 900;
        int obstacleEveryMsMax = 1300;
        int coinEveryMsMin = 600;
        int coinEveryMsMax = 1100;

        int playerW = 35, playerH = 45;
        int obstacleMinH = 30, obstacleMaxH = 140;
        int obstacleW = 18;
        int coinSize = 14;
        // =============================

        // FIX: explizit Swing Timer verwenden
        private javax.swing.Timer timer;

        private final Random rnd = new Random();

        private Player player;
        private final List<Obstacle> obstacles = new ArrayList<>();
        private final List<Coin> coins = new ArrayList<>();

        private Image playerImage;

        private boolean jetOn = false;
        private boolean gameOver = false;

        private long lastObstacleSpawn = 0;
        private long nextObstacleInMs = 0;

        private long lastCoinSpawn = 0;
        private long nextCoinInMs = 0;

        private long startTime = 0;
        private int score = 0;
        private int coinScore = 0;

        private Image loadImage(String path) {
            try {
                return javax.imageio.ImageIO.read(new java.io.File(path));
            } catch (Exception e) {
                System.out.println("Bild nicht gefunden: " + path);
                return null;
            }
        }

            //Konstruktor
        GamePanel(int w, int h) {
            this.W = w;
            this.H = h;

            setPreferredSize(new Dimension(W, H));
            setBackground(new Color(15, 18, 30));
            setFocusable(true);
            addKeyListener(this);

            resetGame();

            playerImage = loadImage("assets/Screenshot 2026-02-02 101301-Photoroom.png");

            // FIX: Timer ist javax.swing.Timer
            timer = new javax.swing.Timer(16, this); // ~60 FPS
        }

        void start() {
            requestFocusInWindow();
            timer.start();
        }

        private void resetGame() {
            player = new Player(120, H / 2.0, playerW, playerH);
            obstacles.clear();
            coins.clear();
            jetOn = false;
            gameOver = false;

            startTime = System.currentTimeMillis();
            score = 0;
            coinScore = 0;

            long now = System.currentTimeMillis();
            lastObstacleSpawn = now;
            nextObstacleInMs = randBetween(obstacleEveryMsMin, obstacleEveryMsMax);

            lastCoinSpawn = now;
            nextCoinInMs = randBetween(coinEveryMsMin, coinEveryMsMax);
        }

        private int randBetween(int a, int b) {
            return a + rnd.nextInt(Math.max(1, (b - a + 1)));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!gameOver) updateWorld();
            repaint();
        }

        private void updateWorld() {
            if (jetOn) player.vy -= thrust;
            player.vy += gravity;

            if (player.vy > maxVy) player.vy = maxVy;
            if (player.vy < -maxVy) player.vy = -maxVy;

            player.y += player.vy;

            if (player.y < 0) {
                player.y = 0;
                player.vy = 0;
            }
            double bottom = H - groundMargin - player.h;
            if (player.y > bottom) {
                player.y = bottom;
                player.vy = 0;
            }

            long now = System.currentTimeMillis();

            if (now - lastObstacleSpawn >= nextObstacleInMs) {
                spawnObstacle();
                lastObstacleSpawn = now;
                nextObstacleInMs = randBetween(obstacleEveryMsMin, obstacleEveryMsMax);
            }

            if (now - lastCoinSpawn >= nextCoinInMs) {
                spawnCoin();
                lastCoinSpawn = now;
                nextCoinInMs = randBetween(coinEveryMsMin, coinEveryMsMax);
            }

            for (Obstacle ob : obstacles) ob.x -= worldSpeed;
            for (Coin c : coins) c.x -= worldSpeed;

            obstacles.removeIf(ob -> ob.x + ob.w < 0);
            coins.removeIf(c -> c.x + c.size < 0 || c.collected);

            Rectangle pr = player.rect();
            for (Obstacle ob : obstacles) {
                if (pr.intersects(ob.rect())) {
                    gameOver = true;
                    return;
                }
            }

            for (Coin c : coins) {
                if (!c.collected && pr.intersects(c.rect())) {
                    c.collected = true;
                    coinScore += 10;
                }
            }

            long aliveMs = now - startTime;
            score = (int) (aliveMs / 50) + coinScore;
        }

        private void spawnObstacle() {
            int h = randBetween(obstacleMinH, obstacleMaxH);
            int y = randBetween(0, (H - groundMargin - h));
            obstacles.add(new Obstacle(W + 30, y, obstacleW, h));
        }

        private void spawnCoin() {
            int y = randBetween(0, H - groundMargin - coinSize);
            coins.add(new Coin(W + 30, y, coinSize));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            GradientPaint gp = new GradientPaint(0, 0, new Color(15, 18, 30), 0, H, new Color(10, 12, 22));
            g2.setPaint(gp);
            g2.fillRect(0, 0, W, H);
            g2.setPaint(null);

            // Ground
            g2.setColor(new Color(40, 45, 70));
            g2.fillRect(0, H - groundMargin, W, groundMargin);

            // Coins
            for (Coin c : coins) {
                g2.setColor(new Color(255, 210, 80));
                g2.fillOval((int) c.x, (int) c.y, c.size, c.size);
            }

            // Obstacles
            for (Obstacle ob : obstacles) {
                g2.setColor(new Color(255, 90, 110));
                g2.fillRoundRect((int) ob.x, (int) ob.y, ob.w, ob.h, 6, 6);
            }

            // Player
           // g2.setColor(new Color(90, 180, 255));
           // g2.fillRoundRect((int) player.x, (int) player.y, player.w, player.h, 10, 10);

            if (playerImage != null) {
                g2.drawImage(playerImage, (int) player.x, (int) player.y, player.w,player.h, null);
            } else {
                g2.setColor(new Color(90, 180, 255));
                g2.fillRoundRect((int) player.x, (int) player.y, player.w, player.h, 10, 10);
            }

            // Jet flame
            if (jetOn && !gameOver) {
                g2.setColor(new Color(255, 140, 60));
                int fx = (int) player.x - 1;
                int fy = (int) (player.y + player.h * 0.6);
                g2.fillOval(fx, fy, 10, 50);
            }

            // HUD
            g2.setColor(Color.RED);
            g2.setFont(new Font("Consolas", Font.BOLD, 16));
            g2.drawString("Score: " + score, 16, 26);
            g2.drawString("Coins: " + (coinScore / 10), 16, 48);
            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.drawString("SPACE = jetpack | R = restart", 16, 70);

            if (gameOver) {
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, 0, W, H);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, 34));
                String t = "GAME OVER";
                int tw = g2.getFontMetrics().stringWidth(t);
                g2.drawString(t, (W - tw) / 2, H / 2 - 20);

                g2.setFont(new Font("Consolas", Font.PLAIN, 18));
                String s1 = "Score: " + score;
                String s2 = "Press R to restart";
                int w1 = g2.getFontMetrics().stringWidth(s1);
                int w2 = g2.getFontMetrics().stringWidth(s2);
                g2.drawString(s1, (W - w1) / 2, H / 2 + 15);
                g2.drawString(s2, (W - w2) / 2, H / 2 + 45);
            }
        }

        @Override public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) jetOn = true;
            if (e.getKeyCode() == KeyEvent.VK_R) resetGame();
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) jetOn = false;
        }
    }

    static class Player {
        double x, y;
        int w, h;
        double vy = 0;

        Player(double x, double y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        Rectangle rect() {
            return new Rectangle((int) x, (int) y, w, h);
        }
    }

    static class Obstacle {
        double x, y;
        int w, h;

        Obstacle(double x, double y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        Rectangle rect() {
            return new Rectangle((int) x, (int) y, w, h);
        }
    }

    static class Coin {
        double x, y;
        int size;
        boolean collected = false;

        Coin(double x, double y, int size) {
            this.x = x; this.y = y; this.size = size;
        }

        Rectangle rect() {
            return new Rectangle((int) x, (int) y, size, size);
        }
    }
}
