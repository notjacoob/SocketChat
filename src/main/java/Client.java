import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {

    private Socket client;
    private PrintWriter out;
    Scanner scanner = new Scanner(System.in);
    private BufferedReader in;
    String username = "null";
    String otherUser = "null";
    private boolean continueReadLoop = true;
    private List<String> consoleLines = new ArrayList<>();
    private volatile boolean canChat = false;
    private volatile boolean canDisconnect = false;
    private final Thread readThread = new Thread(() -> {
        try {
            readLoop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    });
    private final Thread writeThread = new Thread(this::writeLoop);

    public void start(String ip, int port) throws IOException, InterruptedException {
        try {
            client=new Socket(ip, port);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                out.println(new Packet("disconnected").addValue("user", username));
                while (!canDisconnect) {
                    Thread.onSpinWait();
                }
            }));
        } catch (IOException e) {
            System.out.println("Could not connect to server...");
            continueReadLoop = false;
        }
        while (!in.ready()) {}
        readThread.start();
        writeThread.start();
        Thread.currentThread().join();
    }

    public void readLoop() throws IOException {
        Packet p = Packet.from(in.readLine());
        switch (p.topic()) {
            case "connected" -> {
                System.out.print("Connected! Enter your username followed by the username you want to connect to: ");
                String[] usernames = scanner.nextLine().split(" ");
                if (usernames.length > 1) {
                    consoleLines.add("Connected! Enter your username followed by the username you want to connect to: " + usernames[0] + " " + usernames[1]);
                    username = usernames[0];
                    otherUser = usernames[1];
                    out.println(new Packet("connection info")
                            .addValue("username", username)
                            .addValue("searchingFor", otherUser)
                            .toString());
                }
            }
            case "connection success" -> {
                System.out.println("Waiting for other user to connect");
                consoleLines.add("Waiting for other user to connect");
            }
            case "chat start" -> {
                out.println(new Packet("connection confirm").toString());
                consoleLines.add("Other user has connected!");
                printWrite();
                canChat = true;
            }
            case "message" -> {
                consoleLines.add(otherUser + ": " + p.getValue("message"));
                printWrite();
            }
            case "disconnect safe" -> {
                canDisconnect = true;
                continueReadLoop = false;
                System.out.println("Disconnected!");
            }
            case "other disconnect" -> {
                System.out.println("\n" + otherUser + " disconnected...");
                out.println(new Packet("disconnected"));
            }
        }
        if (continueReadLoop) readLoop();
    }
    public void writeLoop() {
        while (!canChat) {
            Thread.onSpinWait();
        }
        String input = scanner.nextLine();
        consoleLines.add(username + ": " + input);
        printWrite();
        out.println(new Packet("message").addValue("message", input));
        if (continueReadLoop) {
            writeLoop();
        }
    }
    public void printWrite() {
        clearConsole();
        consoleLines.forEach(System.out::println);
        System.out.print(username + ": ");
    }
    public void clearConsole() {
        System.out.print("\033[H\033[2J");
        try {
           new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.flush();
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client();
        client.start("127.0.0.1", 4321);
    }

}
