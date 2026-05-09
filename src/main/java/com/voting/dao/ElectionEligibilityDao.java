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

public class ElectionEligibilityDao {
    public boolean hasEligibilityList(int electionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM election_eligibility WHERE election_id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean isEligible(int electionId, int userId) throws SQLException {
        if (!hasEligibilityList(electionId)) {
            return true;
        }
        String sql = "SELECT COUNT(*) FROM election_eligibility WHERE election_id = ? AND user_id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int countEligibleVoters(int electionId) throws SQLException {
        if (!hasEligibilityList(electionId)) {
            return new UserDao().countVoters();
        }
        String sql = "SELECT COUNT(*) FROM election_eligibility ee JOIN users u ON u.id = ee.user_id WHERE ee.election_id = ? AND u.role = 'user'";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void setEligibility(int electionId, int userId, boolean eligible) throws SQLException {
        String sql = eligible
                ? "INSERT IGNORE INTO election_eligibility(election_id, user_id) VALUES(?, ?)"
                : "DELETE FROM election_eligibility WHERE election_id = ? AND user_id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public int bulkAllowByVoterIdsOrEmails(int electionId, List<String> identifiers) throws SQLException {
        String sql = "INSERT IGNORE INTO election_eligibility(election_id, user_id) " +
                "SELECT ?, id FROM users WHERE role = 'user' AND (voter_id_number = ? OR email = ?)";
        int added = 0;
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String identifier : identifiers) {
                if (identifier == null || identifier.trim().isEmpty()) {
                    continue;
                }
                String clean = identifier.trim();
                ps.setInt(1, electionId);
                ps.setString(2, clean);
                ps.setString(3, clean);
                added += ps.executeUpdate();
            }
        }
        return added;
    }

    public List<Map<String, Object>> getCenterEligibilitySummary(int electionId) throws SQLException {
        String sql;
        boolean restricted = hasEligibilityList(electionId);
        if (restricted) {
            sql = "SELECT COALESCE(NULLIF(u.election_center, ''), 'Unassigned') AS center_name, COUNT(*) AS eligible_count " +
                    "FROM election_eligibility ee JOIN users u ON u.id = ee.user_id " +
                    "WHERE ee.election_id = ? AND u.role = 'user' GROUP BY center_name ORDER BY center_name";
        } else {
            sql = "SELECT COALESCE(NULLIF(election_center, ''), 'Unassigned') AS center_name, COUNT(*) AS eligible_count " +
                    "FROM users WHERE role = 'user' GROUP BY center_name ORDER BY center_name";
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (restricted) {
                ps.setInt(1, electionId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("centerName", rs.getString("center_name"));
                    row.put("eligibleCount", rs.getInt("eligible_count"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
