import javax.swing.*;  //GUI
import java.awt.*; //Grafik für Farben, rechtecke etc
import java.awt.event.*; //Tastatur inputs
import java.util.ArrayList; //Listen (Hindernisse und Coins)
import java.util.List;  //Gleiches
import java.util.Random; //Zufallzahlen für Spawnobjekte
import javax.sound.sampled.*;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
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

        private void drawMenu(Graphics2D g2) {
           /* g2.setColor(Color.RED);
            g2.fillRect(0, 0, WIDTH, HEIGHT); */
            if (menuBg != null) {
                drawCover(g2, menuBg, 0, 0, W, H);
            } else {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, W, H);
            }
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(0, 0, W, H);

            g2.setFont(new Font("Consolas", Font.BOLD, 52));
            g2.setColor(Color.WHITE);
            String title = "Auf dem Sitz das Spiel!";
            int tw = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, (W - tw) / 2, 160);

            g2.setFont(new Font("Consolas", Font.PLAIN,26));
            int startY = 260;
            int gap = 50;

            for (int i = 0; i < meniItems.length; i++) {
                boolean selected = (i == menuIndex);
                g2.setColor(selected ? Color.GREEN : Color.RED);
                String line = (selected ? "> " : " ") + meniItems[i];
                int lw = g2.getFontMetrics().stringWidth(line);
                g2.drawString(line, (W - lw) / 2, startY + i * gap);
            }

            g2.setFont(new Font("Consolas", Font.PLAIN,16));
            g2.setColor(Color.WHITE);
            String hint = "Up/Down = Auswahl | ENTER = OK";
            int hw = g2.getFontMetrics().stringWidth(hint);
            g2.drawString(hint, (W - hw) / 2, H - 80);

        }
        private void drawHighscoreScreen(Graphics2D g2) {
            g2.setColor(Color.RED);
            g2.fillRect(0, 0, W, H);

            g2.setFont(new Font("consolas", Font.BOLD, 48));
            g2.setColor(Color.WHITE);
            String t = "HIGHSCORE";
            int tw = g2.getFontMetrics().stringWidth(t);
            g2.drawString(t, (W - tw) / 2, 180);

            g2.setFont(new Font("consolas", Font.BOLD,34));
            String hs = String.valueOf(highscore);
            int hw = g2.getFontMetrics().stringWidth(hs);
            g2.drawString(hs, (W - hw) / 2, 260);

            g2.setFont(new Font("Consolas", Font.PLAIN, 18));
            String back = "ESC oder BACKSPACE = zurück";
            int bw = g2.getFontMetrics().stringWidth(back);
            g2.drawString(back, (W - bw) / 2, H - 80);
        }

        private void drawCharacterScreen(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, W, H);

            g2.setFont(new Font("Consolas", Font.BOLD, 42));
            g2.setColor(Color.WHITE);
            String t = "CHARAKTERWAHL";
            int tw = g2.getFontMetrics().stringWidth(t);
            g2.drawString(t, (W - tw) / 2, 180);

            g2.setFont(new Font("Consolas", Font.PLAIN, 20));
            g2.setColor(Color.LIGHT_GRAY);
            String info = "koks"; //Muss noch gemacht werden
            int iw = g2.getFontMetrics().stringWidth(info);
            g2.drawString(info, (W - iw) / 2, 250);

            String back = "ESC = zurück";
            int bw = g2.getFontMetrics().stringWidth(back);
            g2.drawString(back, (W - bw) / 2, H - 80);
        }


        enum GameState{MENU, RUNNING, HIGHSCORE, CHARACTER}
        private GameState state = GameState.MENU;

        private String[] meniItems = {"Spiel starten", "Highscore", "Character wählen", "Spiel beenden"};

        private int menuIndex = 0;

        //RAM Variante
        private int highscore = 0;

        private long gameOverEndMs = 0;


        final int W, H;

        double gravity = 0.30;
        double thrust  = 0.60;
        double maxVy   = 6.0;

        //Worldspeed========================
        double baseWorldSpeed = 4.0; //startgeschwindigkeit
        double speedstep = 1; //+1 Geschwindigkeit pro 1000
        double maxWorldSpeed = 1400.0;
        double worldSpeed = baseWorldSpeed;

        int groundMargin = 20;

        int obstacleEveryMsMin = 500; // Anzahl Hindernisse!
        int obstacleEveryMsMax = 900;

        int fuelEveryMsMin = 1700; //Anzahl Pancakes
        int fuelEveryMsMax = 2800;

        int playerW = 115, playerH = 135;
        int obstacleMinH = 30, obstacleMaxH = 140;
        int obstacleW = 18;
        int fuelSize = 66;
        // =============================

        // FIX: explizit Swing Timer verwenden
        private javax.swing.Timer timer;

        private final Random rnd = new Random();
        private Player player;
        private final List<Obstacle> obstacles = new ArrayList<>();
        private final List<Fuel> fuelPickups = new ArrayList<>();
        private final List<ScoreCoin> scoreCoins = new ArrayList<>();

        //Münzen
        int scoreCoinEveryMsMin = 2000; // Anzahl Münzen
        int scoreCoinEveryMsMax = 2800;

        private long lastScoreCoinSpawn = 0;
        private long nextScoreCoinInMs = 0;

        private Image scoreCoinImage;
        int scoreCoinSize = 50;
        int scoreCoinPoints = 10;

        //Bilder
        private Image playerImageNormal; //Player pic normal
        private Image playerImagePower; //powerUp
        private Image fuelImage;
        private Image[] bg = new Image[3];
        private double[] bgX = new double[3];
        private Image[] obstacleImgs = new Image[3]; //Hindernisse
        private Image menuBg;

        private Clip coinSound; //Münzensound
        private Clip fuelSound; //Mamf
        private Clip unsichtbarmusic;
        private Clip bgMusic;
        private Clip jetpacksound;
        private Clip deathSound;

        private boolean totAnim = false;
        private boolean deathSoundPlayed = false;

        private double deathFallboost = 1.4;

        private boolean jetpacksoundPlaying = false;

        private double bgSpeedFactor = 0.5;

        private boolean jetOn = false;
        private boolean gameOver = false;

        //Raketen
        private void spawnRocket() {
            int y = randBetween(0, H - groundMargin - raketeH);

            double speed = raketenGeschwindigkeitMin + rnd.nextDouble() * (raketenGeschwindigkeitMax - raketenGeschwindigkeitMin);

            double x = W + 50;

            raketen.add(new Rakete(x, y, raketeW, raketeH, -speed));
        }

        private void activeStoredPowerUp() {
            if (storedPowerUp == null) return;

            long now = System.currentTimeMillis();

            if (storedPowerUp.equals("Unsichtbar")) {
                unsichtbarAktiv = true;
                unsichtbarEndeMs = System.currentTimeMillis() + 5000; //5 Sekunden -> dauer vom Unsuchtbar

               stopSound(bgMusic);
                playLoop(unsichtbarmusic);
            }

            if (storedPowerUp.equals("Magnet")) {
                magnetAktiv = true;
                magnetEndeMs = now + 10000; //Dauer vom Magnet
            }

            if (storedPowerUp.equals("DoppelteMünzen")) {
                doubleCoinsAktiv = true;
                doubleCoinsEndeMs = now + 15000; //Dauer 15 Sek Doppelte Münzen
            }

            if (storedPowerUp.equals("UnendlichBenzin")) {
                infiniteFuelAktiv = true;
                infiniteFuelEndeMs = now + 10000; //Dauer 10 Sek Unendlich Benin
            }

            storedPowerUp = null;

        }

        private void applyMagnet(double px, double py, double dt) {
            //Zielpunkt
            double targetX = px + player.w / 2;
            double targetY = py + player.h / 2;
            //Pancakes
            for (Fuel f : fuelPickups) {
                if (f.collected) continue;

                double cx = f.x + f.size / 2;
                double cy = f.y + f.size / 2;

                double dx = targetX - cx;
                double dy = targetY - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist > 0 && dist <= magnetRange) {
                    double nx = dx / dist;
                    double ny = dy / dist;

                    f.x += nx * magnetPull;
                    f.y += ny * magnetPull;
                }
            }
            //Coins
            for (ScoreCoin sc : scoreCoins) {
                if (sc.collected) continue;

                double cx = sc.x + sc.size / 2;
                double cy = sc.y + sc.size / 2;

                double dx = targetX - cx;
                double dy = targetY - cy;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist > 0 && dist <= magnetRange) {
                    double nx = dx / dist;
                    double ny = dy / dist;

                    sc.x += nx * magnetPull;
                    sc.y += ny * magnetPull;
                }
            }
        }

        //Rakete
        private final List<Rakete> raketen = new ArrayList<>();

        int raketeJedeMsMin = 700; //Spawnrate
        int raketeJedeMsMax = 1400;

        private long letzteRaketeSpawn = 0;
        private long nächsteRaketeInMs = 0;

        double raketenGeschwindigkeitMin = 8.0;
        double raketenGeschwindigkeitMax = 14.0;

        int raketeW = 80; //Raketen breite
        int raketeH = 80; //höhe

        private Image raketenImage;

        private long lastObstacleSpawn = 0;
        private long nextObstacleInMs = 0;

        private long lastFuelSpawn = 0;
        private long nextFuelInMs = 0;

        private long startTime = 0;
        private int score = 0;
        private int coinScore = 0;

        private double benzin;
        private double voll = 100.0;

        private double fueldrain = 8; //Benzin verlieren

        private double benzinprogas = 20;

        private long lastTickMs = 0;

        //Powerups!!!===========================================
        private String storedPowerUp = null;
        //Unsichtbar
        private boolean unsichtbarAktiv = false;
        private long unsichtbarEndeMs = 0;

        private int coinsGesammelt = 0;
        private int nächsterpowerUpAb = 10; // Münz check ab 10 20 usw

        private boolean magnetAktiv = false;
        private long magnetEndeMs = 0;

        //Magnet settings!!!
        private double magnetRange = 800; //Reichweite
        private double magnetPull = 10.0; //Ziehstärkeeö

        // Double Coins!
        private boolean doubleCoinsAktiv = false;
        private long doubleCoinsEndeMs = 0;

        //Unendlich PanCAKES!!!
        private boolean infiniteFuelAktiv = false;
        private long infiniteFuelEndeMs = 0;
