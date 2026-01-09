import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class StartPage extends JFrame {

    private static final String BACKGROUND_IMAGE_PATH = "image_028dbf.jpg";
    private Image backgroundImage;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new StartPage());
    }

    public StartPage() {
        setTitle("Welcome to Calendar App");

        // FIXED: Set size to 1150x780 to match GUI.java
        // This prevents the window from "jumping" in size when switching.
        setSize(1150, 780);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load Background
        try {
            backgroundImage = ImageIO.read(new File(BACKGROUND_IMAGE_PATH));
        } catch (IOException e) {
            System.err.println("Error: Could not find " + BACKGROUND_IMAGE_PATH);
        }

        // Custom Background Panel
        JPanel backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };

        backgroundPanel.setLayout(new GridBagLayout());
        setContentPane(backgroundPanel);

        // Content Box
        JPanel contentBox = new JPanel();
        contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));
        contentBox.setBackground(new Color(255, 255, 255, 220));
        contentBox.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // ASCII ART
        JTextArea asciiArt = new JTextArea(
                "   ____      _                _              \n" +
                        "  / ___|__ _| | ___ _ __   __| | __ _ _ __   \n" +
                        " | |   / _` | |/ _ \\ '_ \\ / _` |/ _` | '__|  \n" +
                        " | |__| (_| | |  __/ | | | (_| | (_| | |     \n" +
                        "  \\____\\__,_|_|\\___|_| |_|\\__,_|\\__,_|_|     \n" +
                        "                                             \n" +
                        "           MANAGEMENT SYSTEM v1.0            ");

        asciiArt.setFont(new Font("Monospaced", Font.BOLD, 14));
        asciiArt.setEditable(false);
        asciiArt.setOpaque(false);
        asciiArt.setForeground(Color.BLACK);
        asciiArt.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Start Button
        JButton startButton = new JButton("ENTER APPLICATION");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        startButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startButton.setBackground(new Color(40, 40, 40));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setBorderPainted(false);
        startButton.setOpaque(true);

        startButton.addActionListener(e -> {
            // Pass the current window to GUI to avoid close/reopen flicker
            new GUI().start(this);
        });

        contentBox.add(asciiArt);
        contentBox.add(Box.createVerticalStrut(30));
        contentBox.add(startButton);

        backgroundPanel.add(contentBox);
        setVisible(true);
    }
}