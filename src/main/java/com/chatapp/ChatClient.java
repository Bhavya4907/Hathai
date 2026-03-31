package com.chatapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.*;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ChatClient extends Application {

    private String username;
    private ChatClientInternal client;

    // UI Components upgraded for Images
    private Map<String, VBox> chatHistories = new HashMap<>();
    private ScrollPane scrollPane = new ScrollPane();
    private VBox currentChatContainer;

    private TextField messageField = new TextField();
    private ListView<String> userSidebar = new ListView<>();
    private ObservableList<String> activeUsers = FXCollections.observableArrayList();
    private String currentTarget = "Global";

    public ChatClient(String username) { this.username = username; }
    public ChatClient() {}

    @Override
    public void start(Stage primaryStage) {
        // --- 1. Sidebar ---
        userSidebar.setItems(activeUsers);
        activeUsers.add("Global");
        userSidebar.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) switchChat(newVal);
        });

        // --- 2. Chat Area Setup ---
        scrollPane.setFitToWidth(true);
        switchChat("Global"); // Initialize default chat

        // --- 3. Input Area (Text + Image Button) ---
        Button attachBtn = new Button("📎");
        attachBtn.setOnAction(e -> sendImage());

        HBox inputBar = new HBox(10, attachBtn, messageField);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        inputBar.setPadding(new Insets(10));

        // --- 4. Layout ---
        BorderPane root = new BorderPane();
        root.setLeft(userSidebar);
        root.setCenter(scrollPane);
        root.setBottom(inputBar);

        messageField.setOnAction(e -> sendMessage());

        primaryStage.setTitle("HathaiM - " + username);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        connectToServer();
    }

    private void switchChat(String target) {
        this.currentTarget = target;
        chatHistories.putIfAbsent(target, new VBox(10));
        currentChatContainer = chatHistories.get(target);
        scrollPane.setContent(currentChatContainer);
    }

    private void sendImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.gif"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);

                // Format: IMG:target:base64Data
                client.send("IMG:" + currentTarget + ":" + base64);

                displayImage(currentTarget, "You", new Image(file.toURI().toString()));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;

        if (currentTarget.equals("Global")) {
            client.send(msg);
        } else {
            client.send("TO:" + currentTarget + ":" + msg);
            displayText(currentTarget, "You: " + msg);
        }
        messageField.clear();
    }

    private void displayText(String target, String msg) {
        Platform.runLater(() -> {
            chatHistories.putIfAbsent(target, new VBox(10));
            Label label = new Label(msg);
            label.setWrapText(true);
            chatHistories.get(target).getChildren().add(label);
            scrollPane.setVvalue(1.0);
        });
    }

    private void displayImage(String target, String sender, Image img) {
        Platform.runLater(() -> {
            chatHistories.putIfAbsent(target, new VBox(10));
            VBox container = chatHistories.get(target);

            Label nameLabel = new Label(sender + " sent an image:");
            ImageView iv = new ImageView(img);
            iv.setFitWidth(250);
            iv.setPreserveRatio(true);

            container.getChildren().addAll(nameLabel, iv);
            scrollPane.setVvalue(1.0);
        });
    }

    private void connectToServer() {
        try {
            // These are the details Railway just gave you
            String railwayDomain = "centerbeam.proxy.rlwy.net";
            int railwayPort = 25610;

            // Important: Keep the "ws://" prefix for WebSockets
            URI serverUri = new URI("ws://" + railwayDomain + ":" + railwayPort);

            client = new ChatClientInternal(serverUri);
            client.connect();

            System.out.println("Connecting to Railway Server at: " + serverUri);
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    class ChatClientInternal extends WebSocketClient {
        public ChatClientInternal(URI uri) { super(uri); }

        @Override
        public void onOpen(ServerHandshake h) {
            send(username);
            displayText("Global", "Connected as " + username);
        }

        @Override
        public void onMessage(String message) {
            if (message.startsWith("IMG:")) {
                String[] parts = message.split(":", 3);
                String sender = parts[1];
                byte[] data = Base64.getDecoder().decode(parts[2]);
                Image img = new Image(new ByteArrayInputStream(data));

                displayImage(sender.equals(username) ? currentTarget : sender, sender, img);
                Platform.runLater(() -> { if(!activeUsers.contains(sender)) activeUsers.add(sender); });
            } else {
                // Regular text logic
                displayText("Global", message);
            }
        }

        @Override public void onClose(int i, String s, boolean b) {}
        @Override public void onError(Exception e) {}
    }

    public static void main(String[] args) { launch(args); }
}