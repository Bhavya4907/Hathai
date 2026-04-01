package com.chatapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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
    private Map<String, VBox> chatHistories = new HashMap<>();
    private ScrollPane scrollPane = new ScrollPane();
    private TextField messageField = new TextField();
    private ObservableList<String> activeUsers = FXCollections.observableArrayList("Global");
    private String currentTarget = "Global";

    public ChatClient(String username) { this.username = username; }
    public ChatClient() {} // Required for JavaFX launch

    @Override
    public void start(Stage stage) {
        ListView<String> userSidebar = new ListView<>(activeUsers);
        userSidebar.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) switchChat(val);
        });

        Button attachBtn = new Button("📎");
        attachBtn.setOnAction(e -> sendImage());

        HBox inputBar = new HBox(10, attachBtn, messageField);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        inputBar.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setLeft(userSidebar);
        root.setCenter(scrollPane);
        root.setBottom(inputBar);

        messageField.setOnAction(e -> sendMessage());

        switchChat("Global");
        stage.setScene(new Scene(root, 800, 500));
        stage.setTitle("HathaiM - " + username);
        stage.show();
        connect();
    }

    private void switchChat(String target) {
        this.currentTarget = target;
        chatHistories.putIfAbsent(target, new VBox(10));
        scrollPane.setContent(chatHistories.get(target));
    }

    private void sendMessage() {
        String txt = messageField.getText().trim();
        if (txt.isEmpty()) return;
        if (currentTarget.equals("Global")) {
            client.send(txt);
        } else {
            client.send("TO:" + currentTarget + ":" + txt);
            displayText(currentTarget, "You: " + txt);
        }
        messageField.clear();
    }

    private void sendImage() {
        File file = new FileChooser().showOpenDialog(null);
        if (file != null) {
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);
                client.send("IMG:" + currentTarget + ":" + base64);
                displayImage(currentTarget, "You", new Image(file.toURI().toString()));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void displayText(String target, String msg) {
        Platform.runLater(() -> {
            chatHistories.putIfAbsent(target, new VBox(5));
            chatHistories.get(target).getChildren().add(new Label(msg));
            scrollPane.setVvalue(1.0);
        });
    }

    private void displayImage(String target, String sender, Image img) {
        Platform.runLater(() -> {
            chatHistories.putIfAbsent(target, new VBox(5));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(200); iv.setPreserveRatio(true);
            chatHistories.get(target).getChildren().addAll(new Label(sender + ":"), iv);
            scrollPane.setVvalue(1.0);
        });
    }

    private void connect() {
        try {
            client = new ChatClientInternal(new URI("ws://centerbeam.proxy.rlwy.net:25610"));
            client.connect();
        } catch (Exception e) { e.printStackTrace(); }
    }

    class ChatClientInternal extends WebSocketClient {
        public ChatClientInternal(URI uri) { super(uri); }
        @Override public void onOpen(ServerHandshake h) { send(username); }
        @Override
        public void onMessage(String msg) {
            Platform.runLater(() -> {
                if (msg.startsWith("USERLIST_ADD:")) {
                    String user = msg.split(":")[1];
                    if (!activeUsers.contains(user) && !user.equals(username)) activeUsers.add(user);
                } else if (msg.startsWith("MSG_GLOBAL:")) {
                    String[] p = msg.split(":", 3);
                    displayText("Global", p[1] + ": " + p[2]);
                } else if (msg.startsWith("MSG_PRIVATE:")) {
                    String[] p = msg.split(":", 3);
                    displayText(p[1], p[1] + ": " + p[2]);
                } else if (msg.startsWith("IMG_RCV:")) {
                    String[] p = msg.split(":", 3);
                    Image img = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(p[2])));
                    displayImage("Global", p[1], img);
                }
            });
        }
        @Override public void onClose(int i, String s, boolean b) {}
        @Override public void onError(Exception e) {}
    }

    public static void main(String[] args) { launch(args); }
}