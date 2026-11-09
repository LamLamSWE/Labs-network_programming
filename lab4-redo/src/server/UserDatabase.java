package server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserDatabase {

    public static String hash(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(("lab4-salt::" + password).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Map<String, String>         accounts = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler>  sessions = new ConcurrentHashMap<>();

    public void seedDemoAccounts() {
        if (accounts.isEmpty()) {
            accounts.put("anh", hash("123456"));
            accounts.put("lam",   hash("123456"));
        }
    }

    public boolean usernameExists(String username) {
        return accounts.containsKey(username);
    }

    public boolean register(String username, String password) {
        if (username == null || username.isEmpty()
                || password == null || password.isEmpty()) return false;
        return accounts.putIfAbsent(username, hash(password)) == null;
    }

    public boolean verifyLogin(String username, String password) {
        String stored = accounts.get(username);
        return stored != null && stored.equals(hash(password));
    }

    public void login(String username, ClientHandler h)  { sessions.put(username, h); }
    public void logout(String username)                   { sessions.remove(username); }

    public boolean isOnline(String username)             { return sessions.containsKey(username); }
    public ClientHandler getHandler(String username)     { return sessions.get(username); }
    public Set<String>   onlineUsers()                   { return sessions.keySet(); }
}
