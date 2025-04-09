import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.*;
import org.json.*;

public class QuizGame {
    private static final int SCREEN_WIDTH = 1024;
    private static final int SCREEN_HEIGHT = 768;
    private static final int MAX_PLAYERS = 4;
    private static final int QUESTIONS_PER_PLAYER = 5;
    private static final int MAX_NAME_LENGTH = 20;
    private static final int QUESTION_TIME_LIMIT = 30;
    
    private static final Color BLACK = new Color(0, 0, 0);
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color GREEN = new Color(0, 200, 0);
    private static final Color RED = new Color(200, 0, 0);
    private static final Color BLUE = new Color(0, 0, 200);
    private static final Color DARK_BLUE = new Color(0, 0, 50);
    private static final Color LIGHT_BLUE = new Color(100, 100, 255);
    
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    
    private List<Player> players;
    private List<Question> questions;
    private int currentPlayer;
    private int numPlayers;
    private String gameState;
    private int timeLeft;
    private long timerStart;
    
    public QuizGame() {
        initializeQuestions();
        setupGUI();
    }
    
    private void setupGUI() {
        frame = new JFrame("Quiz Informatique");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        createMenuPanel();
        createHelpPanel();
        createPlayerSelectPanel();
        createNameInputPanel();
        createQuestionPanel();
        createResultsPanel();
        
        frame.add(mainPanel);
        frame.setVisible(true);
        
        gameState = "MENU";
        showScreen("MENU");
    }
    
    private void initializeQuestions() {
        questions = new ArrayList<>();
        questions.add(new Question("Quel langage a inspiré C++?", 
            Arrays.asList("C", "Java", "Python", "Assembly"), 0));
        questions.add(new Question("Commande Linux pour lister les fichiers?", 
            Arrays.asList("dir", "ls", "list", "show"), 1));
        questions.add(new Question("Gestionnaire de paquets Python?", 
            Arrays.asList("pip", "npm", "apt", "yum"), 0));
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader("questions.json"));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();
            
