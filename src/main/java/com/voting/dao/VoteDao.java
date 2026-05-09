package com.voting.dao;

import com.voting.model.VoteStat;
import com.voting.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VoteDao {
    public boolean hasUserVoted(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes WHERE user_id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean hasUserVotedInElection(int userId, int electionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes WHERE user_id = ? AND election_id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void castVote(int userId, int electionId, int candidateId, String receiptId, String ipAddress, String userAgent, String signature) throws SQLException {
        String sql = "INSERT INTO votes(user_id, election_id, candidate_id, receipt_id, ip_address, user_agent, digital_signature) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, electionId);
            ps.setInt(3, candidateId);
            ps.setString(4, receiptId);
            ps.setString(5, ipAddress);
            ps.setString(6, userAgent);
            ps.setString(7, signature);
            ps.executeUpdate();
        }
    }

    public List<VoteStat> getResultsByElection(int electionId) throws SQLException {
        List<VoteStat> results = new ArrayList<>();
        String sql = "SELECT c.id, c.name, COUNT(v.id) AS total_votes " +
                "FROM candidates c LEFT JOIN votes v ON c.id = v.candidate_id " +
                "WHERE c.election_id = ? " +
                "GROUP BY c.id, c.name ORDER BY total_votes DESC, c.name ASC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new VoteStat(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("total_votes")
                    ));
                }
            }
        }
        return results;
    }

    public int countVotes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countVotesByElection(int electionId) throws SQLException {
        String sql = "SELECT COUNT(v.id) FROM votes v WHERE v.election_id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Map<String, Object>> getVoteHistoryByUser(int userId) throws SQLException {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT v.receipt_id, v.created_at, v.digital_signature, e.id AS election_id, e.title, e.election_date, e.status " +
                "FROM votes v " +
                "JOIN elections e ON e.id = v.election_id " +
                "WHERE v.user_id = ? " +
                "ORDER BY v.created_at DESC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("receiptId", rs.getString("receipt_id"));
                    item.put("createdAt", rs.getTimestamp("created_at"));
                    item.put("electionId", rs.getInt("election_id"));
                    item.put("title", rs.getString("title"));
                    item.put("electionDate", rs.getDate("election_date"));
                    item.put("status", rs.getString("status"));
                    try { item.put("digitalSignature", rs.getString("digital_signature")); } catch (SQLException e) { item.put("digitalSignature", "N/A"); }
                    history.add(item);
                }
            }
        }
        return history;
    }

    public Map<String, Object> findReceipt(String receiptId, int userId) throws SQLException {
        String sql = "SELECT v.receipt_id, v.created_at, v.digital_signature, e.title, e.election_date, e.status " +
                "FROM votes v JOIN elections e ON e.id = v.election_id " +
                "WHERE v.receipt_id = ? AND v.user_id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, receiptId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> receipt = new LinkedHashMap<>();
                    receipt.put("receiptId", rs.getString("receipt_id"));
                    receipt.put("createdAt", rs.getTimestamp("created_at"));
                    receipt.put("digitalSignature", rs.getString("digital_signature"));
                    receipt.put("title", rs.getString("title"));
                    receipt.put("electionDate", rs.getDate("election_date"));
                    receipt.put("status", rs.getString("status"));
                    return receipt;
                }
            }
        }
        return null;
    }

    public List<Map<String, Object>> getHourlyActivityByElection(int electionId) throws SQLException {
        Map<String, Integer> totalsByHour = new HashMap<>();
        String sql = "SELECT DATE_FORMAT(v.created_at, '%Y-%m-%d %H:00') AS hour_label, COUNT(*) AS total_votes " +
                "FROM votes v " +
                "WHERE v.election_id = ? AND v.created_at >= DATE_SUB(NOW(), INTERVAL 11 HOUR) " +
                "GROUP BY hour_label";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totalsByHour.put(rs.getString("hour_label"), rs.getInt("total_votes"));
                }
            }
        }

        List<Map<String, Object>> activity = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).minusHours(11);
        DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00");
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("ha");
        for (int i = 0; i < 12; i++) {
            Map<String, Object> point = new LinkedHashMap<>();
            String key = cursor.format(keyFormatter);
            point.put("label", cursor.format(labelFormatter).toLowerCase());
            point.put("totalVotes", totalsByHour.getOrDefault(key, 0));
            activity.add(point);
            cursor = cursor.plusHours(1);
        }
        return activity;
    }

    public Map<String, Object> getIntegritySummary() throws SQLException {
        String sql = "SELECT COUNT(*) AS total_votes, " +
                "SUM(CASE WHEN receipt_id IS NULL OR receipt_id = '' THEN 1 ELSE 0 END) AS missing_receipts, " +
                "SUM(CASE WHEN digital_signature IS NULL OR digital_signature = '' THEN 1 ELSE 0 END) AS missing_signatures, " +
                "COUNT(DISTINCT receipt_id) AS distinct_receipts " +
                "FROM votes";
        Map<String, Object> summary = new LinkedHashMap<>();
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int totalVotes = rs.getInt("total_votes");
                int distinctReceipts = rs.getInt("distinct_receipts");
                int missingReceipts = rs.getInt("missing_receipts");
                int missingSignatures = rs.getInt("missing_signatures");
                int duplicateReceipts = Math.max(0, totalVotes - distinctReceipts - missingReceipts);
                boolean verified = missingReceipts == 0 && missingSignatures == 0 && duplicateReceipts == 0;
                summary.put("totalVotes", totalVotes);
                summary.put("missingReceipts", missingReceipts);
                summary.put("missingSignatures", missingSignatures);
                summary.put("duplicateReceipts", duplicateReceipts);
                summary.put("verified", verified);
            }
        }
        if (summary.isEmpty()) {
            summary.put("totalVotes", 0);
            summary.put("missingReceipts", 0);
            summary.put("missingSignatures", 0);
            summary.put("duplicateReceipts", 0);
            summary.put("verified", true);
        }
        return summary;
    }

    public List<Map<String, Object>> getEvidenceRows() throws SQLException {
        String sql = "SELECT v.id, v.election_id, e.title AS election_title, v.user_id, u.voter_id_number, " +
                "v.candidate_id, c.name AS candidate_name, v.receipt_id, v.ip_address, v.digital_signature, v.created_at " +
                "FROM votes v " +
                "LEFT JOIN elections e ON e.id = v.election_id " +
                "LEFT JOIN users u ON u.id = v.user_id " +
                "LEFT JOIN candidates c ON c.id = v.candidate_id " +
                "ORDER BY v.created_at DESC";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("voteId", rs.getInt("id"));
                row.put("electionId", rs.getInt("election_id"));
                row.put("electionTitle", rs.getString("election_title"));
                row.put("userId", rs.getInt("user_id"));
                row.put("voterId", rs.getString("voter_id_number"));
                row.put("candidateId", rs.getInt("candidate_id"));
                row.put("candidateName", rs.getString("candidate_name"));
                row.put("receiptId", rs.getString("receipt_id"));
                row.put("ipAddress", rs.getString("ip_address"));
                row.put("digitalSignature", rs.getString("digital_signature"));
                row.put("createdAt", rs.getTimestamp("created_at"));
                rows.add(row);
            }
        }
        return rows;
    }

    public List<Map<String, Object>> getCenterTurnoutByElection(int electionId) throws SQLException {
        String sql = "SELECT COALESCE(NULLIF(u.election_center, ''), 'Unassigned') AS center_name, " +
                "COUNT(DISTINCT v.id) AS votes_cast " +
                "FROM votes v JOIN users u ON u.id = v.user_id " +
                "WHERE v.election_id = ? GROUP BY center_name ORDER BY center_name";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("centerName", rs.getString("center_name"));
                    row.put("votesCast", rs.getInt("votes_cast"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
