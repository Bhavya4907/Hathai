package com.chatapp;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ChatSignup extends Application {

    private TextField usernameField = new TextField();
    private PasswordField passwordField = new PasswordField();
    private Button signupButton = new Button("Create Account");
    private ProgressIndicator loadingIndicator = new ProgressIndicator();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HathaiM - Cloud Signup");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));
        grid.setStyle("-fx-background-color: #ffffff;");

        Label headerLabel = new Label("Join the Chat");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        grid.add(headerLabel, 0, 0, 2, 1);

        grid.add(new Label("Username:"), 0, 1);
        usernameField.setPromptText("Pick a username");
        grid.add(usernameField, 1, 1);

        grid.add(new Label("Password:"), 0, 2);
        passwordField.setPromptText("Create a password");
        grid.add(passwordField, 1, 2);

        // Loading indicator (hidden by default)
        loadingIndicator.setVisible(false);
        loadingIndicator.setPrefSize(20, 20);

        signupButton.setMaxWidth(Double.MAX_VALUE);
        signupButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Hyperlink loginLink = new Hyperlink("Already have an account? Login");
        loginLink.setOnAction(e -> {
            new ChatLogin().start(new Stage());
            primaryStage.close();
        });

        HBox buttonBox = new HBox(10, signupButton, loadingIndicator);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(buttonBox, 1, 3);
        grid.add(loginLink, 1, 4);

        signupButton.setOnAction(e -> handleSignup(primaryStage));

        Scene scene = new Scene(grid, 400, 350);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleSignup(Stage stage) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Form Error", "Please fill in all fields.");
            return;
        }

        signupButton.setDisable(true);
        loadingIndicator.setVisible(true);

        Task<Boolean> signupTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // Log for debugging in IntelliJ console
                System.out.println("Attempting to connect to Aiven...");

                Connection conn = ChatDBConnection.getConnection();

                // This prevents the NullPointerException
                if (conn == null) {
                    throw new Exception("Connection failed: Database connection object is null.");
                }

                try (conn) {
                    // 1. Check existence
                    String checkSql = "SELECT username FROM users WHERE username=?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next()) {
                        throw new Exception("Username already exists!");
                    }

                    // 2. Insert
                    String insertSql = "INSERT INTO users(username, password) VALUES (?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, password);
                    return insertStmt.executeUpdate() > 0;
                }
            }
        };

        signupTask.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully!");
            new ChatLogin().start(new Stage());
            stage.close();
        });

        signupTask.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            signupButton.setDisable(false);

            // This will now catch the actual error (like WrongArgument or NullPointer)
            Throwable ex = signupTask.getException();
            ex.printStackTrace(); // Look at your IntelliJ console for the full red text!

            String errorMsg = (ex.getMessage() != null) ? ex.getMessage() : "An unknown error occurred.";
            showAlert(Alert.AlertType.ERROR, "Signup Failed", errorMsg);
        });

        new Thread(signupTask).start();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}