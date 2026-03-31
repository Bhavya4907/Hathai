package com.chatapp;

import javafx.application.Application;
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
import java.sql.SQLException;

public class ChatLogin extends Application {

    private TextField usernameField = new TextField();
    private PasswordField passwordField = new PasswordField();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HathaiM - Login");

        // --- 1. Layout (GridPane) ---
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setStyle("-fx-background-color: #f4f4f4;");

        // --- 2. UI Components ---
        Label sceneTitle = new Label("Welcome");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

        grid.add(new Label("Username:"), 0, 1);
        grid.add(usernameField, 1, 1);

        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);

        Button loginBtn = new Button("Sign In");
        Button signupBtn = new Button("Sign Up");

        // Styling buttons
        loginBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");

        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().addAll(signupBtn, loginBtn);
        grid.add(hbBtn, 1, 4);

        // --- 3. Event Handling ---
        loginBtn.setOnAction(e -> handleLogin(primaryStage));

        signupBtn.setOnAction(e -> {
            // Logic to switch to Signup Stage
            System.out.println("Switching to Signup...");
        });

        Scene scene = new Scene(grid, 350, 275);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleLogin(Stage stage) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Form Error!", "Please enter your username and password");
            return;
        }

        try (Connection connection = ChatDBConnection.getConnection()) {
            String query = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // Success! Launch the Chat Client
                // Assuming your ChatClient extends Application or handles a Stage
                new ChatClient(username).start(new Stage());
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password.");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}