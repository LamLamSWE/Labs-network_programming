package server;

import common.Message;
import common.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final UserDatabase db;
    private final ChatServer server;

    private ObjectOutputStream oos;
    private ObjectInputStream  ois;

    private volatile String username;
    private volatile boolean running = true;

    public ClientHandler(Socket socket, UserDatabase db, ChatServer server) {
        this.socket = socket;
        this.db     = db;
        this.server = server;
    }

    public String  getUsername()     { return username; }
    public Socket  getSocket()       { return socket; }
    public boolean isAuthenticated() { return username != null; }

    @Override
    public void run() {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            LOG.info("Client connected: " + socket.getRemoteSocketAddress());

            while (running) {
                Message msg;
                try {
                    msg = (Message) ois.readObject();
                } catch (java.io.EOFException | java.net.SocketException e) {
                    LOG.info("Client disconnected: " + username);
                    break;
                }
                handle(msg);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Handler error for " + username, ex);
        } finally {
            cleanup();
        }
    }

    public synchronized void send(Message m) throws IOException {
        if (!running) throw new IOException("Handler closed");
        oos.writeObject(m);
        oos.flush();
    }

    private void handle(Message msg) {
        byte type = msg.getType();
        try {
            switch (type) {
                case Protocol.REGISTER:    handleRegister(msg);   break;
                case Protocol.LOGIN:       handleLogin(msg);      break;
                case Protocol.LOGOUT:      handleLogout(msg);     break;
                case Protocol.LIST_USERS:   handleListUsers(msg);  break;
                case Protocol.CHAT_SEND:   handleChatSend(msg);   break;
                case Protocol.FILE_OFFER:  handleFileOffer(msg);  break;
                case Protocol.FILE_ACCEPT: handleFileAccept(msg); break;
                case Protocol.FILE_REJECT: handleFileReject(msg); break;
                case Protocol.FILE_CHUNK: handleFileChunk(msg);  break;
                case Protocol.FILE_END:    handleFileEnd(msg);    break;
                default:
                    send(new Message(Protocol.FAIL, "", msg.getFrom(),
                            "Unknown message type: " + type));
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Handle msg error", ex);
            try {
                send(new Message(Protocol.FAIL, "", msg.getFrom(),
                        "Server error: " + ex.getMessage()));
            } catch (IOException ignored) {}
        }
    }

    private void handleRegister(Message msg) throws IOException {
        if (!(msg.getPayload() instanceof String[] data) || data.length < 2) {
            send(new Message(Protocol.ERR_INVALID_ARGS, "", msg.getFrom(),
                    "REGISTER needs [username, password]"));
            return;
        }
        String u = data[0].trim();
        String p = data[1];

        if (u.isEmpty() || p.isEmpty()) {
            send(new Message(Protocol.ERR_INVALID_ARGS, "", msg.getFrom(),
                    "Username/password cannot be empty"));
            return;
        }
        if (db.usernameExists(u)) {
            send(new Message(Protocol.ERR_USERNAME_EXISTS, "", msg.getFrom(),
                    "Username already exists"));
            return;
        }
        if (db.register(u, p)) {
            send(new Message(Protocol.OK, "", msg.getFrom(), "Register successful"));
            LOG.info("Registered: " + u);
        } else {
            send(new Message(Protocol.FAIL, "", msg.getFrom(), "Register failed"));
        }
    }

    private void handleLogin(Message msg) throws IOException {
        if (!(msg.getPayload() instanceof String[] data) || data.length < 2) {
            send(new Message(Protocol.ERR_INVALID_ARGS, "", msg.getFrom(),
                    "LOGIN needs [username, password]"));
            return;
        }
        String u = data[0].trim();
        String p = data[1];

        if (!db.verifyLogin(u, p)) {
            send(new Message(Protocol.ERR_BAD_CRED, "", msg.getFrom(),
                    "Invalid credentials"));
            return;
        }
        if (db.isOnline(u)) {
            send(new Message(Protocol.FAIL, "", msg.getFrom(),
                    "Account already logged in"));
            return;
        }

        this.username = u;
        db.login(u, this);

        send(new Message(Protocol.OK, "", u, "Login successful"));
        LOG.info("Login: " + u + " from " + socket.getInetAddress());

        server.sendUserListTo(this);
        server.broadcastSystemNotify("*** " + u + " is online ***");
    }

    private void handleLogout(Message msg) throws IOException {
        send(new Message(Protocol.OK, "", username, "Logged out"));
        running = false;
    }

    private void handleListUsers(Message msg) throws IOException {
        if (!isAuthenticated()) {
            send(new Message(Protocol.ERR_NOT_LOGGED_IN, "", null, "Not logged in"));
            return;
        }
        server.sendUserListTo(this);
    }

    private void handleChatSend(Message msg) throws IOException {
        if (!isAuthenticated()) {
            send(new Message(Protocol.ERR_NOT_LOGGED_IN, "", msg.getFrom(), "Not logged in"));
            return;
        }
        String to = msg.getTo();
        if (to == null || to.isEmpty()) {
            send(new Message(Protocol.ERR_INVALID_ARGS, "", username, "Missing recipient"));
            return;
        }
        ClientHandler target = db.getHandler(to);
        if (target == null) {
            send(new Message(Protocol.ERR_USER_OFFLINE, "", username,
                    "User '" + to + "' is not online"));
            return;
        }
        target.send(new Message(Protocol.CHAT_RECV, username, to, msg.getPayload()));
    }

    private void handleFileOffer(Message msg) throws IOException {
        if (!isAuthenticated()) {
            send(new Message(Protocol.ERR_NOT_LOGGED_IN, "", msg.getFrom(), "Not logged in"));
            return;
        }
        String to = msg.getTo();
        ClientHandler target = db.getHandler(to);
        if (target == null) {
            send(new Message(Protocol.ERR_USER_OFFLINE, "", username,
                    "User '" + to + "' is not online"));
            return;
        }
        target.send(new Message(Protocol.FILE_OFFER_INCOMING, username, to, msg.getPayload()));
    }

    private void handleFileAccept(Message msg) throws IOException {
        if (!isAuthenticated()) return;
        String transferId = (String) msg.getPayload();
        String to = msg.getTo();
        ClientHandler sender = db.getHandler(to);
        if (sender != null) {
            sender.send(new Message(Protocol.FILE_ACCEPTED, username, to, transferId));
        }
    }

    private void handleFileReject(Message msg) throws IOException {
        if (!isAuthenticated()) return;
        Object payload = msg.getPayload();
        String transferId, reason;
        if (payload instanceof String[] arr && arr.length >= 2) {
            transferId = arr[0];
            reason     = arr[1];
        } else {
            transferId = String.valueOf(payload);
            reason     = "rejected";
        }
        ClientHandler sender = db.getHandler(msg.getTo());
        if (sender != null) {
            sender.send(new Message(Protocol.FILE_REJECTED,
                    username, msg.getTo(), new String[]{transferId, reason}));
        }
    }

    private void handleFileChunk(Message msg) throws IOException {
        if (!isAuthenticated()) return;
        ClientHandler target = db.getHandler(msg.getTo());
        if (target == null) return;
        target.send(new Message(Protocol.FILE_CHUNK_INCOMING,
                username, msg.getTo(), msg.getPayload()));
    }

    private void handleFileEnd(Message msg) throws IOException {
        if (!isAuthenticated()) return;
        String transferId = (String) msg.getPayload();
        ClientHandler target = db.getHandler(msg.getTo());
        if (target != null) {
            target.send(new Message(Protocol.FILE_COMPLETE, username, msg.getTo(), transferId));
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                db.logout(username);
                LOG.info("Logout: " + username);
                server.broadcastSystemNotify("*** " + username + " is offline ***");
            }
            running = false;
            if (ois != null) try { ois.close(); } catch (IOException ignored) {}
            if (oos != null) try { oos.close(); } catch (IOException ignored) {}
            if (socket != null && !socket.isClosed())
                try { socket.close(); } catch (IOException ignored) {}
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Cleanup error", ex);
        }
    }
}
