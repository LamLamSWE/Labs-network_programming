package server;

import common.Message;
import common.Protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {

    private static final Logger LOG = Logger.getLogger(ChatServer.class.getName());

    private final int port;
    private final UserDatabase db;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private final ExecutorService clientExecutor = Executors.newCachedThreadPool(r ->
            new Thread(r, "client-handler"));

    public ChatServer(int port) {
        this.port = port;
        this.db   = new UserDatabase();
        this.db.seedDemoAccounts();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
        running = true;
        LOG.info("================================================");
        LOG.info("   CHAT SERVER started on 0.0.0.0:" + port);
        LOG.info("   Demo accounts: anh/123456, lam/123456");
        LOG.info("================================================");

        while (running) {
            try {
                java.net.Socket sock = serverSocket.accept();
                ClientHandler h = new ClientHandler(sock, db, this);
                clientExecutor.execute(h);
            } catch (IOException e) {
                if (running) LOG.log(Level.WARNING, "Accept failed", e);
            }
        }
    }

    public void sendUserListTo(ClientHandler h) {
        try {
            String[] users = db.onlineUsers().toArray(new String[0]);
            java.util.Arrays.sort(users);
            h.send(new Message(Protocol.USER_LIST, "", h.getUsername(), users));
        } catch (IOException ignored) {}
    }

    public void broadcastUserList() {
        Collection<ClientHandler> all = db.onlineUsers().stream()
                .map(db::getHandler)
                .filter(java.util.Objects::nonNull)
                .toList();
        for (ClientHandler h : all) sendUserListTo(h);
    }

    public void broadcastSystemNotify(String text) {
        Collection<ClientHandler> all = db.onlineUsers().stream()
                .map(db::getHandler)
                .filter(java.util.Objects::nonNull)
                .toList();
        for (ClientHandler h : all) {
            try {
                h.send(new Message(Protocol.SYSTEM_NOTIFY, "", h.getUsername(), text));
            } catch (IOException ignored) {}
        }
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null) serverSocket.close();
        clientExecutor.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        int port = 9000;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        ChatServer s = new ChatServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { s.stop(); } catch (Exception ignored) {}
        }));
        s.start();
    }
}
