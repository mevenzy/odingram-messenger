package mevenzy.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField ipField;
    private JTextField portField;
    private JTextField nicknameField;
    private JButton connectButton;
    private final ConnectionCallback callback;

    public interface ConnectionCallback {
        void onConnect(String ip, int port, String nickname);
    }

    public LoginFrame(ConnectionCallback callback) {
        this.callback = callback;

        setTitle("Odingram — Вход");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(360, 440);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        JLabel titleLabel = new JLabel("Odingram", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(30, 144, 255));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Введите данные для подключения", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(Color.GRAY);
        gbc.gridy = 1;
        mainPanel.add(subtitleLabel, gbc);

        gbc.gridy = 2;
        mainPanel.add(Box.createVerticalStrut(10), gbc);

        gbc.gridy = 3;
        mainPanel.add(new JLabel("Ваш никнейм:"), gbc);
        nicknameField = new JTextField();
        nicknameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nicknameField.putClientProperty("JTextField.placeholderText", "Например, Alex");
        nicknameField.putClientProperty("JComponent.roundRect", true);
        gbc.gridy = 4;
        mainPanel.add(nicknameField, gbc);

        gbc.gridy = 5;
        mainPanel.add(new JLabel("IP-адрес сервера:"), gbc);
        ipField = new JTextField("localhost");
        ipField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ipField.putClientProperty("JTextField.placeholderText", "192.168.1.X или домен");
        ipField.putClientProperty("JComponent.roundRect", true);
        gbc.gridy = 6;
        mainPanel.add(ipField, gbc);

        gbc.gridy = 7;
        mainPanel.add(new JLabel("Порт:"), gbc);
        portField = new JTextField("8080");
        portField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        portField.putClientProperty("JTextField.placeholderText", "Например, 8080");
        portField.putClientProperty("JComponent.roundRect", true);
        gbc.gridy = 8;
        mainPanel.add(portField, gbc);

        gbc.gridy = 9;
        mainPanel.add(Box.createVerticalStrut(15), gbc);

        connectButton = new JButton("Войти в чат");
        connectButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        connectButton.putClientProperty("JButton.buttonType", "roundRect");
        connectButton.setBackground(new Color(30, 144, 255));
        connectButton.setForeground(Color.WHITE);
        gbc.gridy = 10;
        mainPanel.add(connectButton, gbc);

        connectButton.addActionListener(e -> handleLogin());
        nicknameField.addActionListener(e -> handleLogin());
        ipField.addActionListener(e -> handleLogin());
        portField.addActionListener(e -> handleLogin());

        add(mainPanel);
    }

    private void handleLogin() {
        String nickname = nicknameField.getText().trim();
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();

        if (nickname.isEmpty() || ip.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, заполните все поля!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);

            this.setVisible(false);
            this.dispose();

            callback.onConnect(ip, port, nickname);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Порт должен быть числом!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}