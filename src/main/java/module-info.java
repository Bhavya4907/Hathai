module com.chatapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.java_websocket;
    requires java.sql;

    opens com.chatapp to javafx.fxml;
    exports com.chatapp;
}