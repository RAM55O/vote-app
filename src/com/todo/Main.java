package com.todo;

import com.sun.net.httpserver.HttpServer;
import com.todo.dao.TodoDao;
import com.todo.handler.StaticHandler;
import com.todo.handler.TodoHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            System.out.println("Initializing SQLite Database...");
            TodoDao todoDao = new TodoDao();

            // Create HTTP Server
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Register Handlers
            server.createContext("/api/todos", new TodoHandler(todoDao));
            server.createContext("/", new StaticHandler("public"));

            // Use multi-threaded executor for handling requests
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            server.start();

            System.out.println("=================================================");
            System.out.println("   Todo Application Server is Running!");
            System.out.println("   URL: http://localhost:" + PORT);
            System.out.println("   API: http://localhost:" + PORT + "/api/todos");
            System.out.println("=================================================");
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
