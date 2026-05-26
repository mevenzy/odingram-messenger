package mevenzy.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatClient extends Application {
    private VBox chatMessagesPanel;
    private ScrollPane chatScrollPane;
    private TextField messageField;
    private Button sendButton;
    private SplitPane splitPane;
    private ListView<String> userListView;
    private ObservableList<String> listModel;
    private VBox rightPanel;
    private Button toggleUsersButton;
    private Label connectionStatusLabel;
    private boolean isUsersVisible = true;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname;
    private String serverAddress;
    private int serverPort;

    private Stage mainStage;

    private final Color[] avatarColors = {
            Color.web("#E07070"), // Красный
            Color.web("#F2A84B"), // Оранжевый
            Color.web("#54B4D3"), // Голубой
            Color.web("#6EC574"), // Зеленый
            Color.web("#9985DA"), // Фиолетовый
            Color.web("#E473AB")  // Розовый
    };

    @Override
    public void start(Stage primaryStage) {
        this.mainStage = primaryStage;
        showLoginWindow();
    }

    private void showLoginWindow() {
        mainStage.setTitle("Odingram Login");

        VBox loginRoot = new VBox(20);
        loginRoot.setAlignment(Pos.CENTER);
        loginRoot.setPadding(new Insets(30, 40, 30, 40));
        loginRoot.setStyle("-fx-background-color: #18191A;");

        Label titleLabel = new Label("Odingram Messenger");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        titleLabel.setTextFill(Color.WHITE);
        VBox.setMargin(titleLabel, new Insets(0, 0, 10, 0));

        String fieldStyle = "-fx-background-color: #242526; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: #3C3F41; " +
                "-fx-border-radius: 12; " +
                "-fx-text-fill: white; " +
                "-fx-prompt-text-fill: #72767D; " +
                "-fx-pref-height: 42; " +
                "-fx-font-size: 15;";

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("IP сервера");
        ipField.setStyle(fieldStyle);
        ipField.setMaxWidth(280);

        TextField portField = new TextField("8080");
        portField.setPromptText("Порт");
        portField.setStyle(fieldStyle);
        portField.setMaxWidth(280);

        TextField nickField = new TextField();
        nickField.setPromptText("Ваш ник");
        nickField.setStyle(fieldStyle);
        nickField.setMaxWidth(280);

        Button loginButton = new Button("Подключиться");
        loginButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        loginButton.setTextFill(Color.WHITE);
        loginButton.setStyle("-fx-background-color: linear-gradient(to right, #7543F4, #1E90FF); -fx-background-radius: 15; -fx-pref-height: 40; -fx-pref-width: 160;");
        VBox.setMargin(loginButton, new Insets(10, 0, 0, 0));

        loginRoot.getChildren().addAll(titleLabel, ipField, portField, nickField, loginButton);

        Scene loginScene = new Scene(loginRoot, 380, 420);
        mainStage.setScene(loginScene);
        mainStage.setResizable(false);
        mainStage.show();

        loginButton.setOnAction(e -> handleLogin(ipField.getText(), portField.getText(), nickField.getText()));

        nickField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleLogin(ipField.getText(), portField.getText(), nickField.getText());
            }
        });
    }

    private void handleLogin(String ip, String portStr, String nick) {
        String ipAddress = ip.trim();
        String pStr = portStr.trim();
        String nicknameInput = nick.trim();

        if (ipAddress.isEmpty() || pStr.isEmpty() || nicknameInput.isEmpty()) {
            showErrorAlert("Все поля должны быть заполнены!");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(pStr);
        } catch (NumberFormatException e) {
            showErrorAlert("Порт должен быть числом!");
            return;
        }

        try {
            socket = new Socket(ipAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(nicknameInput);
            String serverResponse = in.readLine();

            if (serverResponse != null && serverResponse.startsWith("ERROR")) {
                showErrorAlert(serverResponse.substring(6));
                socket.close();
            } else if (serverResponse != null && serverResponse.equals("OK")) {
                this.nickname = nicknameInput;
                this.serverAddress = ipAddress;
                this.serverPort = port;

                initMainChatUI();

                new Thread(this::listenToServer).start();
                new Thread(this::startOnlineUsersUpdater).start();
            } else {
                showErrorAlert("Неизвестный ответ сервера.");
                socket.close();
            }
        } catch (IOException e) {
            showErrorAlert("Не удалось подключиться: " + e.getMessage());
        }
    }

    private void initMainChatUI() {
        mainStage.setTitle("Odingram Messenger");
        mainStage.setResizable(true);

        BorderPane mainPanel = new BorderPane();
        mainPanel.setPadding(new Insets(10));
        mainPanel.setStyle("-fx-background-color: #18191A;");

        BorderPane topPanel = new BorderPane();
        topPanel.setPadding(new Insets(0, 5, 10, 5));

        connectionStatusLabel = new Label("● Подключено");
        connectionStatusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        connectionStatusLabel.setTextFill(Color.web("#6EC574"));

        toggleUsersButton = new Button("👥 Скрыть пользователей");
        toggleUsersButton.setFont(Font.font("Segoe UI", 12));
        toggleUsersButton.setStyle("-fx-background-color: linear-gradient(to right, #7543F4, #1E90FF); -fx-background-radius: 15; -fx-padding: 8");
        toggleUsersButton.setTextFill(Color.WHITE);

        topPanel.setLeft(connectionStatusLabel);
        BorderPane.setAlignment(connectionStatusLabel, Pos.CENTER_LEFT);
        topPanel.setRight(toggleUsersButton);
        mainPanel.setTop(topPanel);

        chatMessagesPanel = new VBox(8);
        chatMessagesPanel.setPadding(new Insets(10));
        chatMessagesPanel.setStyle("-fx-background-color: #242526;");
        chatMessagesPanel.prefWidthProperty().bind(chatMessagesPanel.widthProperty());

        chatScrollPane = new ScrollPane(chatMessagesPanel);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setStyle("-fx-background-color: #242526; -fx-background: #242526; -fx-border-color: #3C3F41; -fx-border-radius: 5; -fx-background-radius: 5;");

        rightPanel = new VBox(5);
        rightPanel.setPadding(new Insets(0, 0, 0, 5));
        rightPanel.setMinWidth(200);
        rightPanel.setPrefWidth(200);

        Label onlineLabel = new Label(" В сети:");
        onlineLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        onlineLabel.setTextFill(Color.WHITE);

        listModel = FXCollections.observableArrayList();
        userListView = new ListView<>(listModel);
        userListView.setStyle("-fx-background-color: #18191A; -fx-border-color: #3C3F41; -fx-border-radius: 5; -fx-background-radius: 5;");
        VBox.setVgrow(userListView, Priority.ALWAYS);

        userListView.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setStyle("-fx-background-color: transparent; -fx-text-fill: white;");

                    String cleanName = item.replace(" (Вы)", "").trim();
                    String firstLetter = cleanName.isEmpty() ? "?" : cleanName.substring(0, 1).toUpperCase();

                    int colorIndex = Math.abs(cleanName.hashCode()) % avatarColors.length;
                    Color circleColor = avatarColors[colorIndex];

                    Circle avatarCircle = new Circle(14, circleColor);
                    Text letterText = new Text(firstLetter);
                    letterText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                    letterText.setFill(Color.WHITE);

                    StackPane avatarStack = new StackPane(avatarCircle, letterText);

                    Label nameLabel = new Label("  " + item);
                    nameLabel.setFont(Font.font("Segoe UI", 13));
                    nameLabel.setTextFill(Color.WHITE);

                    HBox cellLayout = new HBox(avatarStack, nameLabel);
                    cellLayout.setAlignment(Pos.CENTER_LEFT);
                    cellLayout.setPadding(new Insets(4, 0, 4, 0));

                    setGraphic(cellLayout);
                }
            }
        });

        rightPanel.getChildren().addAll(onlineLabel, userListView);

        splitPane = new SplitPane(chatScrollPane, rightPanel);
        splitPane.setDividerPositions(0.73);
        splitPane.setStyle("-fx-background-color: transparent; -fx-box-border: transparent;");
        mainPanel.setCenter(splitPane);

        HBox inputPanel = new HBox(8);
        inputPanel.setPadding(new Insets(10, 0, 0, 0));

        messageField = new TextField();
        messageField.setFont(Font.font("Segoe UI", 14));
        messageField.setPromptText("Введите сообщение...");
        messageField.setStyle("-fx-background-radius: 15; -fx-background-color: #242526; -fx-text-fill: white; -fx-prompt-text-fill: gray; -fx-padding: 12; -fx-border-color: #3C3F41; -fx-border-radius: 15;");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        sendButton = new Button("Отправить");
        sendButton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        sendButton.setTextFill(Color.WHITE);
        sendButton.setStyle("-fx-background-color: linear-gradient(to right, #7543F4, #1E90FF); -fx-background-radius: 15; -fx-padding: 12;");
        sendButton.setPrefHeight(30);

        inputPanel.getChildren().addAll(messageField, sendButton);
        mainPanel.setBottom(inputPanel);

        toggleUsersButton.setOnAction(e -> {
            if (isUsersVisible) {
                splitPane.getItems().remove(rightPanel);
                toggleUsersButton.setText("👥 Показать пользователей");
            } else {
                splitPane.getItems().add(rightPanel);
                splitPane.setDividerPositions(0.73);
                toggleUsersButton.setText("👥 Скрыть пользователей");
            }
            isUsersVisible = !isUsersVisible;
        });

        sendButton.setOnAction(e -> sendMessage());
        messageField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        Scene scene = new Scene(mainPanel, 750, 600);
        mainStage.setScene(scene);

        chatMessagesPanel.heightProperty().addListener((observable, oldValue, newValue) ->
                chatScrollPane.setVvalue(1.0)
        );
    }

    private void setStatusDisconnected() {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("● Соединение разорвано");
            connectionStatusLabel.setTextFill(Color.web("#E07070")); // Красный
        });
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
            messageField.setDisable(true);
            sendButton.setDisable(true);
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
        Platform.runLater(() -> {
            listModel.clear();
            for (String name : names) {
                if (!name.trim().isEmpty()) {
                    if (name.equalsIgnoreCase(nickname)) {
                        listModel.add(name + " (Вы)");
                    } else {
                        listModel.add(name);
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

        Platform.runLater(() -> {
            HBox rowPanel = new HBox();
            rowPanel.setAlignment(isMe ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            rowPanel.setPadding(new Insets(2, 0, 2, 0));

            VBox bubble = new VBox(3);
            bubble.setPadding(new Insets(8, 14, 6, 14));
            bubble.setMaxWidth(400);

            String style = isMe
                    ? "-fx-background-color: linear-gradient(to right, #7543F4, #1E90FF); -fx-background-radius: 16 16 0 16;"
                    : "-fx-background-color: #3A3B3C; -fx-background-radius: 16 16 16 0;";
            bubble.setStyle(style);

            if (!isMe) {
                Label nameLabel = new Label(finalSender);
                nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                nameLabel.setTextFill(Color.web("#AAAFB4"));
                bubble.getChildren().add(nameLabel);
            }

            Label messageLabel = new Label(finalBoxText);
            messageLabel.setFont(Font.font("Segoe UI", 14));
            messageLabel.setTextFill(Color.WHITE);
            messageLabel.setWrapText(true);
            bubble.getChildren().add(messageLabel);

            String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Label timeLabel = new Label(timeStr);
            timeLabel.setFont(Font.font("Segoe UI", javafx.scene.text.FontPosture.ITALIC, 10));
            timeLabel.setTextFill(isMe ? Color.web("#D2E6FF") : Color.web("#A0A0A0"));

            HBox timeContainer = new HBox(timeLabel);
            timeContainer.setAlignment(Pos.CENTER_RIGHT);
            bubble.getChildren().add(timeContainer);

            rowPanel.getChildren().add(bubble);
            chatMessagesPanel.getChildren().add(rowPanel);
        });
    }

    private void appendSystemMessage(String text) {
        Platform.runLater(() -> {
            HBox rowPanel = new HBox();
            rowPanel.setAlignment(Pos.CENTER);
            rowPanel.setPadding(new Insets(4, 0, 4, 0));

            String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Label sysLabel = new Label("[" + timeStr + "] " + text);
            sysLabel.setFont(Font.font("Segoe UI", 11));
            sysLabel.setTextFill(Color.GRAY);

            rowPanel.getChildren().add(sysLabel);
            chatMessagesPanel.getChildren().add(rowPanel);
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

    private void showErrorAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}