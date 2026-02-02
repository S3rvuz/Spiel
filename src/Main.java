// =======================
// IMPORTS
// =======================

// Swing = GUI (Fenster, Buttons, Zeichnen)
import javax.swing.*;

// Grafik (Farben, Zeichnen, Rechtecke)
import java.awt.*;

// Tastatur, Events, Timer-Events
import java.awt.event.*;

// Listen (für Hindernisse, Coins)
import java.util.ArrayList;
import java.util.List;

// Zufallszahlen
import java.util.Random;


// =======================
// HAUPTKLASSE
// =======================
public class Main {

    // Einstiegspunkt des Programms
    public static void main(String[] args) {

        // GUI immer im Swing-Thread starten (Standard-Regel)
        SwingUtilities.invokeLater(() -> {

            // Fenster erstellen
            JFrame f = new JFrame("Jetpack Runner");

            // Programm beenden, wenn Fenster geschlossen wird
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Fenstergröße nicht veränderbar
            f.setResizable(false);

            // Unser Spielfeld (GamePanel) erstellen
            GamePanel panel = new GamePanel(900, 500);

            // Panel ins Fenster setzen
            f.setContentPane(panel);

            // Fenstergröße an Panel anpassen
            f.pack();

            // Fenster zentrieren
            f.setLocationRelativeTo(null);

            // Fenster anzeigen
            f.setVisible(true);

            // Spiel starten
            panel.start();
        });
    }


