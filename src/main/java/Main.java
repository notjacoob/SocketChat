import java.io.IOException;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length > 0) {
            if (Objects.equals(args[0], "client")) {
                Client client = new Client();
                client.start("127.0.0.1", 4321);
            } else if (Objects.equals(args[0], "server")) {
                Server serv = new Server();
                serv.run();
            }
        }
    }

}
