package com.todo.dao;

import com.todo.model.Todo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TodoDao {
    private static final String DB_URL = "jdbc:sqlite:todos.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    public TodoDao() {
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS todos (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "title TEXT NOT NULL, " +
                     "description TEXT, " +
                     "priority TEXT DEFAULT 'MEDIUM', " +
                     "completed BOOLEAN DEFAULT 0, " +
                     "due_date TEXT, " +
                     "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                     "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                     ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Todo> getAll(String status, String search) throws SQLException {
        List<Todo> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, title, description, priority, completed, due_date, created_at, updated_at FROM todos WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if ("completed".equalsIgnoreCase(status)) {
            sql.append("AND completed = 1 ");
        } else if ("active".equalsIgnoreCase(status) || "pending".equalsIgnoreCase(status)) {
            sql.append("AND completed = 0 ");
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (title LIKE ? OR description LIKE ?) ");
            String keyword = "%" + search.trim() + "%";
            params.add(keyword);
            params.add(keyword);
        }

        sql.append("ORDER BY completed ASC, CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 ELSE 4 END, id DESC");

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public Todo getById(int id) throws SQLException {
        String sql = "SELECT id, title, description, priority, completed, due_date, created_at, updated_at FROM todos WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    public Todo create(Todo todo) throws SQLException {
        String sql = "INSERT INTO todos (title, description, priority, completed, due_date, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, todo.getTitle());
            pstmt.setString(2, todo.getDescription());
            pstmt.setString(3, todo.getPriority() != null ? todo.getPriority() : "MEDIUM");
            pstmt.setInt(4, todo.isCompleted() ? 1 : 0);
            pstmt.setString(5, todo.getDueDate());
            
            pstmt.executeUpdate();
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    return getById(id);
                }
            }
        }
        return null;
    }

    public boolean update(Todo todo) throws SQLException {
        String sql = "UPDATE todos SET title = ?, description = ?, priority = ?, completed = ?, due_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, todo.getTitle());
            pstmt.setString(2, todo.getDescription());
            pstmt.setString(3, todo.getPriority() != null ? todo.getPriority() : "MEDIUM");
            pstmt.setInt(4, todo.isCompleted() ? 1 : 0);
            pstmt.setString(5, todo.getDueDate());
            pstmt.setInt(6, todo.getId());

            int rows = pstmt.executeUpdate();
            return rows > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM todos WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        }
    }

    private Todo mapResultSet(ResultSet rs) throws SQLException {
        return new Todo(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("priority"),
            rs.getInt("completed") == 1,
            rs.getString("due_date"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        );
    }
}
