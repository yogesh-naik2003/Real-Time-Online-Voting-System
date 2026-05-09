package com.voting.dao;

import com.voting.model.Election;
import com.voting.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OfficerOpsDao {
    private final CandidateDao candidateDao = new CandidateDao();
    private final ElectionEligibilityDao eligibilityDao = new ElectionEligibilityDao();
    private final VoteDao voteDao = new VoteDao();

    public List<Map<String, Object>> getReadinessChecklist(List<Election> elections) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (elections == null) {
            return rows;
        }
        for (Election election : elections) {
            int candidateCount = candidateDao.countCandidatesByElection(election.getId());
            int eligibleCount = eligibilityDao.countEligibleVoters(election.getId());
            int pendingNominations = countPendingNominations(election.getId());
            boolean timeWindowValid = election.getElectionDate() != null
                    && election.getStartTime() != null
                    && election.getEndTime() != null
                    && !election.getEndTime().equals(election.getStartTime());
            boolean nominationWindowValid = election.getNominationStartsAt() == null
                    || election.getNominationEndsAt() == null
                    || election.getNominationEndsAt().after(election.getNominationStartsAt());
            boolean ready = candidateCount > 0 && eligibleCount > 0 && timeWindowValid && nominationWindowValid && pendingNominations == 0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("electionId", election.getId());
            row.put("title", election.getTitle());
            row.put("candidateCount", candidateCount);
            row.put("eligibleCount", eligibleCount);
            row.put("pendingNominations", pendingNominations);
            row.put("timeWindowValid", timeWindowValid);
            row.put("nominationWindowValid", nominationWindowValid);
            row.put("ready", ready);
            rows.add(row);
        }
        return rows;
    }

    public List<Map<String, Object>> getCenterTurnout(int electionId) throws SQLException {
        Map<String, Map<String, Object>> byCenter = new LinkedHashMap<>();
        for (Map<String, Object> row : eligibilityDao.getCenterEligibilitySummary(electionId)) {
            String centerName = String.valueOf(row.get("centerName"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("centerName", centerName);
            item.put("eligibleCount", row.get("eligibleCount"));
            item.put("votesCast", 0);
            item.put("turnoutPercentage", 0);
            byCenter.put(centerName, item);
        }
        for (Map<String, Object> row : voteDao.getCenterTurnoutByElection(electionId)) {
            String centerName = String.valueOf(row.get("centerName"));
            Map<String, Object> item = byCenter.get(centerName);
            if (item == null) {
                item = new LinkedHashMap<>();
                item.put("centerName", centerName);
                item.put("eligibleCount", 0);
                byCenter.put(centerName, item);
            }
            int eligible = Integer.parseInt(String.valueOf(item.get("eligibleCount")));
            int votes = Integer.parseInt(String.valueOf(row.get("votesCast")));
            item.put("votesCast", votes);
            item.put("turnoutPercentage", eligible == 0 ? 0 : Math.round((votes * 10000.0) / eligible) / 100.0);
        }
        return new ArrayList<>(byCenter.values());
    }

    public void addOfficerNote(int electionId, int officerId, String note) throws SQLException {
        String sql = "INSERT INTO officer_notes(election_id, officer_id, note) VALUES(?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, officerId);
            ps.setString(3, note);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getOfficerNotes() throws SQLException {
        String sql = "SELECT n.*, e.title AS election_title, u.name AS officer_name " +
                "FROM officer_notes n JOIN elections e ON e.id = n.election_id JOIN users u ON u.id = n.officer_id " +
                "ORDER BY n.created_at DESC LIMIT 30";
        return queryRows(sql);
    }

    public void addIncident(int electionId, int officerId, String severity, String category, String description) throws SQLException {
        String sql = "INSERT INTO election_incidents(election_id, officer_id, severity, category, description) VALUES(?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, officerId);
            ps.setString(3, severity);
            ps.setString(4, category);
            ps.setString(5, description);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getIncidents() throws SQLException {
        String sql = "SELECT i.*, e.title AS election_title, u.name AS officer_name " +
                "FROM election_incidents i JOIN elections e ON e.id = i.election_id JOIN users u ON u.id = i.officer_id " +
                "ORDER BY i.created_at DESC LIMIT 30";
        return queryRows(sql);
    }

    public void addDryRun(int electionId, int officerId, String result, String details) throws SQLException {
        String sql = "INSERT INTO election_dry_runs(election_id, officer_id, result, details) VALUES(?, ?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            ps.setInt(2, officerId);
            ps.setString(3, result);
            ps.setString(4, details);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getDryRuns() throws SQLException {
        String sql = "SELECT d.*, e.title AS election_title, u.name AS officer_name " +
                "FROM election_dry_runs d JOIN elections e ON e.id = d.election_id JOIN users u ON u.id = d.officer_id " +
                "ORDER BY d.created_at DESC LIMIT 20";
        return queryRows(sql);
    }

    public boolean isNominationOpen(Election election) {
        if (election == null || "COMPLETED".equalsIgnoreCase(election.getStatus())) {
            return false;
        }
        if (election.getNominationStartsAt() == null || election.getNominationEndsAt() == null) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(election.getNominationStartsAt().toLocalDateTime())
                && !now.isAfter(election.getNominationEndsAt().toLocalDateTime());
    }

    private int countPendingNominations(int electionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidate_nominations WHERE election_id = ? AND status = 'PENDING'";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<Map<String, Object>> queryRows(String sql) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int columns = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columns; i++) {
                    row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
