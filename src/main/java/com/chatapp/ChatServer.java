package com.chatapp;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer extends WebSocketServer {

    private final Map<WebSocket, String> socketToUser = new ConcurrentHashMap<>();
    private final Map<String, WebSocket> userToSocket = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    public static void main(String[] args) {
        // Use the PORT environment variable provided by Render/Railway
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        ChatServer server = new ChatServer(port);
        server.start();
        System.out.println("Chat Server started on port: " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (!socketToUser.containsKey(conn)) {
            handleLogin(conn, message);
        } else {
            handleChat(conn, message);
        }
    }

    private void handleLogin(WebSocket conn, String username) {
        socketToUser.put(conn, username);
        userToSocket.put(username, conn);
        broadcast("🟢 " + username + " joined the chat");
    }

    private void handleChat(WebSocket conn, String message) {
        String sender = socketToUser.get(conn);
        // Your existing TO: logic here...
        broadcast(sender + ": " + message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String user = socketToUser.remove(conn);
        if (user != null) {
            userToSocket.remove(user);
            broadcast("🔴 " + user + " left");
        }
    }

    @Override public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
    @Override public void onStart() { System.out.println("Server ready!"); }
}