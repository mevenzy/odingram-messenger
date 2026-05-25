package mevenzy.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int PORT = 8080;
    private static final Map<PrintWriter, ClientHandler> clientMap = new ConcurrentHashMap<>();
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    private static final String HISTORY_FILE = "chat_history.txt";

    public static void main(String[] args) {
        System.out.println("Сервер запущен и ждет подключений...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isNameTaken(String clientName) {
        for (ClientHandler client : clientMap.values()) {
            if (client.getClientName().equalsIgnoreCase(clientName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean changeName(PrintWriter out, String oldName, String newName) {
        if (isNameTaken(newName)) {
            return false;
        }

        ClientHandler handler = clientMap.get(out);
        if (handler != null) {
            handler.setClientName(newName);
            broadcastSystemMessage("[Система]: Пользователь " + oldName + " поменял ник на " + newName);
            return true;
        }
        return false;
    }

    public static void addClient(PrintWriter out, ClientHandler handler) {
        clientMap.put(out, handler);
    }

    public static void removeClient(PrintWriter out) {
        clientMap.remove(out);
    }

    public static String getOnlineClients() {
        if (clientMap.isEmpty()) {
            System.out.println("Список пуст.");
        }

        List<String> names = new ArrayList<>();
        for (ClientHandler handler : clientMap.values()) {
            names.add(handler.getClientName());
        }

        return String.join(", ", names);
    }

    public static void sendPrivateMessage(PrintWriter senderOut, String senderName, String targetName, String message) {
        PrintWriter targetOut = null;

        for (Map.Entry<PrintWriter, ClientHandler> entry : clientMap.entrySet()) {
            if (entry.getValue().getClientName().equalsIgnoreCase(targetName)) {
                targetOut = entry.getKey();
                targetName = entry.getValue().getClientName();
                break;
            }
        }

        if (targetOut != null) {
            targetOut.println("[Лично от " + senderName + "]: " + message);

            if (targetOut != senderOut) {
                senderOut.println("[Лично для " + targetName + "]: " + message);
            }
        } else {
            senderOut.println("[Система]: Пользователь '" + targetName + "' не найден.");
        }
    }

    public static boolean clearChatHistory() {
        try (PrintWriter writer = new PrintWriter(HISTORY_FILE)) {
            writer.print("");
            broadcastSystemMessage("[Система]: История чата была очищена.");
            return true;
        } catch (FileNotFoundException e) {
            System.err.println("Ошибка при очистке истории чата: " + e.getMessage());
            return false;
        }
    }

    public static void sendHelpMessage(PrintWriter out) {
        out.println("[Система]: Список доступных команд:");
        out.println("  /online                   - Показать пользователей в сети");
        out.println("  /rename новый_ник         - Изменить свой никнейм");
        out.println("  /private ник сообщение    - Отправить приватное сообщение");
        out.println("  /clear                    - Очистить историю чата");
        out.println("  /help                     - Показать это меню");
        out.println("  /exit                     - Выйти из чата");
    }

    public static void broadcastUserMessage(PrintWriter senderOut, String senderName, String message) {
        if (message.toLowerCase().startsWith("/private") || message.toLowerCase().startsWith("/online") || message.toLowerCase().startsWith("/rename")) {
            return;
        }

        String formattedMessage;
        for (PrintWriter writer : clientMap.keySet()) {
            if (writer == senderOut) {
                writer.println("[Вы]: " + message);
            } else {
                formattedMessage = "[" + senderName + "]: " + message;
                writer.println(formattedMessage);
            }
        }

        saveToHistory("[" + senderName + "]: " + message);
    }

    public static void broadcastSystemMessage(String message) {
        for (PrintWriter writer : clientMap.keySet()) {
            writer.println(message);
        }

        saveToHistory(message);
    }

    public static void sendChatHistory(PrintWriter out) {
        File file = new File(HISTORY_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении истории чата: " + e.getMessage());
        }
    }

    private static synchronized void saveToHistory(String message) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) {
            writer.println(message);
        } catch (IOException e) {
            System.err.println("Ошибка при записи истории чата: " + e.getMessage());
        }
    }
}