            JSONArray jsonArray = new JSONArray(jsonString.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String question = obj.getString("question");
                JSONArray optionsArray = obj.getJSONArray("options");
                List<String> options = new ArrayList<>();
                for (int j = 0; j < optionsArray.length(); j++) {
                    options.add(optionsArray.getString(j));
                }
                int correctAnswer = obj.getInt("correct_answer");
                String category = obj.optString("category", "Général");
                questions.add(new Question(question, options, correctAnswer, category));
            }
        } catch (Exception e) {
            // File not found or invalid JSON - use default questions
        }
    }
    
    private void assignQuestions() {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            if (!questions.get(i).isUsed()) {
                available.add(i);
            }
        }
        
        Collections.shuffle(available);
        
        for (Player player : players) {
            int questionsToAssign = Math.min(QUESTIONS_PER_PLAYER, available.size());
            player.setQuestionIndices(new ArrayList<>(available.subList(0, questionsToAssign)));
            player.setCurrentQuestionIndex(0);
        }
    }
    
    // Panel creation methods
    private void createMenuPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(DARK_BLUE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                drawCenteredString(g2d, "QUIZ INFORMATIQUE", 
                    new Font("Arial", Font.BOLD, 48), 
                    GOLD, SCREEN_WIDTH/2, 100);
            }
        };
        panel.setLayout(null);
        
        JButton startBtn = createButton("1. Commencer", SCREEN_WIDTH/2 - 150, 200, 300, 50);
        startBtn.addActionListener(e -> {
            gameState = "PLAYER_SELECT";
            showScreen("PLAYER_SELECT");
        });
        
        JButton helpBtn = createButton("2. Aide", SCREEN_WIDTH/2 - 150, 270, 300, 50);
        helpBtn.addActionListener(e -> {
            showScreen("HELP");
        });
        
        JButton quitBtn = createButton("3. Quitter", SCREEN_WIDTH/2 - 150, 340, 300, 50);
        quitBtn.setBackground(RED);
        quitBtn.addActionListener(e -> System.exit(0));
        
        panel.add(startBtn);
        panel.add(helpBtn);
        panel.add(quitBtn);
        
        mainPanel.add(panel, "MENU");
    }
    
    private void createHelpPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(DARK_BLUE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                drawCenteredString(g2d, "AIDE", 
                    new Font("Arial", Font.BOLD, 48), 
                    GOLD, SCREEN_WIDTH/2, 50);
                
                String[] lines = {
                    "Comment jouer:",
                    "- 2-4 joueurs",
                    "- Répondez aux questions",
                    "- Bonne réponse: +10 points",
                    "- Utilisez 1-4 pour répondre",
                    "- Échap: Retour au menu"
                };
                
                g2d.setFont(new Font("Arial", Font.PLAIN, 18));
                g2d.setColor(WHITE);
                for (int i = 0; i < lines.length; i++) {
                    g2d.drawString(lines[i], 100, 120 + i * 30);
                }
            }
        };
        panel.setLayout(null);
        
        JButton backBtn = createButton("Retour", SCREEN_WIDTH/2 - 150, 400, 300, 50);
        backBtn.addActionListener(e -> {
            showScreen(gameState.equals("MENU") ? "MENU" : "PLAYER_SELECT");
        });
        panel.add(backBtn);
        
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showScreen(gameState.equals("MENU") ? "MENU" : "PLAYER_SELECT");
            }
        });
        
        mainPanel.add(panel, "HELP");
    }
    
    private void createPlayerSelectPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(DARK_BLUE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                drawCenteredString(g2d, "Nombre de joueurs", 
                    new Font("Arial", Font.BOLD, 48), 
                    GOLD, SCREEN_WIDTH/2, 50);
            }
        };
        panel.setLayout(null);
        
        for (int i = 2; i <= 4; i++) {
            JButton btn = createButton(i + " Joueurs", SCREEN_WIDTH/2 - 150, 100 + i * 60, 300, 50);
            final int num = i;
            btn.addActionListener(e -> {
                numPlayers = num;
                players = new ArrayList<>();
                gameState = "NAME_INPUT";
                showScreen("NAME_INPUT");
            });
            panel.add(btn);
        }
        
        JButton backBtn = createButton("Retour", SCREEN_WIDTH/2 - 150, 400, 300, 50);
        backBtn.setBackground(RED);
        backBtn.addActionListener(e -> {
            gameState = "MENU";
            showScreen("MENU");
        });
        panel.add(backBtn);
        
        mainPanel.add(panel, "PLAYER_SELECT");
    }
    
    private void createNameInputPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(DARK_BLUE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                String prompt = "Joueur " + (players.size() + 1) + " - Entrez votre nom:";
                drawCenteredString(g2d, prompt, 
                    new Font("Arial", Font.PLAIN, 24), 
                    WHITE, SCREEN_WIDTH/2, 150);
            }
        };
        panel.setLayout(null);
        
        JTextField nameField = new JTextField();
        nameField.setBounds(SCREEN_WIDTH/2 - 150, 200, 300, 40);
        nameField.setFont(new Font("Arial", Font.PLAIN, 20));
        nameField.addActionListener(e -> {
            if (!nameField.getText().trim().isEmpty()) {
                players.add(new Player(nameField.getText().trim()));
                nameField.setText("");
                
                if (players.size() == numPlayers) {
                    assignQuestions();
                    currentPlayer = 0;
                    gameState = "GAME";
                    showScreen("GAME");
                } else {
                    panel.repaint();
                }
            }
        });
        
        panel.add(nameField);
        
        mainPanel.add(panel, "NAME_INPUT");
    }
    
    private void createQuestionPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                
                Player player = players.get(currentPlayer);
                if (player.getCurrentQuestionIndex() >= player.getQuestionIndices().size()) {
                    currentPlayer = (currentPlayer + 1) % numPlayers;
                    if (currentPlayer == 0) {
                        gameState = "RESULTS";
                        showScreen("RESULTS");
                    }
                    return;
                }
                
                int qIdx = player.getQuestionIndices().get(player.getCurrentQuestionIndex());
                Question question = questions.get(qIdx);
                
                g2d.setColor(BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw player info and timer
                g2d.setColor(WHITE);
                g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                g2d.drawString(player.getName() + " - Score: " + player.getScore(), 20, 30);
                
                int timeWidth = (SCREEN_WIDTH - 40) * timeLeft / QUESTION_TIME_LIMIT;
                g2d.setColor(timeLeft > 10 ? GREEN : RED);
                g2d.fillRect(20, 50, timeWidth, 10);
                
                // Draw question
                drawCenteredString(g2d, question.getQuestion(), 
                    new Font("Arial", Font.BOLD, 36), 
                    WHITE, SCREEN_WIDTH/2, 150);
                
                // Draw options
                for (int i = 0; i < question.getOptions().size(); i++) {
                    drawCenteredString(g2d, (i+1) + ". " + question.getOptions().get(i), 
                        new Font("Arial", Font.PLAIN, 24), 
                        WHITE, SCREEN_WIDTH/2, 220 + i * 60);
                }
            }
        };
        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    gameState = "MENU";
                    showScreen("MENU");
                    return;
                }
                
                Player player = players.get(currentPlayer);
                if (player.getCurrentQuestionIndex() >= player.getQuestionIndices().size()) {
                    return;
                }
                
                int qIdx = player.getQuestionIndices().get(player.getCurrentQuestionIndex());
                Question question = questions.get(qIdx);
                
                if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_4) {
                    int selected = e.getKeyCode() - KeyEvent.VK_1;
                    boolean correct = selected == question.getCorrectAnswer();
                    
                    if (correct) {
                        player.setScore(player.getScore() + 10);
                        player.setCorrectAnswers(player.getCorrectAnswers() + 1);
                        showFeedback("Bonne réponse! +10 points", GREEN);
                    } else {
                        showFeedback("Mauvaise réponse! La bonne réponse était: " + 
                            question.getOptions().get(question.getCorrectAnswer()), RED);
                    }
                    
                    player.setCurrentQuestionIndex(player.getCurrentQuestionIndex() + 1);
                    currentPlayer = (currentPlayer + 1) % numPlayers;
                    
                    // Update display
                    panel.repaint();
                }
            }
        });
        
        mainPanel.add(panel, "GAME");
    }
    
    private void createResultsPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(DARK_BLUE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                drawCenteredString(g2d, "Résultats Finaux", 
                    new Font("Arial", Font.BOLD, 48), 
                    GOLD, SCREEN_WIDTH/2, 50);
                
                int maxScore = players.stream().mapToInt(Player::getScore).max().orElse(1);
                List<Player> winners = new ArrayList<>();
                for (Player p : players) {
                    if (p.getScore() == maxScore) {
                        winners.add(p);
                    }
                }
                
                players.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
                
                int yPos = 120;
                for (int i = 0; i < players.size(); i++) {
                    Player player = players.get(i);
                    int scoreWidth = (SCREEN_WIDTH - 200) * player.getScore() / maxScore;
                    
                    g2d.setColor(BLUE);
                    g2d.fillRect(100, yPos + 20, scoreWidth, 30);
                    
                    String playerText = (i+1) + ". " + player.getName() + ": " + player.getScore() + " pts";
                    g2d.setColor(winners.contains(player) ? GOLD : WHITE);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 20));
                    g2d.drawString(playerText, 110, yPos + 40);
                    
                    yPos += 70;
                }
            }
        };
        panel.setLayout(null);
        
        JButton restartBtn = createButton("Nouvelle partie", SCREEN_WIDTH/2 - 150, 500, 300, 50);
        restartBtn.addActionListener(e -> {
            resetGame();
            gameState = "MENU";
            showScreen("MENU");
        });
        panel.add(restartBtn);
        
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                resetGame();
                gameState = "MENU";
                showScreen("MENU");
            }
        });
        
        mainPanel.add(panel, "RESULTS");
    }
    
    private void showFeedback(String message, Color color) {
        JPanel feedbackPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                drawCenteredString(g2d, message, 
                    new Font("Arial", Font.BOLD, 24), 
                    color, SCREEN_WIDTH/2, SCREEN_HEIGHT/2);
            }
        };
        
        mainPanel.add(feedbackPanel, "FEEDBACK");
        cardLayout.show(mainPanel, "FEEDBACK");
        
        Timer timer = new Timer(2000, e -> {
            cardLayout.show(mainPanel, "GAME");
            mainPanel.remove(feedbackPanel);
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    private void drawCenteredString(Graphics2D g2d, String text, Font font, Color color, int x, int y) {
        FontMetrics metrics = g2d.getFontMetrics(font);
        int textX = x - metrics.stringWidth(text) / 2;
        int textY = y - metrics.getHeight() / 2 + metrics.getAscent();
        
        g2d.setFont(font);
        g2d.setColor(color);
        g2d.drawString(text, textX, textY);
    }
    
    private JButton createButton(String text, int x, int y, int width, int height) {
        JButton button = new JButton(text);
        button.setBounds(x, y, width, height);
        button.setBackground(BLUE);
        button.setForeground(WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(WHITE, 2));
        button.setBorderPainted(true);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(LIGHT_BLUE);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BLUE);
            }
        });
        
        return button;
    }
    
    private void showScreen(String screen) {
        cardLayout.show(mainPanel, screen);
        if (screen.equals("GAME")) {
            mainPanel.getComponent(4).requestFocusInWindow();
        }
    }
    
    private void resetGame() {
        players = new ArrayList<>();
        currentPlayer = 0;
        for (Question q : questions) {
            q.setUsed(false);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizGame());
    }
}

class Player {
    private String name;
    private int score;
    private int correctAnswers;
    private int currentQuestionIndex;
    private List<Integer> questionIndices;
    
    public Player(String name) {
        this.name = name;
        this.score = 0;
        this.correctAnswers = 0;
        this.currentQuestionIndex = 0;
        this.questionIndices = new ArrayList<>();
    }
    
    // Getters and setters
    public String getName() { return name; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public void setCurrentQuestionIndex(int currentQuestionIndex) { this.currentQuestionIndex = currentQuestionIndex; }
    public List<Integer> getQuestionIndices() { return questionIndices; }
    public void setQuestionIndices(List<Integer> questionIndices) { this.questionIndices = questionIndices; }
}

class Question {
    private String question;
    private List<String> options;
    private int correctAnswer;
    private String category;
    private boolean used;
    
    public Question(String question, List<String> options, int correctAnswer, String category) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.category = category;
        this.used = false;
    }
    
    public Question(String question, List<String> options, int correctAnswer) {
        this(question, options, correctAnswer, "Général");
    }
    
    // Getters and setters
    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public int getCorrectAnswer() { return correctAnswer; }
    public String getCategory() { return category; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}