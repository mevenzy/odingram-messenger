package mevenzy.client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatClientSwing {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8080;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private String nickname;

    public static void main(String[] args) {
        new ChatClientSwing().startApplication();
    }

    private void startApplication() {
        while (true) {
            nickname = JOptionPane.showInputDialog(
                    null,
                    "Введите ваш ник для чата:",
                    "Вход в чат",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (nickname == null) {
                System.exit(0);
            }

            nickname = nickname.trim();

            if (nickname.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Ник не может быть пустым!", "Ошибка", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            String status = tryConnectAndVerifyName();

            if (status.equals("OK")) {
                break;
            } else if (status.startsWith("ERROR")) {
                String errorMsg = status.substring(6);
                JOptionPane.showMessageDialog(null, errorMsg, "Имя занято", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Не удалось подключиться к серверу! Проверьте, что он запущен.", "Ошибка сети", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }

        SwingUtilities.invokeLater(this::createAndShowGUI);
    }

    private String tryConnectAndVerifyName() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(nickname);

            String response = in.readLine();
            if (response != null) {
                return response;
            }
        } catch (IOException e) {
            return "CONNECT_FAILED";
        }
        return "CONNECT_FAILED";
    }

    private void createAndShowGUI() {
        frame = new JFrame("Odingram - " + nickname);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 500);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Отправить");

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        frame.setVisible(true);

        startListening();
    }

    private void startListening() {
        Thread readThread = new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {

                    if (serverMessage.startsWith("RENAME_OK: ")) {
                        String newName = serverMessage.substring(11);
                        nickname = newName;
                        SwingUtilities.invokeLater(() -> frame.setTitle("Мини Чат - " + nickname));
                        continue;
                    }

                    String finalMessage = showTime() + serverMessage;
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(finalMessage + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatArea.append("[Система]: Соединение с сервером разорвано.\n"));
            }
        });
        readThread.start();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            out.println(text);
            inputField.setText("");

            if (text.equalsIgnoreCase("/exit")) {
                System.exit(0);
            }
        }
    }

    private String showTime() {
        return "[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "] ";
    }
}