package client;

import common.ChatMessage;
import common.FileOffer;
import common.Message;
import common.Protocol;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class ConsoleClient {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java client.ConsoleClient <host> <port> <user> <pass>");
            System.exit(1);
        }
        String host = args[0];
        int    port = Integer.parseInt(args[1]);
        String user = args[2];
        String pass = args[3];

        BlockingQueue<Message> inbound = new LinkedBlockingQueue<>();

        ChatClient client = new ChatClient(host, port, m -> {
            try { inbound.put(m); } catch (InterruptedException ignored) {}
        }, ev -> {
            switch (ev.type) {
                case PROGRESS:   System.out.println("[File] progress " + ev.info);    break;
                case COMPLETED:  System.out.println("[File] DONE -> " + ev.path);     break;
                case REJECTED:   System.out.println("[File] REJECTED: " + ev.info);   break;
                case FAILED:     System.out.println("[File] FAILED: " + ev.info);     break;
            }
        });

        client.connect();
        Message loginResp = client.login(user, pass);
        if (loginResp.getType() != Protocol.OK) {
            System.err.println("Login failed: " + loginResp.getPayload());
            System.exit(1);
        }
        client.setUsername(user);
        System.out.println("Logged in as " + user + ". Commands: /users /to /file /quit");

        Thread reader = new Thread(() -> {
            try {
                while (true) {
                    Message m = inbound.poll(1, TimeUnit.SECONDS);
                    if (m == null) continue;
                    byte t = m.getType();
                    switch (t) {
                        case Protocol.CHAT_RECV: {
                            ChatMessage cm = (ChatMessage) m.getPayload();
                            System.out.println("[" + m.getFrom() + "]: " + cm.getText());
                            break;
                        }
                        case Protocol.SYSTEM_NOTIFY: {
                            System.out.println("[System] " + m.getPayload());
                            break;
                        }
                        case Protocol.FILE_OFFER_INCOMING: {
                            FileOffer offer = (FileOffer) m.getPayload();
                            System.out.println("[File] " + m.getFrom()
                                    + " sends: " + offer.fileName
                                    + " (" + offer.fileSize + " bytes)");
                            File saveTo = new File("recv_" + offer.fileName);
                            client.markIncomingAccepted(offer.transferId, saveTo);
                            client.acceptFile(user, m.getFrom(), offer.transferId);
                            System.out.println("[File] Auto-accept -> " + saveTo.getAbsolutePath());
                            break;
                        }
                        default:
                            System.out.println("<< " + m);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }, "inbound-reader");
        reader.setDaemon(true);
        reader.start();

        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.equalsIgnoreCase("/quit") || line.equalsIgnoreCase("/exit")) break;

            if (line.equalsIgnoreCase("/users")) {
                Message r = client.requestUserList();
                if (r.getType() == Protocol.USER_LIST) {
                    String[] users = (String[]) r.getPayload();
                    System.out.println("Online: " + String.join(", ", users));
                } else {
                    System.out.println("Error: " + r.getPayload());
                }
            } else if (line.startsWith("/to ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length < 3) { System.out.println("Usage: /to <user> <text>"); continue; }
                client.sendChat(user, parts[1], parts[2]);
                System.out.println("[Me -> " + parts[1] + "]: " + parts[2]);
            } else if (line.startsWith("/file ")) {
                String[] parts = line.split(" ", 3);
                if (parts.length < 3) { System.out.println("Usage: /file <user> <path>"); continue; }
                File f = new File(parts[2]);
                if (!f.exists()) { System.out.println("File not found"); continue; }
                String tid = UUID.randomUUID().toString();
                client.sendFile(user, parts[1], f, tid);
                System.out.println("[Me -> " + parts[1] + "] sending " + f.getName());
            } else {
                System.out.println("Commands: /users /to <user> <text> /file <user> <path> /quit");
            }
        }

        client.logout();
        System.exit(0);
    }
}