    // =======================
    // SPIELFELD
    // =======================
    static class GamePanel extends JPanel
            implements ActionListener, KeyListener {

        // Breite & Höhe des Fensters
        final int W, H;

        // -----------------------
        // SPIEL-PARAMETER
        // -----------------------

        // Schwerkraft (zieht Spieler nach unten)
        double gravity = 0.6;

        // Jetpack-Schub nach oben
        double thrust = 1.1;

        // Maximale Geschwindigkeit
        double maxVy = 10.0;

        // Geschwindigkeit der Welt (Scrolling)
        double worldSpeed = 4.0;

        // Boden-Rand (nur Optik)
        int groundMargin = 20;

        // -----------------------
        // OBJEKT-GRÖSSEN
        // -----------------------
        int playerW = 35, playerH = 45;
        int obstacleW = 18;
        int obstacleMinH = 30, obstacleMaxH = 140;
        int coinSize = 14;

        // -----------------------
        // SPIEL-OBJEKTE
        // -----------------------

        // Swing-Timer (ruft ~60x pro Sekunde actionPerformed auf)
        javax.swing.Timer timer;

        // Zufallsgenerator
        Random rnd = new Random();

        // Spieler
        Player player;

        // Listen für Hindernisse & Coins
        List<Obstacle> obstacles = new ArrayList<>();
        List<Coin> coins = new ArrayList<>();

        // Steuerung / Status
        boolean jetOn = false;
        boolean gameOver = false;

        // Zeit & Punkte
        long startTime;
        int score = 0;
        int coinScore = 0;


        // =======================
        // KONSTRUKTOR
        // =======================
        GamePanel(int w, int h) {
            this.W = w;
            this.H = h;

            // Panel-Größe
            setPreferredSize(new Dimension(W, H));

            // Hintergrundfarbe
            setBackground(Color.BLACK);

            // Tastatur erlauben
            setFocusable(true);
            addKeyListener(this);

            // Spiel zurücksetzen
            resetGame();

            // Timer: alle 16 ms -> ca. 60 FPS
            timer = new javax.swing.Timer(16, this);
        }

        // Spiel starten
        void start() {
            requestFocusInWindow(); // Fokus für Tastatur
            timer.start();          // Timer starten
        }

        // =======================
        // SPIEL RESET
        // =======================
        void resetGame() {

            // Spieler in die Mitte setzen
            player = new Player(120, H / 2.0, playerW, playerH);

            // Listen leeren
            obstacles.clear();
            coins.clear();

            // Status zurücksetzen
            jetOn = false;
            gameOver = false;

            startTime = System.currentTimeMillis();
            score = 0;
            coinScore = 0;
        }

        // =======================
        // TIMER CALLBACK
        // =======================
        @Override
        public void actionPerformed(ActionEvent e) {

            // Nur updaten, wenn nicht Game Over
            if (!gameOver) {
                updateWorld();
            }

            // Neu zeichnen
            repaint();
        }

        // =======================
        // SPIEL-LOGIK
        // =======================
        void updateWorld() {

            // Jetpack zieht nach oben
            if (jetOn) player.vy -= thrust;

            // Schwerkraft zieht nach unten
            player.vy += gravity;

            // Geschwindigkeit begrenzen
            player.vy = Math.max(-maxVy, Math.min(maxVy, player.vy));

            // Position ändern
            player.y += player.vy;

            // Grenzen oben/unten
            if (player.y < 0) player.y = 0;
            if (player.y > H - groundMargin - player.h)
                player.y = H - groundMargin - player.h;

            // Hindernisse bewegen
            for (Obstacle o : obstacles) o.x -= worldSpeed;

            // Coins bewegen
            for (Coin c : coins) c.x -= worldSpeed;

            // Kollision prüfen
            Rectangle pr = player.rect();

            for (Obstacle o : obstacles) {
                if (pr.intersects(o.rect())) {
                    gameOver = true;
                }
            }

            for (Coin c : coins) {
                if (!c.collected && pr.intersects(c.rect())) {
                    c.collected = true;
                    coinScore += 10;
                }
            }

            // Score = Zeit + Coins
            score = (int)((System.currentTimeMillis() - startTime) / 50)
                    + coinScore;
        }

        // =======================
        // ZEICHNEN
        // =======================
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            // Spieler
            g2.setColor(Color.CYAN);
            g2.fillRect((int) player.x, (int) player.y,
                    player.w, player.h);

            // Hindernisse
            g2.setColor(Color.RED);
            for (Obstacle o : obstacles) {
                g2.fillRect((int) o.x, (int) o.y, o.w, o.h);
            }

            // Coins
            g2.setColor(Color.YELLOW);
            for (Coin c : coins) {
                if (!c.collected)
                    g2.fillOval((int) c.x, (int) c.y, c.size, c.size);
            }

            // Text
            g2.setColor(Color.WHITE);
            g2.drawString("Score: " + score, 10, 20);

            if (gameOver) {
                g2.drawString("GAME OVER - R zum Neustart",
                        W / 2 - 80, H / 2);
            }
        }

        // =======================
        // TASTATUR
        // =======================
        @Override public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {

            // Leertaste -> Jetpack an
            if (e.getKeyCode() == KeyEvent.VK_SPACE)
                jetOn = true;

            // R -> Neustart
            if (e.getKeyCode() == KeyEvent.VK_R)
                resetGame();
        }

        @Override
        public void keyReleased(KeyEvent e) {

            // Leertaste loslassen -> Jetpack aus
            if (e.getKeyCode() == KeyEvent.VK_SPACE)
                jetOn = false;
        }
    }


    // =======================
    // SPIELER
    // =======================
    static class Player {
        double x, y;
        int w, h;
        double vy = 0;

        Player(double x, double y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        Rectangle rect() {
            return new Rectangle((int)x, (int)y, w, h);
        }
    }

    // =======================
    // HINDERNIS
    // =======================
    static class Obstacle {
        double x, y;
        int w, h;

        Obstacle(double x, double y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        Rectangle rect() {
            return new Rectangle((int)x, (int)y, w, h);
        }
    }

    // =======================
    // COIN
    // =======================
    static class Coin {
        double x, y;
        int size;
        boolean collected = false;

        Coin(double x, double y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }

        Rectangle rect() {
            return new Rectangle((int)x, (int)y, size, size);
        }
    }
}
