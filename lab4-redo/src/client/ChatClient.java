package client;

import common.ChatMessage;
import common.FileChunk;
import common.FileOffer;
import common.Message;
import common.Protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatClient {

    private static final Logger LOG = Logger.getLogger(ChatClient.class.getName());
    private static final int CHUNK_SIZE = 64 * 1024;

    private final String host;
    private final int port;
    private final Consumer<Message> inboundHandler;
    private final Consumer<FileTransferEvent> fileEventHandler;

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream  ois;

    private volatile boolean connected = false;
    private Thread readerThread;

    private final Map<String, FileWriter> incomingFiles = new ConcurrentHashMap<>();
    private final Map<String, FileOffer> outgoingOffers = new ConcurrentHashMap<>();
    private final BlockingQueue<Message> responseQueue = new LinkedBlockingQueue<>();
    private final ExecutorService fileSenderExecutor = Executors.newCachedThreadPool(r ->
            new Thread(r, "file-sender"));

    public ChatClient(String host, int port,
                      Consumer<Message> inboundHandler,
                      Consumer<FileTransferEvent> fileEventHandler) {
        this.host = host;
        this.port = port;
        this.inboundHandler = inboundHandler;
        this.fileEventHandler = fileEventHandler;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());
        connected = true;
        readerThread = new Thread(this::readLoop, "client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void disconnect() {
        connected = false;
        fileSenderExecutor.shutdownNow();
        try { if (oos != null) oos.close(); } catch (IOException ignored) {}
        try { if (ois != null) ois.close(); } catch (IOException ignored) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }

    private void send(Message m) throws IOException {
        if (!connected) throw new IOException("Not connected");
        synchronized (oos) {
            oos.writeObject(m);
            oos.flush();
        }
    }

    public Message register(String username, String password) throws IOException {
        responseQueue.clear();
        send(new Message(Protocol.REGISTER, "", "", new String[]{username, password}));
        return waitResponse(5_000);
    }

    public Message login(String username, String password) throws IOException {
        responseQueue.clear();
        send(new Message(Protocol.LOGIN, "", "", new String[]{username, password}));
        return waitResponse(5_000);
    }

    public Message requestUserList() throws IOException {
        responseQueue.clear();
        send(new Message(Protocol.LIST_USERS, username, "", null));
        return waitResponse(5_000);
    }

    public void logout() {
        try {
            if (connected) send(new Message(Protocol.LOGOUT, username, "", null));
        } catch (IOException ignored) {}
        disconnect();
    }

    private Message waitResponse(long timeoutMs) {
        try {
            Message m = responseQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            return m != null ? m : new Message(Protocol.FAIL, "", "", "Timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Message(Protocol.FAIL, "", "", "Interrupted");
        }
    }

    public void sendChat(String from, String to, String text) throws IOException {
        send(new Message(Protocol.CHAT_SEND, from, to, new ChatMessage(text)));
    }

    public void sendFile(String from, String to, java.io.File file, String transferId) throws IOException {
        if (!file.exists() || !file.isFile())
            throw new IOException("File not found: " + file);
        long size = file.length();
        FileOffer offer = new FileOffer(transferId, file.getName(), size, "");
        outgoingOffers.put(transferId, offer);
        send(new Message(Protocol.FILE_OFFER, from, to, offer));

        fileSenderExecutor.submit(() -> {
            try {
                long deadline = System.currentTimeMillis() + 30_000;
                while (outgoingOffers.containsKey(transferId)
                        && !Thread.currentThread().isInterrupted()
                        && System.currentTimeMillis() < deadline) {
                    if (outgoingOffers.get(transferId) == null) break;
                    Thread.sleep(100);
                }
                if (!outgoingOffers.containsKey(transferId)) return;
                outgoingOffers.put(transferId, null);

                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    byte[] buf = new byte[CHUNK_SIZE];
                    int seq = 0;
                    int n;
                    while ((n = fis.read(buf)) > 0) {
                        byte[] data = new byte[n];
                        System.arraycopy(buf, 0, data, 0, n);
                        send(new Message(Protocol.FILE_CHUNK, from, to,
                                new FileChunk(transferId, seq++, data)));
                    }
                }
                send(new Message(Protocol.FILE_END, from, to, transferId));
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "sendFile error", ex);
                try {
                    send(new Message(Protocol.FILE_ERROR, from, to,
                            new String[]{transferId, ex.getMessage()}));
                } catch (IOException ignored) {}
            }
        });
    }

    public void acceptFile(String from, String to, String transferId) throws IOException {
        send(new Message(Protocol.FILE_ACCEPT, from, to, transferId));
    }

    public void rejectFile(String from, String to, String transferId, String reason) throws IOException {
        send(new Message(Protocol.FILE_REJECT, from, to, new String[]{transferId, reason}));
    }

    private void onFileAccepted(String transferId) {
        outgoingOffers.put(transferId, null);
    }

    private void onFileRejected(String transferId, String reason) {
        outgoingOffers.remove(transferId);
        if (fileEventHandler != null) {
            fileEventHandler.accept(new FileTransferEvent(
                    FileTransferEvent.Type.REJECTED, transferId, null, reason));
        }
    }

    private void onFileComplete(String transferId) {
        if (fileEventHandler != null) {
            FileWriter fw = incomingFiles.remove(transferId);
            String path = fw != null ? fw.path : null;
            fileEventHandler.accept(new FileTransferEvent(
                    FileTransferEvent.Type.COMPLETED, transferId, path, null));
        }
    }

    private void onFileError(String transferId, String reason) {
        FileWriter fw = incomingFiles.remove(transferId);
        if (fw != null) fw.close();
        if (fileEventHandler != null) {
            fileEventHandler.accept(new FileTransferEvent(
                    FileTransferEvent.Type.FAILED, transferId, null, reason));
        }
    }

    private String username;

    private void readLoop() {
        try {
            while (connected) {
                Message msg;
                try {
                    msg = (Message) ois.readObject();
                } catch (java.io.EOFException | java.net.SocketException e) {
                    LOG.info("Server closed connection");
                    break;
                }

                byte type = msg.getType();

                switch (type) {
                    case Protocol.OK:
                    case Protocol.FAIL:
                    case Protocol.ERR_USERNAME_EXISTS:
                    case Protocol.ERR_BAD_CRED:
                    case Protocol.ERR_USER_OFFLINE:
                    case Protocol.ERR_NOT_LOGGED_IN:
                    case Protocol.ERR_INVALID_ARGS:
                    case Protocol.USER_LIST:
                        responseQueue.offer(msg);
                        break;

                    case Protocol.FILE_ACCEPTED: {
                        String tid = (String) msg.getPayload();
                        onFileAccepted(tid);
                        break;
                    }
                    case Protocol.FILE_REJECTED: {
                        Object pl = msg.getPayload();
                        if (pl instanceof String[] arr && arr.length >= 2) {
                            onFileRejected(arr[0], arr[1]);
                        } else {
                            onFileRejected(String.valueOf(pl), "rejected");
                        }
                        break;
                    }
                    case Protocol.FILE_COMPLETE: {
                        String tid = (String) msg.getPayload();
                        onFileComplete(tid);
                        break;
                    }
                    case Protocol.FILE_ERROR: {
                        Object pl = msg.getPayload();
                        String tid, reason;
                        if (pl instanceof String[] arr && arr.length >= 2) {
                            tid = arr[0]; reason = arr[1];
                        } else {
                            tid = String.valueOf(pl); reason = "error";
                        }
                        onFileError(tid, reason);
                        break;
                    }

                    case Protocol.FILE_OFFER_INCOMING: {
                        FileOffer offer = (FileOffer) msg.getPayload();
                        FileWriter fw = new FileWriter(offer);
                        fw.fromUser = msg.getFrom();
                        incomingFiles.put(offer.transferId, fw);
                        if (inboundHandler != null) inboundHandler.accept(msg);
                        break;
                    }

                    case Protocol.FILE_CHUNK_INCOMING: {
                        FileChunk chunk = (FileChunk) msg.getPayload();
                        FileWriter fw = incomingFiles.get(chunk.transferId);
                        if (fw != null && fw.isAccepted()) {
                            fw.writeChunk(chunk);
                            if (fileEventHandler != null) {
                                fileEventHandler.accept(new FileTransferEvent(
                                        FileTransferEvent.Type.PROGRESS,
                                        chunk.transferId, null,
                                        chunk.sequence + ":" + fw.receivedBytes));
                            }
                        }
                        break;
                    }

                    default:
                        if (inboundHandler != null) inboundHandler.accept(msg);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "readLoop error", ex);
        } finally {
            connected = false;
        }
    }

    public void markIncomingAccepted(String transferId, java.io.File saveTo) throws IOException {
        FileWriter fw = incomingFiles.get(transferId);
        if (fw != null) {
            fw.openForWrite(saveTo);
        }
    }

    public void markIncomingRejected(String transferId) {
        FileWriter fw = incomingFiles.remove(transferId);
        if (fw != null) fw.close();
    }

    public FileOffer getIncomingOffer(String transferId) {
        FileWriter fw = incomingFiles.get(transferId);
        return fw != null ? fw.offer : null;
    }

    public String getIncomingSender(String transferId) {
        FileWriter fw = incomingFiles.get(transferId);
        return fw != null ? fw.fromUser : null;
    }

    public List<FileOffer> getPendingIncomingOffers() {
        List<FileOffer> r = new ArrayList<>();
        incomingFiles.forEach((k, v) -> r.add(v.offer));
        return r;
    }

    private static class FileWriter {
        final FileOffer offer;
        String fromUser;
        java.io.FileOutputStream fos;
        java.io.File saveFile;
        long receivedBytes = 0;
        boolean accepted = false;
        String path;

        FileWriter(FileOffer offer) {
            this.offer = offer;
        }
        boolean isAccepted() { return accepted; }
        void openForWrite(java.io.File f) throws IOException {
            this.saveFile = f;
            this.path = f.getAbsolutePath();
            this.fos = new java.io.FileOutputStream(f);
            this.accepted = true;
        }
        void writeChunk(FileChunk c) {
            try {
                fos.write(c.data);
                receivedBytes += c.data.length;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        void close() {
            try { if (fos != null) fos.close(); } catch (IOException ignored) {}
        }
    }

    public static class FileTransferEvent {
        public enum Type { PROGRESS, COMPLETED, REJECTED, FAILED }
        public final Type type;
        public final String transferId;
        public final String path;
        public final String info;
        public FileTransferEvent(Type type, String transferId, String path, String info) {
            this.type = type;
            this.transferId = transferId;
            this.path = path;
            this.info = info;
        }
    }

    public void setUsername(String u) { this.username = u; }
    public String getUsername() { return username; }
}
