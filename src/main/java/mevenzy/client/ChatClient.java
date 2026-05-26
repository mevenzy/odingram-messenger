package mevenzy.client;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Socket socket = null;
        BufferedReader in = null;
        PrintWriter out = null;
        String nickname = "";

        System.out.println("\033[1m===== Добро пожаловать в Odingram! =====\033[0m\n");

        while (true) {
            System.out.print("Введите ваш ник для чата: ");
            nickname = scanner.nextLine().trim();

            if (nickname.isEmpty()) {
                System.out.println("[Ошибка]: Ник не может быть пустым. Попробуйте еще раз!\n");
                continue;
            }

            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println(nickname);

                String serverResponse = in.readLine();

                if (serverResponse != null && serverResponse.startsWith("ERROR")) {
                    System.out.println("[Ошибка сервера]: " + serverResponse.substring(6));
                    System.out.println("Пожалуйста, выберите другое имя.\n");

                    socket.close();
                } else if (serverResponse != null && serverResponse.equals("OK")) {
                    break;
                } else {
                    System.out.println("[Ошибка]: Неизвестный ответ от сервера. Попробуйте снова.");
                    if (socket != null) socket.close();
                }
            } catch (IOException e) {
                System.out.println("[Ошибка]: Не удалось подключиться к серверу.");
                return;
            }
        }

        try {
            System.out.println("\nВы успешно подключились к чату под ником: " + nickname);
            System.out.println("Для отображения списка пользователей в сети: /online");
            System.out.println("Для смены ника: /rename новый_ник");
            System.out.println("Для приватного сообщения: /private ник сообщение");
            System.out.println("Для очистки истории чата : /clear");
            System.out.println("Для отображения списка доступных команд: /help");
            System.out.println("Для выхода из чата введите команду: /exit\n");

            BufferedReader finalIn = in;
            Thread readThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = finalIn.readLine()) != null) {

                        if (serverMessage.startsWith("RENAME_OK: ")) {
                            System.out.print("\r\u001B[K");
                            continue;
                        }

                        System.out.print("\r\u001B[K");

                        String finalMessage = showTime() + serverMessage;
                        System.out.println(finalMessage);
                        System.out.print("Введите сообщение: ");
                    }
                } catch (IOException e) {
                    System.err.println("\nСоединение с сервером разорвано.");
                }
            });
            readThread.start();

            System.out.print("Введите сообщение: ");

            while (scanner.hasNextLine()) {
                String clientMessage = scanner.nextLine();

                if (clientMessage.equalsIgnoreCase("/exit")) {
                    out.println("/exit");
                    break;
                }

                out.println(clientMessage);
                System.out.print("Введите сообщение: ");
            }

            System.out.println("Выходим из чата...");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String showTime() {
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return "[" + now.format(formatter) + "] ";
    }
}