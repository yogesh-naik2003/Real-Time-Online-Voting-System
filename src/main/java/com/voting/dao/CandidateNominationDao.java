package com.voting.dao;

import com.voting.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CandidateNominationDao {
    public void createNomination(int userId, int electionId, String manifesto) throws SQLException {
        String sql = "INSERT INTO candidate_nominations(user_id, election_id, manifesto, status) VALUES(?, ?, ?, 'PENDING') " +
                "ON DUPLICATE KEY UPDATE manifesto = VALUES(manifesto), status = 'PENDING', admin_note = NULL, reviewed_at = NULL";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, electionId);
            ps.setString(3, manifesto);
            ps.executeUpdate();
        }
    }

    public Map<String, Object> findById(int nominationId) throws SQLException {
        List<Map<String, Object>> rows = query("WHERE n.id = ?", nominationId, false);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> findAll() throws SQLException {
        return query("", 0, false);
    }

    public List<Map<String, Object>> findByUser(int userId) throws SQLException {
        return query("WHERE n.user_id = ?", userId, false);
    }

    public void updateStatus(int nominationId, String status, String adminNote) throws SQLException {
        String sql = "UPDATE candidate_nominations SET status = ?, admin_note = ?, reviewed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, adminNote);
            ps.setInt(3, nominationId);
            ps.executeUpdate();
        }
    }

    public void updateDocumentStatus(int nominationId, String status, String note) throws SQLException {
        String sql = "UPDATE candidate_nominations SET document_status = ?, document_note = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, note);
            ps.setInt(3, nominationId);
            ps.executeUpdate();
        }
    }

    private List<Map<String, Object>> query(String whereClause, int value, boolean unused) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        String sql = "SELECT n.*, u.name AS user_name, u.email, u.profile_photo_path, e.title AS election_title " +
                "FROM candidate_nominations n " +
                "JOIN users u ON u.id = n.user_id " +
                "JOIN elections e ON e.id = n.election_id " +
                whereClause + " ORDER BY n.created_at DESC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (whereClause.contains("?")) {
                ps.setInt(1, value);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("userId", rs.getInt("user_id"));
                    row.put("electionId", rs.getInt("election_id"));
                    row.put("manifesto", rs.getString("manifesto"));
                    row.put("status", rs.getString("status"));
                    row.put("adminNote", rs.getString("admin_note"));
                    try { row.put("documentStatus", rs.getString("document_status")); } catch (SQLException ignored) { row.put("documentStatus", "PENDING"); }
                    try { row.put("documentNote", rs.getString("document_note")); } catch (SQLException ignored) { row.put("documentNote", ""); }
                    row.put("createdAt", rs.getTimestamp("created_at"));
                    row.put("reviewedAt", rs.getTimestamp("reviewed_at"));
                    row.put("userName", rs.getString("user_name"));
                    row.put("email", rs.getString("email"));
                    row.put("profilePhotoPath", rs.getString("profile_photo_path"));
                    row.put("electionTitle", rs.getString("election_title"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
