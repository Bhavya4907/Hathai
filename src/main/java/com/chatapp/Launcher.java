package com.chatapp;

public class Launcher {
    public static void main(String[] args) {
        // This starts the WebSocket server WITHOUT loading JavaFX graphics
        ChatServer.main(args);
    }
}