package com.todo.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.todo.dao.TodoDao;
import com.todo.model.Todo;
import com.todo.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodoHandler implements HttpHandler {
    private final TodoDao todoDao;

    public TodoHandler(TodoDao todoDao) {
        this.todoDao = todoDao;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Set CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath(); // e.g., /api/todos or /api/todos/5
        String[] segments = path.split("/");

        try {
            if (segments.length == 3 && "todos".equals(segments[2])) {
                // /api/todos
                if ("GET".equals(method)) {
                    handleGetAll(exchange, uri.getQuery());
                } else if ("POST".equals(method)) {
                    handleCreate(exchange);
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } else if (segments.length == 4 && "todos".equals(segments[2])) {
                // /api/todos/{id}
                int id;
                try {
                    id = Integer.parseInt(segments[3]);
                } catch (NumberFormatException e) {
                    sendJsonResponse(exchange, 400, "{\"error\":\"Invalid ID format\"}");
                    return;
                }

                if ("GET".equals(method)) {
                    handleGetOne(exchange, id);
                } else if ("PUT".equals(method)) {
                    handleUpdate(exchange, id);
                } else if ("DELETE".equals(method)) {
                    handleDelete(exchange, id);
                } else {
                    sendMethodNotAllowed(exchange);
                }
            } else {
                sendJsonResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, "{\"error\":\"Internal Server Error: " + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    private void handleGetAll(HttpExchange exchange, String query) throws Exception {
        Map<String, String> queryParams = parseQuery(query);
        String status = queryParams.get("status");
        String search = queryParams.get("search");

        List<Todo> todos = todoDao.getAll(status, search);
        String json = JsonUtil.toJson(todos);
        sendJsonResponse(exchange, 200, json);
    }

    private void handleGetOne(HttpExchange exchange, int id) throws Exception {
        Todo todo = todoDao.getById(id);
        if (todo != null) {
            sendJsonResponse(exchange, 200, JsonUtil.toJson(todo));
        } else {
            sendJsonResponse(exchange, 404, "{\"error\":\"Todo not found\"}");
        }
    }

    private void handleCreate(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Todo todo = JsonUtil.fromJson(body);
        if (todo.getTitle() == null || todo.getTitle().trim().isEmpty()) {
            sendJsonResponse(exchange, 400, "{\"error\":\"Title is required\"}");
            return;
        }

        Todo created = todoDao.create(todo);
        if (created != null) {
            sendJsonResponse(exchange, 201, JsonUtil.toJson(created));
        } else {
            sendJsonResponse(exchange, 500, "{\"error\":\"Failed to create todo\"}");
        }
    }

    private void handleUpdate(HttpExchange exchange, int id) throws Exception {
        Todo existing = todoDao.getById(id);
        if (existing == null) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Todo not found\"}");
            return;
        }

        String body = readRequestBody(exchange);
        Todo updateData = JsonUtil.fromJson(body);
        updateData.setId(id);

        if (updateData.getTitle() == null || updateData.getTitle().trim().isEmpty()) {
            updateData.setTitle(existing.getTitle());
        }
        if (updateData.getDescription() == null && body.contains("\"description\"")) {
            updateData.setDescription("");
        } else if (updateData.getDescription() == null) {
            updateData.setDescription(existing.getDescription());
        }
        if (updateData.getPriority() == null) {
            updateData.setPriority(existing.getPriority());
        }
        if (!body.contains("\"completed\"")) {
            updateData.setCompleted(existing.isCompleted());
        }
        if (!body.contains("\"dueDate\"")) {
            updateData.setDueDate(existing.getDueDate());
        }

        boolean success = todoDao.update(updateData);
        if (success) {
            Todo updated = todoDao.getById(id);
            sendJsonResponse(exchange, 200, JsonUtil.toJson(updated));
        } else {
            sendJsonResponse(exchange, 500, "{\"error\":\"Failed to update todo\"}");
        }
    }

    private void handleDelete(HttpExchange exchange, int id) throws Exception {
        boolean success = todoDao.delete(id);
        if (success) {
            sendJsonResponse(exchange, 200, "{\"message\":\"Todo deleted successfully\"}");
        } else {
            sendJsonResponse(exchange, 404, "{\"error\":\"Todo not found\"}");
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length > 0) {
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
                map.put(key, value);
            }
        }
        return map;
    }
}
