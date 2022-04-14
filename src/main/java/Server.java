import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {

    private ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService chatRouter = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, String> chats = new HashMap<>();
    private final Map<String, Socket> users = new HashMap<>();
    private final List<String> waiting = new ArrayList<>();
    private final Map<SocketAddress, PrintWriter> sessions = new HashMap<>();

    public void alert() {
        threadPool.submit(() -> {
            System.out.println("[server]: Thread " + Thread.currentThread().getName() + " waiting...");
            Socket client;
            try {
                client = serverSocket.accept();
                SocketAddress IP = client.getRemoteSocketAddress();
                alert();
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                sessions.put(IP, out);
                out.println(new Packet("connected"));
                try {
                    String connectionInfo = in.readLine();
                    System.out.println(Thread.currentThread().getName() + ": moving packet [" + connectionInfo + "]");
                    Packet p = Packet.from(connectionInfo);
                    if (p.topic().equals("connection info")) {
                        users.put(p.getValue("username"), client);
                        chats.put(p.getValue("username"), p.getValue("searchingFor"));
                        out.println(new Packet("connection success"));
                        waiting.add(p.getValue("username"));
                        String chatConfirm = in.readLine();
                        System.out.println(Thread.currentThread().getName() + ": moving packet [" + chatConfirm + "]");
                        if (Packet.from(chatConfirm).topic().equals("connection confirm")) {
                            messageLoop(p.getValue("username"), in, IP);
                        }
                    }
                } catch (IOException e) {
                    sessions.remove(IP);
                    if (users.containsValue(client)) {
                        users.entrySet().stream().filter(x -> x.getValue() == client).findFirst().ifPresent(s -> {
                            String username = s.getKey();
                            users.remove(username);
                            if (chats.containsKey(username)) {
                                String otherUser = getOtherValue(username, chats);
                                sessions.get(users.get(otherUser).getRemoteSocketAddress()).println(new Packet("other disconnect"));
                                chats.remove(username);
                            } else if (chats.containsValue(username)) {
                                String otherUser = getOtherValue(username, chats);
                                sessions.get(users.get(otherUser).getRemoteSocketAddress()).println(new Packet("other disconnect"));
                                chats.remove(getOtherValue(username, chats));
                            }
                        });
                    }
                    System.out.println("User disconnected");
                }
            } catch (IOException e) {
                System.out.println("Socket disconnect");
            }
        });
    }


    public void messageLoop(String username, BufferedReader r, SocketAddress address) {
        try {
            Packet message = Packet.from(r.readLine());
            System.out.println(Thread.currentThread().getName() + ": moving packet [" + message + "]");
            if (message.topic().equals("message")) {
                String otherUser = getOtherValue(username, chats);
                sessions.get(users.get(otherUser).getRemoteSocketAddress()).println(new Packet("message").addValue("user", username).addValue("message", message.getValue("message")).toString());
                messageLoop(username, r, address);
            } else if (message.topic().equals("disconnected")) {
                System.out.println("Disconnect detect");
                String otherUser = getOtherValue(username, chats);
                if (chats.containsKey(username)) {
                    chats.remove(username);
                } else {
                    chats.remove(otherUser);
                }
                Socket sock = users.get(username);
                PrintWriter writer = sessions.get(sock.getRemoteSocketAddress());
                sessions.remove(sock.getRemoteSocketAddress());
                users.remove(username);
                writer.println(new Packet("disconnect safe"));
                writer.close();
                sock.close();
                sessions.get(users.get(otherUser).getRemoteSocketAddress()).println(new Packet("other disconnect"));
            }
        } catch (IOException e) {
            sessions.remove(address);
            if (users.containsKey(username)) {
                users.entrySet().stream().filter(x -> Objects.equals(x.getKey(), username)).findFirst().ifPresent(s -> {
                    if (chats.containsKey(username)) {
                        String otherUser = getOtherValue(username, chats);
                        sessions.get(users.get(otherUser).getRemoteSocketAddress()).println(new Packet("other disconnect"));
                        chats.remove(username);
                    } else if (chats.containsValue(username)) {
                        String otherUser = getOtherValue(username, chats);
                        sessions.get(users.get(otherUser).getRemoteSocketAddress()).println(new Packet("other disconnect"));
                        chats.remove(getOtherValue(username, chats));
                    }
                });
            }
        }
    }
    public void run() throws IOException {
        serverSocket = new ServerSocket(4321);
        alert();
        alert();
        alert();
        chatRouter.scheduleAtFixedRate(() -> waiting.forEach(s -> {
            String opp = chats.get(s);
            if (users.containsKey(opp)) {
                waiting.remove(s);
                notifyConnect(s);
                notifyConnect(opp);
            }
        }), 1, 1, TimeUnit.SECONDS);
    }
    public void notifyConnect(String username) {
        sessions.get(users.get(username).getRemoteSocketAddress()).println(new Packet("chat start"));
    }
    public static String getOtherValue(String value, Map<String, String> map) {
        if (map.containsKey(value)) {
            return map.get(value);
        } else if (map.containsValue(value)) {
            return map.keySet().stream().filter(s -> s.equals(value)).findFirst().get();
        } else return null;
    }


}