//====================================================================




        private void drawCover(Graphics2D g2, Image img, int x, int y, int w, int h) {
            int iw = img.getWidth(null);
            int ih = img.getHeight(null);
            if (iw <= 0 || ih <= 0) return;

            double scale = Math.max((double) w / iw, (double) h / ih);
            int dw = (int) (iw * scale);
            int dh = (int) (ih * scale);

            int dx = x + (w - dw) / 2;
            int dy = y + (h - dh) / 2;

            g2.drawImage(img, dx, dy, dw, dh, null);
        }

        private Image loadImage(String path) {
            try {
                return javax.imageio.ImageIO.read(new java.io.File(path));
            } catch (Exception e) {
                System.out.println("Bild nicht gefunden: " + path);
                return null;
            }
        }

        private void playSound(Clip clip) {
            if (clip == null) return;
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();

        }

        private Clip loadSound(String path) {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                return clip;
            } catch (Exception e) {
                System.out.println("Sound nicht geladen: " + path);
                return null;
            }
        }

        private void playLoop(Clip clip) {
            if (clip == null) return;
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        private void stopSound(Clip clip) {
            if (clip == null) return;
            clip.stop();
            clip.setFramePosition(0);
        }

        private void startJetpackSound() {
            if (jetpacksound == null) return;
            if (jetpacksoundPlaying) return;

            jetpacksound.setFramePosition(0);
            jetpacksound.loop(Clip.LOOP_CONTINUOUSLY);
            jetpacksoundPlaying = true;
        }

        private void stopJetpackSound() {
            if (jetpacksound == null) return;
            if (!jetpacksoundPlaying) return;

            jetpacksound.stop();
            jetpacksound.setFramePosition(0);
            jetpacksoundPlaying = false;
        }

        private void gehZurück() {
            totAnim = false;
            deathSoundPlayed = false;
            state = GameState.MENU;

            gameOver = false;
            jetOn = false;
            stopJetpackSound();
            stopSound(bgMusic);
            stopSound(unsichtbarmusic);

            obstacles.clear();
            fuelPickups.clear();
            scoreCoins.clear();
            raketen.clear();
        }

        private void triggerGameOver() {
            if (gameOver) return;

            gameOver = true;
            gameOverEndMs = System.currentTimeMillis() + 5000; //Länge vom Gameover screen

            highscore = Math.max(highscore, score);

            jetOn = false;
            stopJetpackSound();
            stopSound(bgMusic);
            stopSound(unsichtbarmusic);

            totAnim = true;
            deathSoundPlayed = false;

            player.vy = 2.0; //fallgeschwindigkeit
        }

        //Konstruktor
        GamePanel(int w, int h) {
            this.W = w;
            this.H = h;

            setPreferredSize(new Dimension(W, H));
            setBackground(new Color(15, 18, 30));
            setFocusable(true);
            addKeyListener(this);


            playerImageNormal = loadImage("assets/Screenshot 2026-02-02 101301-Photoroom.png"); //Player pic
            playerImagePower = new ImageIcon("assets/Power.gif").getImage();

            fuelImage = loadImage("assets/Pancakes.png"); //Benzin picture

            //Münzen bild
            scoreCoinImage = loadImage("assets/Münze.png");

            bg[0] = loadImage("assets/ost_ost.png"); //Hintergünde
            bg[1] = loadImage("assets/ost_ost.png");
            bg[2] = loadImage("assets/ost_ost.png");

            menuBg = loadImage("assets/MenuBg.png");


            //Raketen bild
            raketenImage = loadImage("assets/Rocket_card_render_1.png");

            obstacleImgs[0] = loadImage("assets/Rechner.png");
            obstacleImgs[1] = null;
            obstacleImgs[2] = null; //Hindernisse bild

            coinSound = loadSound("assets/coin.wav"); //Coinsound
            fuelSound = loadSound("assets/pancake.wav"); //Mamf
            unsichtbarmusic = loadSound("assets/PowerUpNeu.wav"); //PowerUp
            bgMusic = loadSound("assets/BackgroundBanger.wav");
            jetpacksound = loadSound("assets/Jetpack.wav");
            deathSound = loadSound("assets/GameOver.wav");

            resetGame();

            // FIX: Timer ist javax.swing.Timer
            timer = new javax.swing.Timer(16, this); // ~60 FPS
        }

        void start() {
            requestFocusInWindow();
            timer.start();
            repaint();
        }

        private void resetGame() {
            totAnim = false;
            deathSoundPlayed = false;
            stopJetpackSound();
            obstacleEveryMsMin = 500;
            obstacleEveryMsMax = 900;


            player = new Player(120, H / 2.0, playerW, playerH);
            obstacles.clear();
            fuelPickups.clear();
            scoreCoins.clear();
            jetOn = false;
            gameOver = false;
            worldSpeed = baseWorldSpeed;

            startTime = System.currentTimeMillis();
            score = 0;
            coinScore = 0;

            storedPowerUp = null;

            unsichtbarAktiv = false;
            unsichtbarEndeMs = 0;

            magnetAktiv = false;
            magnetEndeMs = 0;

            stopSound(unsichtbarmusic);
            stopSound(bgMusic);
            playLoop(bgMusic);

            coinsGesammelt = 0;
            nächsterpowerUpAb = 1; // Anzahl der benötigten Münzen

            long now = System.currentTimeMillis();
            lastObstacleSpawn = now;
            nextObstacleInMs = randBetween(obstacleEveryMsMin, obstacleEveryMsMax);

            lastFuelSpawn = now;
            nextFuelInMs = randBetween(fuelEveryMsMin, fuelEveryMsMax);

            benzin = 60.0; //60% startleben
            voll = 100;

            lastScoreCoinSpawn = now;
            nextScoreCoinInMs = randBetween(scoreCoinEveryMsMin, scoreCoinEveryMsMax);


            lastTickMs = System.currentTimeMillis();

            bgX[0] = 0;
            bgX[1] = W;
            bgX[2] = 2 * W;

            raketen.clear();

            letzteRaketeSpawn = now;
            nächsteRaketeInMs = randBetween(raketeJedeMsMin, raketeJedeMsMax);
        }

        private int randBetween(int a, int b) {
            return a + rnd.nextInt(Math.max(1, (b - a + 1)));
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (state == GameState.RUNNING && !gameOver) {
                updateWorld();
            }

            if (state == GameState.RUNNING && gameOver) {
                updateDeathAnimation();
            }

            if (gameOver && System.currentTimeMillis() >= gameOverEndMs) {
                gehZurück();
                repaint();
                return;
            }
            if (state == GameState.RUNNING && !gameOver) {
                updateWorld();
            }
            repaint();
        }

        private void updateWorld() {

            long now = System.currentTimeMillis();
            double dt = (now - lastTickMs) /  1000.0;
            lastTickMs = now;

            //1. Powerups Uncihtbar!!
            if (unsichtbarAktiv && now >= unsichtbarEndeMs) {
                unsichtbarAktiv = false;
                stopSound(unsichtbarmusic);
                if (!gameOver) playLoop(bgMusic);
            }

            //Magnet endet
            if (magnetAktiv && now >= magnetEndeMs) {
                magnetAktiv = false;
            }

            if (doubleCoinsAktiv && now >= doubleCoinsEndeMs) {
                doubleCoinsAktiv = false;
            }

            if (infiniteFuelAktiv && now >= infiniteFuelEndeMs) {
                infiniteFuelAktiv = false;
            }

            if (dt < 0) dt = 0;
            if (dt > 0.1) dt = 0.1;

            boolean kanister = jetOn && benzin > 0;

            if (!gameOver && kanister) startJetpackSound();
            else stopJetpackSound();

            if (kanister) {
                player.vy -= thrust;

                /*benzin -= fueldrain * dt;
                if (benzin < 0) benzin = 0; */

                if (!infiniteFuelAktiv) {
                    benzin -= fueldrain * dt;
                    if (benzin < 0) benzin = 0;
                }
            }
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
            double bgSpeed = worldSpeed * bgSpeedFactor;

            for(int i = 0; i < 3; i++) {
                bgX[i] -= bgSpeed;

                if(bgX[i] <= -W) {
                    bgX[i] += 3 * W;
                }
            }

            if (now - lastObstacleSpawn >= nextObstacleInMs) {
                spawnObstacle();
                lastObstacleSpawn = now;
                nextObstacleInMs = randBetween(obstacleEveryMsMin, obstacleEveryMsMax);
            }

            if (now - lastFuelSpawn >= nextFuelInMs) {
                spawnFuel();
                lastFuelSpawn = now;
                nextFuelInMs = randBetween(fuelEveryMsMin, fuelEveryMsMax);
            }

            if (now - lastScoreCoinSpawn >= nextScoreCoinInMs) {
                spawnScoreCoin();
                lastScoreCoinSpawn = now;
                nextScoreCoinInMs = randBetween(scoreCoinEveryMsMin, scoreCoinEveryMsMax);
            }

            for (Obstacle ob : obstacles) ob.x -= worldSpeed;

            for (Fuel f : fuelPickups) f.x -= worldSpeed;

            for (ScoreCoin sc : scoreCoins) sc.x -= worldSpeed; // Münzen bewegung

            for (Rakete r : raketen) r.x += (r.vx - worldSpeed);

            //PowerUp Magnet
            if (magnetAktiv) {
                applyMagnet(player.x, player.y, dt);
            }



            obstacles.removeIf(ob -> ob.x + ob.w < 0);
            fuelPickups.removeIf(f -> f.x + f.size < 0 || f.collected);
            scoreCoins.removeIf(sc -> sc.x + sc.size < 0 || sc.collected);
            raketen.removeIf(r -> r.x + r.w < 0);

            Rectangle pr = player.rect();
            for (Obstacle ob : obstacles) {
                if (pr.intersects(ob.rect())) {
                    if (!unsichtbarAktiv) {
                  /*  gameOver = true;
                    highscore = Math.max(highscore, score);
                    stopJetpackSound();
                    stopSound(bgMusic);
                    stopSound(unsichtbarmusic);
                    return; */
                    highscore = Math.max(highscore, score);
                    triggerGameOver();
                    return;
                    }
                }
            }

            for (Rakete r : raketen) {
                if (pr.intersects(r.rect())) {
                    if (!unsichtbarAktiv) {
                    /*gameOver = true;
                    highscore = Math.max(highscore, score);
                    stopJetpackSound();
                    stopSound(bgMusic);
                    stopSound(unsichtbarmusic);
                    return; */
                        highscore = Math.max(highscore, score);
                        triggerGameOver();
                        return;
                    }
                }
            }

            for (Fuel f : fuelPickups) {
                if (!f.collected && pr.intersects(f.rect())) {
                    f.collected = true;

                    benzin += benzinprogas;
                    if (benzin > voll) benzin = voll;

                    playSound(fuelSound);


                }

                if (now - letzteRaketeSpawn >= nächsteRaketeInMs) {
                    spawnRocket();
                    letzteRaketeSpawn = now;
                    nächsteRaketeInMs = randBetween(raketeJedeMsMin, raketeJedeMsMax);
                }
            }
            //Coins einsammeln amk
            for (ScoreCoin sc : scoreCoins) {
                if (!sc.collected && pr.intersects(sc.rect())) {
                    sc.collected = true;

                    int gain = scoreCoinPoints;
                    if (doubleCoinsAktiv) gain *= 2;

                    coinScore += gain;
                    coinsGesammelt++;

                   /* coinsGesammelt++;
                    coinScore += scoreCoinPoints;
                    checkPowerUpDrop(); */

                    checkPowerUpDrop();
                    playSound(coinSound);
                }
            }

            long aliveMs = now - startTime;
            score = (int) (aliveMs / 50) + coinScore;

            int level = score / 500; //Alle 500 Score wirds schneller
            worldSpeed = Math.min(maxWorldSpeed, baseWorldSpeed + level * speedstep);

            obstacleEveryMsMin = Math.max(250, 500 - level * 20);
            obstacleEveryMsMax = Math.max(450, 900 + level * 25);
        }

        private void updateDeathAnimation() {
            player.x -= 5.5;
            if (!totAnim) return;

            if (!deathSoundPlayed) {
                playSound(deathSound);
                deathSoundPlayed = true;
            }

            player.vy += gravity * deathFallboost;
            if (player.vy > 25) player.vy = 25;

            player.y += player.vy;

            if (player.y > H + 300) {
                totAnim = false;
            }
        }

        private void spawnObstacle() {
            int h = randBetween(obstacleMinH, obstacleMaxH);
            int y = randBetween(0, (H - groundMargin - h));

            int imgIdx = 0;

            obstacles.add(new Obstacle(W + 30, y, obstacleW, h, imgIdx));
        }

        private void spawnFuel() {
            int y = randBetween(0, H - groundMargin - fuelSize);
            fuelPickups.add(new Fuel(W + 30, y, fuelSize));
        }

        private void checkPowerUpDrop() {
            if (storedPowerUp != null) return;

            if (coinsGesammelt >= nächsterpowerUpAb) {
                nächsterpowerUpAb += 10; //Alle 10 Münzen ein Powerup

                int chance = 100; //35 ist dann richtig
                if (rnd.nextInt(100) < chance) {


                    int r = rnd.nextInt(4);
                    if (r == 0) storedPowerUp = "Unsichtbar";
                    else if (r == 1) storedPowerUp = "Magnet";
                    else if (r == 2) storedPowerUp = "DoppelteMünzen";
                    else if (r == 3) storedPowerUp = "UnendlichBenzin";
                }
            }
        }

        private void spawnScoreCoin() {
            int y = randBetween(0, H - groundMargin - scoreCoinSize);
            scoreCoins.add(new ScoreCoin(W + 30, y, scoreCoinSize));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            if (state == GameState.MENU) {
                drawMenu(g2);
                return;
            }
            if (state == GameState.HIGHSCORE) {
                drawHighscoreScreen(g2);
                return;
            }
            if (state == GameState.CHARACTER) {
                drawCharacterScreen(g2);
                return;
            }

            if (bg[0] != null && bg[1] != null && bg[2] != null) {
                for (int i = 0; i < 3; i++) {
                    drawCover(g2, bg[i], (int) bgX[i], 0, W, H);
                }
            } else {
                g2.setColor(new Color(15, 18, 30));
                g2.fillRect(0, 0, W, H);
            }
            if (state == GameState.MENU) {
                drawMenu(g2);
                return;
            }
            if (state == GameState.HIGHSCORE) {
                drawHighscoreScreen(g2);
                return;
            }
            if (state == GameState.CHARACTER) {
                drawCharacterScreen(g2);
                return;
            }

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Ground
            g2.setColor(new Color(40, 45, 70));
            g2.fillRect(0, H - groundMargin, W, groundMargin);

            // Fuel
            for (Fuel f : fuelPickups) {
                if (fuelImage != null) {
                    g2.drawImage(fuelImage, (int) f.x, (int) f.y, f.size, f.size, null);
                } else {
                    g2.setColor(new Color(40, 45, 70));
                    g2.drawRect((int) f.x, (int) f.y, f.size, f.size);
                }
            }

            for (ScoreCoin sc : scoreCoins) {
                if (scoreCoinImage != null) {
                    g2.drawImage(scoreCoinImage, (int) sc.x, (int) sc.y, sc.size, sc.size, null);
                } else {
                    g2.setColor(Color.YELLOW);
                    g2.fillOval((int) sc.x, (int) sc.y, sc.size, sc.size);
                }
            }

            // Obstacles
            for (Obstacle ob : obstacles) {
                Image img = obstacleImgs[ob.imgIndex];

                if (img != null) {
                    g2.drawImage(img, (int) ob.x, (int) ob.y, ob.w, ob.h, null);
                } else {
                    g2.setColor(new Color(255, 90, 70));
                    g2.fillRoundRect((int) ob.x, (int) ob.y, ob.w, ob.h, 5, 5);
                }

                g2.setColor(new Color(255, 90, 110));
                g2.fillRoundRect((int) ob.x, (int) ob.y, ob.w, ob.h, 6, 6);
            }

            // Player
            Image img = unsichtbarAktiv ? playerImagePower : playerImageNormal;

            if (img != null) {
                g2.drawImage(img, (int) player.x, (int) player.y, player.w,player.h, null);
            } else {
                g2.setColor(new Color(90, 180, 255));
                g2.fillRoundRect((int) player.x, (int) player.y, player.w, player.h, 10, 10);
            }
           /* // DEBUG HITBOX
            g2.setColor(new Color(0, 255, 0, 120));
            Rectangle hb = player.rect();
            g2.fillRect(hb.x, hb.y, hb.width, hb.height); */


            // Jet flame
            if (jetOn && benzin > 0 && !gameOver) {
                g2.setColor(new Color(255, 140, 60));
                int fx = (int) player.x - 1;
                int fy = (int) (player.y + player.h * 0.8);
                g2.fillOval(fx, fy, 25, 60);
            }

            for (Rakete r : raketen) {
                if (raketenImage != null) {
                    g2.drawImage(raketenImage, (int) r.x, (int) r.y, r.w, r.h, null);
                } else {
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.fillRoundRect((int) r.x, (int) r.y, r.w, r.h, 10, 10);
                    g2.setColor(Color.RED);
                    g2.fillOval((int) (r.x + r.w - 14), (int) (r.y + r.h/2 - 4), 8, 8);
                }
            }

            // HUD
            g2.setColor(Color.RED);
            g2.setFont(new Font("Consolas", Font.BOLD, 16));
            g2.drawString("Score: " + score, 16, 26);
            g2.drawString("Münzen: " + (coinScore / 10), 16, 48);
            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.drawString("SPACE = jetpack | R = restart", 16, 70);
            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.drawString("QUIT | ESC", 16, 90);
            //Powerup HUD
            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            if (storedPowerUp != null) {
                g2.drawString("PowerUp: " + storedPowerUp + " (E)", 16, 110);
            } else {
                g2.drawString("PowerUp: -", 16, 110);
            }

            if (unsichtbarAktiv) {
                long left = Math.max(0, (unsichtbarEndeMs - System.currentTimeMillis()) / 1000);
                g2.drawString("Unsichtbar: " + left + "s", 16, 128);
            }

            if (magnetAktiv) {
                long left = Math.max(0, (magnetEndeMs - System.currentTimeMillis()) / 1000);
                g2.drawString("Magnet: " + left + "s", 16, 146);
            }

            if (doubleCoinsAktiv) {
                long left =  Math.max(0, (doubleCoinsEndeMs - System.currentTimeMillis()) / 1000);
                g2.drawString("x2 Münzen " + left + "s", 16, 168);
            }

            if (infiniteFuelAktiv) {
                long left  = Math.max(0, infiniteFuelEndeMs - System.currentTimeMillis()) / 1000;
                g2.drawString("∞ Benzin: " + left + "s", 16, 180);
            }

            //bar
            int barX = 465;
            int barY = 30;
            int barW = 1000;
            int barH = 20;

            g2.setColor(Color.WHITE);
            g2.drawRect(barX, barY, barW, barH);

            double benzinRatio = (voll <= 0) ? 0 : (benzin / voll);
            int fillW = (int) (barW * benzinRatio);

            g2.setColor(new Color(255, 234, 0));
            g2.fillRect(barX + 1, barY + 1, Math.max(0, fillW - 1), barH - 1);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 20));
            g2.setColor(Color.RED);
            g2.drawString("BENZIN", barX, barY - 4);

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
            if (state == GameState.MENU) {
                drawMenu(g2);
                return;
            }
            if (state == GameState.HIGHSCORE) {
                drawHighscoreScreen(g2);
                return;
            }
            if (state == GameState.CHARACTER) {
                drawCharacterScreen(g2);
                return;
            }
        }

        @Override public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            //==MENÜ Steuerung==
            if (state == GameState.MENU) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    menuIndex = (menuIndex -1 + meniItems.length) % meniItems.length;
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    menuIndex = (menuIndex + 1) % meniItems.length;
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (menuIndex == 0) { //Start
                        state = GameState.RUNNING;
                        resetGame();
                    } else if (menuIndex == 1) { //Highscore
                        state = GameState.HIGHSCORE;
                    } else if (menuIndex == 2) {
                        state = GameState.CHARACTER;
                    } else if (menuIndex == 3) {
                        System.exit(0);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
                return;
            }
            if (state == GameState.HIGHSCORE || state == GameState.CHARACTER) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    state = GameState.MENU;
                }
                return;
            }

            //==(Game Steuerung)====
            if (e.getKeyCode() == KeyEvent.VK_SPACE) jetOn = true;
            if (e.getKeyCode() == KeyEvent.VK_W) jetOn = true;

            if (e.getKeyCode() == KeyEvent.VK_R) resetGame();
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) System.exit(0);
            if (e.getKeyCode() == KeyEvent.VK_E) {
                activeStoredPowerUp();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) jetOn = false;
            if (e.getKeyCode() == KeyEvent.VK_W) jetOn = false;
        }
    }

    static class Player {
        double x, y;
        int w, h;
        double vy = 0;

        int hitboxOffsetX = 25;
        int hitboxOffsetY = 15;
        int hitboxW = 85;
        int hitboxH = 93;

        Player(double x, double y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        Rectangle rect() {
            return new Rectangle((int) x + hitboxOffsetX, (int) y + hitboxOffsetY, hitboxW, hitboxH); //Viereck
        }
    }

    static class Obstacle {
        double x, y;
        int w, h;
        int imgIndex;

        Obstacle(double x, double y, int w, int h,int imgIndex) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.imgIndex = imgIndex;
        }

        Rectangle rect() {
            return new Rectangle((int) x, (int) y, w, h);
        }
    }

    static class Fuel {
        double x, y;
        int size;
        boolean collected = false;

        Fuel(double x, double y, int size) {
            this.x = x; this.y = y; this.size = size;
        }

        Rectangle rect() {
            return new Rectangle((int) x, (int) y, size, size);
        }
    }

    static class ScoreCoin {
        double x, y;
        int size;
        boolean collected = false;

        ScoreCoin(double x, double y, int size) {
            this.x = x; this.y = y; this.size = size; //Konstrktor
        }

        Rectangle rect() {
            return new Rectangle((int) x, (int) y, size, size);
        }
    }

    static class Rakete {
        double x, y;
        int w, h;
        double vx;

        Rakete(double x, double y, int w, int h, double vx) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.vx = vx;
        }

        Rectangle rect() {
            return new Rectangle((int) x, (int) y, w, h);
        }
    }
}
