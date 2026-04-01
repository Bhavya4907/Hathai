package com.chatapp;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // If you pass "server" as an argument, it starts the WebSocket Server
        // Otherwise, it starts the JavaFX GUI
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            System.out.println("Starting in SERVER mode...");
            ChatServer.main(args);
        } else {
            System.out.println("Starting in CLIENT mode (GUI)...");
            // This calls your JavaFX Main class
            Application.launch(ChatSignup.class, args);
        }
    }
}