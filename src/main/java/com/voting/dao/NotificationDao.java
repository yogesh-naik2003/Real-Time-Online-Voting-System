package com.voting.dao;

import com.voting.model.User;
import com.voting.util.DatabaseUtil;
import com.voting.util.EmailUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {
    public void create(Integer userId, String title, String message, String type) throws SQLException {
        String sql = "INSERT INTO notifications(user_id, title, message, type) VALUES(?, ?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, title);
            ps.setString(3, message);
            ps.setString(4, type == null ? "info" : type);
            ps.executeUpdate();
        }
        if (userId != null) {
            User user = new UserDao().findById(userId);
            if (user != null) {
                try {
                    EmailUtil.sendNotification(user.getEmail(), user.getName(), title, message);
                } catch (IllegalStateException ex) {
                    System.err.println("[NotificationDao] Email notification skipped: " + ex.getMessage());
                }
            }
        }
    }

    public void notifyAllVoters(String title, String message, String type) throws SQLException {
        UserDao userDao = new UserDao();
        for (User user : userDao.findAllVoters()) {
            create(user.getId(), title, message, type);
        }
    }

    public List<String> findRecentMessagesForUser(int userId, int limit) throws SQLException {
        List<String> notifications = new ArrayList<>();
        String sql = "SELECT title, message FROM notifications WHERE user_id = ? OR user_id IS NULL ORDER BY created_at DESC LIMIT ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(rs.getString("title") + ": " + rs.getString("message"));
                }
            }
        }
        return notifications;
    }
}
