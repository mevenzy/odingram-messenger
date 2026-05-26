package mevenzy.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;

    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = out;

            String inputName = in.readLine();
            if (inputName == null || inputName.trim().isEmpty()) {
                out.println("ERROR: Ник не может быть пустым!");
                return;
            }

            inputName = inputName.trim();

            if (ChatServer.isNameTaken(inputName)) {
                out.println("ERROR: Ник " + inputName + " уже занят другим пользователем!");
                return;
            }

            this.clientName = inputName;
            out.println("OK");

            ChatServer.sendChatHistory(out);

            ChatServer.addClient(out, this);
            ChatServer.broadcastSystemMessage("[Система]: " + clientName + " вошел в чат!");
            System.out.println(clientName + " подключился (" + socket.getRemoteSocketAddress() + ")");

            String message;
            while ((message = in.readLine()) != null) {
                String lowerMessage = message.trim().toLowerCase();

                if (lowerMessage.equals("/exit")) {
                    break;
                }

                if (lowerMessage.equals("/online")) {
                    String onlineClients = ChatServer.getOnlineClients();
                    out.println("[Система]: Сейчас в сети: " + onlineClients);
                    continue;
                }

                if (lowerMessage.startsWith("/rename ")) {
                    String[] parts = message.split(" ", 2);

                    if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                        String oldName = this.clientName;
                        String newName = parts[1].trim();

                        if (ChatServer.changeName(out, oldName, newName)) {
                            out.println("RENAME_OK: " + newName);
                        } else {
                            out.println("[Система]: Ник " + newName + " уже занят.");
                        }
                    } else {
                        out.println("[Система]: Неверный формат. Используйте: /rename новый_ник");
                    }
                    continue;
                }

                if (lowerMessage.startsWith("/private ")) {
                    String[] parts = message.split(" ", 3);

                    if (parts.length >= 3) {
                        String targetName = parts[1];
                        String privateMsg = parts[2];
                        ChatServer.sendPrivateMessage(out, clientName, targetName, privateMsg);
                    } else {
                        out.println("[Система]: Неверный формат. Используйте: /private ник сообщение");
                    }
                    continue;
                }

                if (lowerMessage.equals("/clear")) {
                    if (!ChatServer.clearChatHistory()) {
                        out.println("[Система]: Произошла ошибка.");
                    }
                    continue;
                }

                if  (lowerMessage.equals("/help")) {
                    ChatServer.sendHelpMessage(out);
                    continue;
                }

                if (message.startsWith("/")) {
                    out.println("[Система]: Неизвестная команда: " + message);
                    ChatServer.sendHelpMessage(out);
                    continue;
                }

                ChatServer.broadcastUserMessage(out, clientName, message);
            }
        } catch (IOException ignored) {

        } finally {
            if (out != null) {
                ChatServer.removeClient(out);
            }
            if (clientName != null) {
                ChatServer.broadcastSystemMessage("[Система]: " + clientName + " покинул чат.");
                System.out.println(clientName + " отключился.");
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}