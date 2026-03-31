package com.chatapp;

import java.sql.Connection;
import java.sql.DriverManager;

public class ChatDBConnection {

    public static Connection getConnection() throws Exception {
        // --- DATA FROM YOUR PUBLIC PROXY URL ---
        String host = "interchange.proxy.rlwy.net";
        String port = "58847"; // Use the port from the Public URL
        String user = "root";
        String pass = "luvISJZiqRhnvNsseaxqulLlEwJEkzhn";
        String db   = "railway";

        // JDBC URL for Public Access
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            System.err.println("CONNECTION FAILED! Are you using the Public URL?");
            System.err.println("Attempted: " + url);
            throw e;
        }
    }
}