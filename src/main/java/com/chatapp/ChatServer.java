package com.chatapp;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer extends WebSocketServer {

    // Thread-safe maps to track who is online
    private final Map<WebSocket, String> socketToUser = new ConcurrentHashMap<>();
    private final Map<String, WebSocket> userToSocket = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    public static void main(String[] args) {
        // Railway provides the PORT environment variable automatically
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        ChatServer server = new ChatServer(port);
        server.start();
        System.out.println("Cloud Chat Server started on port: " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection established: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // If the socket isn't mapped to a name, the first message is the username (Login)
        if (!socketToUser.containsKey(conn)) {
            handleLogin(conn, message);
        } else {
            handleTraffic(conn, message);
        }
    }

    private void handleLogin(WebSocket conn, String username) {
        socketToUser.put(conn, username);
        userToSocket.put(username, conn);

        // 1. Tell the NEW user about everyone already online
        for (String existingUser : userToSocket.keySet()) {
            conn.send("USERLIST_ADD:" + existingUser);
        }

        // 2. Tell EVERYONE that this new user has joined
        broadcast("USERLIST_ADD:" + username);
        broadcast("MSG_GLOBAL:System:" + username + " joined the chat");
        System.out.println("User Logged In: " + username);
    }

    private void handleTraffic(WebSocket conn, String message) {
        String sender = socketToUser.get(conn);

        if (message.startsWith("IMG:")) {
            // Format: IMG:target:base64Data
            // We rebroadcast it so the client knows who sent it
            broadcast(message.replace("IMG:", "IMG_RCV:" + sender + ":"));
        }
        else if (message.startsWith("TO:")) {
            // Format: TO:target:content
            String[] parts = message.split(":", 3);
            if (parts.length < 3) return;

            String target = parts[1];
            String content = parts[2];
            WebSocket targetWs = userToSocket.get(target);

            if (targetWs != null && targetWs.isOpen()) {
                targetWs.send("MSG_PRIVATE:" + sender + ":" + content);
            }
        }
        else {
            // Standard message sent to the Global room
            broadcast("MSG_GLOBAL:" + sender + ":" + message);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String user = socketToUser.remove(conn);
        if (user != null) {
            userToSocket.remove(user);
            // Tell clients to remove this person from their sidebar
            broadcast("USERLIST_REMOVE:" + user);
            broadcast("MSG_GLOBAL:System:" + user + " has left the chat.");
            System.out.println("User Disconnected: " + user);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error on connection " + (conn != null ? conn.getRemoteSocketAddress() : "null") + ":" + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Server is now ready to receive connections!");
        setConnectionLostTimeout(30); // Clean up ghost connections every 30s
    }
}