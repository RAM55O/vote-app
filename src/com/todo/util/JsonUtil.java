package com.todo.util;

import com.todo.model.Todo;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {

    public static String escape(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (ch < ' ') {
                        String hex = String.format("\\u%04x", (int) ch);
                        sb.append(hex);
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    public static String toJson(Todo todo) {
        if (todo == null) return "null";
        return "{" +
                "\"id\":" + (todo.getId() != null ? todo.getId() : "null") + "," +
                "\"title\":\"" + escape(todo.getTitle() != null ? todo.getTitle() : "") + "\"," +
                "\"description\":\"" + escape(todo.getDescription() != null ? todo.getDescription() : "") + "\"," +
                "\"priority\":\"" + escape(todo.getPriority() != null ? todo.getPriority() : "MEDIUM") + "\"," +
                "\"completed\":" + todo.isCompleted() + "," +
                "\"dueDate\":" + (todo.getDueDate() != null ? "\"" + escape(todo.getDueDate()) + "\"" : "null") + "," +
                "\"createdAt\":" + (todo.getCreatedAt() != null ? "\"" + escape(todo.getCreatedAt()) + "\"" : "null") + "," +
                "\"updatedAt\":" + (todo.getUpdatedAt() != null ? "\"" + escape(todo.getUpdatedAt()) + "\"" : "null") +
                "}";
    }

    public static String toJson(List<Todo> todos) {
        if (todos == null) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < todos.size(); i++) {
            sb.append(toJson(todos.get(i)));
            if (i < todos.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static Todo fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return new Todo();
        Todo todo = new Todo();

        // Extract ID
        Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
        Matcher idMatcher = idPattern.matcher(json);
        if (idMatcher.find()) {
            todo.setId(Integer.parseInt(idMatcher.group(1)));
        }

        // Extract title
        Pattern titlePattern = Pattern.compile("\"title\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        Matcher titleMatcher = titlePattern.matcher(json);
        if (titleMatcher.find()) {
            todo.setTitle(unescape(titleMatcher.group(1)));
        }

        // Extract description
        Pattern descPattern = Pattern.compile("\"description\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        Matcher descMatcher = descPattern.matcher(json);
        if (descMatcher.find()) {
            todo.setDescription(unescape(descMatcher.group(1)));
        }

        // Extract priority
        Pattern priorityPattern = Pattern.compile("\"priority\"\\s*:\\s*\"([A-Za-z]+)\"");
        Matcher priorityMatcher = priorityPattern.matcher(json);
        if (priorityMatcher.find()) {
            todo.setPriority(priorityMatcher.group(1).toUpperCase());
        }

        // Extract completed
        Pattern completedPattern = Pattern.compile("\"completed\"\\s*:\\s*(true|false)");
        Matcher completedMatcher = completedPattern.matcher(json);
        if (completedMatcher.find()) {
            todo.setCompleted(Boolean.parseBoolean(completedMatcher.group(1)));
        }

        // Extract dueDate
        Pattern dueDatePattern = Pattern.compile("\"dueDate\"\\s*:\\s*\"([0-9\\-]+)\"");
        Matcher dueDateMatcher = dueDatePattern.matcher(json);
        if (dueDateMatcher.find()) {
            todo.setDueDate(dueDateMatcher.group(1));
        }

        return todo;
    }

    private static String unescape(String s) {
        if (s == null) return null;
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\f", "\f");
    }
}
