package mevenzy.client;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatClientFrame extends JFrame {
    private JPanel chatMessagesPanel;
    private JScrollPane chatScrollPane;

    private JTextField messageField;
    private JButton sendButton;

    private JSplitPane splitPane;
    private JList<String> userList;
    private DefaultListModel<String> listModel;
    private JPanel rightPanel;
    private JButton toggleUsersButton;
    private JLabel connectionStatusLabel; // Индикатор сети
    private boolean isUsersVisible = true;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;

    public ChatClientFrame() {
        setTitle("Odingram Messenger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 600);
        setLocationRelativeTo(null);

        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));

        initUI();
        SwingUtilities.invokeLater(this::startConnectionWorkflow);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(new EmptyBorder(0, 5, 5, 5));

        connectionStatusLabel = new JLabel("● Подключение к серверу...");
        connectionStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        connectionStatusLabel.setForeground(new Color(242, 168, 75));

        toggleUsersButton = new JButton("👥 Скрыть пользователей");
        toggleUsersButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        toggleUsersButton.putClientProperty("JButton.buttonType", "roundRect");

        topPanel.add(connectionStatusLabel, BorderLayout.WEST);
        topPanel.add(toggleUsersButton, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBackground(new Color(36, 37, 38));
        chatMessagesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        chatScrollPane = new JScrollPane(chatMessagesPanel);
        chatScrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65), 1, true));
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        rightPanel = new JPanel(new BorderLayout(5, 5));
        JLabel onlineLabel = new JLabel(" В сети:", SwingConstants.LEFT);
        onlineLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        userList.setCellRenderer(new UserListRenderer());
        userList.setFixedCellHeight(44);

        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 63, 65), 1, true));

        rightPanel.add(onlineLabel, BorderLayout.NORTH);
        rightPanel.add(userScrollPane, BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(200, 0));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScrollPane, rightPanel);
        splitPane.setResizeWeight(1.0);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageField.putClientProperty("JTextField.placeholderText", "Введите сообщение...");
        messageField.putClientProperty("JComponent.roundRect", true);

        sendButton = new JButton("Отправить");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.putClientProperty("JButton.buttonType", "roundRect");
        sendButton.setBackground(new Color(30, 144, 255));
        sendButton.setForeground(Color.WHITE);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        add(mainPanel);

        toggleUsersButton.addActionListener(e -> {
            if (isUsersVisible) {
                splitPane.setRightComponent(null);
                toggleUsersButton.setText("👥 Показать пользователей");
            } else {
                splitPane.setRightComponent(rightPanel);
                splitPane.setDividerLocation(splitPane.getWidth() - 210);
                toggleUsersButton.setText("👥 Скрыть пользователей");
            }
            isUsersVisible = !isUsersVisible;
            splitPane.revalidate();
            splitPane.repaint();
        });

        sendButton.addActionListener(e -> sendMessage());
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
    }

    private void setStatusConnected(String serverInfo) {
        SwingUtilities.invokeLater(() -> {
            connectionStatusLabel.setText("● Подключено к " + serverInfo);
            connectionStatusLabel.setForeground(new Color(110, 197, 116));
        });
    }

    private void setStatusDisconnected() {
        SwingUtilities.invokeLater(() -> {
            connectionStatusLabel.setText("● Соединение разорвано");
            connectionStatusLabel.setForeground(new Color(224, 112, 112));
        });
    }

    private void startConnectionWorkflow() {
        String serverAddress = JOptionPane.showInputDialog(this, "Введите адрес сервера (IP/Домен):", "Подключение", JOptionPane.QUESTION_MESSAGE);
        if (serverAddress == null || serverAddress.trim().isEmpty()) System.exit(0);

        String portStr = JOptionPane.showInputDialog(this, "Введите порт:", "Подключение", JOptionPane.QUESTION_MESSAGE);
        if (portStr == null || portStr.trim().isEmpty()) System.exit(0);
        int port = Integer.parseInt(portStr.trim());

        while (true) {
            nickname = JOptionPane.showInputDialog(this, "Введите ник:", "Авторизация", JOptionPane.QUESTION_MESSAGE);
            if (nickname == null || nickname.trim().isEmpty()) System.exit(0);
            nickname = nickname.trim();

            try {
                socket = new Socket(serverAddress, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println(nickname);
                String serverResponse = in.readLine();

                if (serverResponse != null && serverResponse.startsWith("ERROR")) {
                    JOptionPane.showMessageDialog(this, serverResponse.substring(6), "Ошибка", JOptionPane.ERROR_MESSAGE);
                    socket.close();
                } else if (serverResponse != null && serverResponse.equals("OK")) {
                    break;
                } else {
                    JOptionPane.showMessageDialog(this, "Неизвестный ответ сервера.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    socket.close();
                    System.exit(0);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка сети: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }

        setStatusConnected(serverAddress + ":" + port);
        new Thread(this::listenToServer).start();
        new Thread(this::startOnlineUsersUpdater).start();
    }

    private void listenToServer() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                if (serverMessage.startsWith("RENAME_OK: ")) {
                    String newName = serverMessage.substring(11);
                    appendSystemMessage("Вы успешно сменили ник на " + newName);
                    continue;
                }

                if (serverMessage.startsWith("[Система]: Сейчас в сети: ")) {
                    String namesRaw = serverMessage.substring(25);
                    updateOnlineListUI(namesRaw);
                    continue;
                }

                parseAndAppendMessage(serverMessage);
            }
        } catch (IOException e) {
            setStatusDisconnected();
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
        }
    }

    private void startOnlineUsersUpdater() {
        try {
            while (socket != null && !socket.isClosed()) {
                out.println("/online");
                Thread.sleep(4000);
            }
        } catch (InterruptedException ignored) {
        }
    }

    private void updateOnlineListUI(String namesRaw) {
        String[] names = namesRaw.split(", ");
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            for (String name : names) {
                if (!name.trim().isEmpty()) {
                    if (name.equalsIgnoreCase(nickname)) {
                        listModel.addElement(name + " (Вы)");
                    } else {
                        listModel.addElement(name);
                    }
                }
            }
        });
    }

    private void parseAndAppendMessage(String rawMessage) {
        if (rawMessage.startsWith("[Система]:")) {
            appendSystemMessage(rawMessage.replace("[Система]:", "").trim());
            return;
        }

        boolean isMe = rawMessage.startsWith("[Вы]:");
        String senderName = "";
        String textContent = "";

        if (rawMessage.contains("]: ")) {
            int splitIdx = rawMessage.indexOf("]: ");
            senderName = rawMessage.substring(1, splitIdx);
            textContent = rawMessage.substring(splitIdx + 3);
        } else {
            textContent = rawMessage;
        }

        final String finalSender = senderName;
        final String finalBoxText = textContent;

        SwingUtilities.invokeLater(() -> {
            JPanel rowPanel = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 4));
            rowPanel.setOpaque(false);
            rowPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

            JPanel bubble = new RoundedPanel(16);
            bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
            bubble.setBorder(new EmptyBorder(8, 14, 8, 14));

            Color bubbleColor = isMe ? new Color(30, 144, 255) : new Color(58, 59, 60);
            bubble.setBackground(bubbleColor);

            if (!isMe) {
                JLabel nameLabel = new JLabel(finalSender);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                nameLabel.setForeground(new Color(170, 175, 180));
                nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                bubble.add(nameLabel);
                bubble.add(Box.createVerticalStrut(3));
            }

            JTextArea messageText = new JTextArea(finalBoxText);
            messageText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageText.setForeground(Color.WHITE);
            messageText.setEditable(false);
            messageText.setFocusable(false);
            messageText.setLineWrap(true);
            messageText.setWrapStyleWord(true);
            messageText.setOpaque(false);
            messageText.setBackground(new Color(0, 0, 0, 0));
            messageText.setAlignmentX(Component.LEFT_ALIGNMENT);

            int maxBubbleWidth = 400;
            if (messageText.getPreferredSize().width > maxBubbleWidth) {
                messageText.setSize(new Dimension(maxBubbleWidth, Short.MAX_VALUE));
            }
            bubble.add(messageText);

            bubble.add(Box.createVerticalStrut(4));
            String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            JLabel timeLabel = new JLabel(timeStr, SwingConstants.RIGHT);
            timeLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
            timeLabel.setForeground(isMe ? new Color(210, 230, 255) : new Color(160, 160, 160));
            timeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
            bubble.add(timeLabel);

            rowPanel.add(bubble);
            chatMessagesPanel.add(rowPanel);

            chatMessagesPanel.revalidate();
            chatMessagesPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private void appendSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
            rowPanel.setOpaque(false);
            rowPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

            String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            JLabel sysLabel = new JLabel("[" + timeStr + "] " + text);
            sysLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            sysLabel.setForeground(Color.GRAY);

            rowPanel.add(sysLabel);
            chatMessagesPanel.add(rowPanel);

            chatMessagesPanel.revalidate();
            chatMessagesPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            out.println(text);
            if (text.equalsIgnoreCase("/exit")) {
                System.exit(0);
            }
            messageField.setText("");
        }
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new ChatClientFrame().setVisible(true));
    }

    private static class RoundedPanel extends JPanel {
        private final int cornerRadius;

        public RoundedPanel(int radius) {
            this.cornerRadius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius));
            g2.dispose();
        }
    }

    private static class UserListRenderer extends DefaultListCellRenderer {
        private final Color[] avatarColors = {
                new Color(224, 112, 112),
                new Color(242, 168, 75),
                new Color(84, 180, 211),
                new Color(110, 197, 116),
                new Color(153, 133, 218),
                new Color(228, 115, 171)
        };

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            String originalText = (value != null) ? value.toString() : "";
            String cleanName = originalText.replace(" (Вы)", "").trim();
            String firstLetter = cleanName.isEmpty() ? "?" : cleanName.substring(0, 1).toUpperCase();

            int colorIndex = Math.abs(cleanName.hashCode()) % avatarColors.length;
            Color circleColor = avatarColors[colorIndex];

            Icon avatarIcon = new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    g2.setColor(circleColor);
                    g2.fill(new Ellipse2D.Double(x, y, 28, 28));

                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 13));

                    FontMetrics fm = g2.getFontMetrics();
                    int letterWidth = fm.stringWidth(firstLetter);
                    int letterHeight = fm.getAscent();
                    int letterX = x + (28 - letterWidth) / 2;
                    int letterY = y + (28 - letterHeight) / 2 + fm.getAscent();

                    g2.drawString(firstLetter, letterX, letterY);
                    g2.dispose();
                }

                @Override
                public int getIconWidth() { return 28; }
                @Override
                public int getIconHeight() { return 28; }
            };

            label.setIcon(avatarIcon);
            label.setText("  " + originalText);
            label.setBorder(new EmptyBorder(0, 8, 0, 8));

            return label;
        }
    }
}