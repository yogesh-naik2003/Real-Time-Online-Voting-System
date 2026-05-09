package com.voting.dao;

import com.voting.model.Election;
import com.voting.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ElectionDao {
    public void createElection(Election election) throws SQLException {
        if (checkOverlap(election.getElectionDate(), election.getStartTime(), election.getEndTime(), -1)) {
            throw new SQLException("Time overlap with an existing election on the same day.");
        }
        String sql = "INSERT INTO elections(title, description, status, election_date, start_time, end_time, results_announced, nomination_starts_at, nomination_ends_at, ready_for_results) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, election.getTitle());
            ps.setString(2, election.getDescription());
            ps.setString(3, election.getStatus());
            ps.setDate(4, election.getElectionDate());
            ps.setTime(5, election.getStartTime());
            ps.setTime(6, election.getEndTime());
            ps.setBoolean(7, election.isResultsAnnounced());
            ps.setTimestamp(8, election.getNominationStartsAt());
            ps.setTimestamp(9, election.getNominationEndsAt());
            ps.setBoolean(10, election.isReadyForResults());
            ps.executeUpdate();
        }
    }

    public void updateElection(Election election) throws SQLException {
        if (checkOverlap(election.getElectionDate(), election.getStartTime(), election.getEndTime(), election.getId())) {
            throw new SQLException("Time overlap with an existing election on the same day.");
        }
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement deactivate = connection.prepareStatement("UPDATE elections SET status = 'INACTIVE' WHERE id <> ?");
             PreparedStatement ps = connection.prepareStatement("UPDATE elections SET title = ?, description = ?, status = ?, election_date = ?, start_time = ?, end_time = ?, results_announced = ?, nomination_starts_at = ?, nomination_ends_at = ?, ready_for_results = ? WHERE id = ?")) {
            connection.setAutoCommit(false);
            if ("ACTIVE".equalsIgnoreCase(election.getStatus())) {
                deactivate.setInt(1, election.getId());
                deactivate.executeUpdate();
            }
            ps.setString(1, election.getTitle());
            ps.setString(2, election.getDescription());
            ps.setString(3, election.getStatus());
            ps.setDate(4, election.getElectionDate());
            ps.setTime(5, election.getStartTime());
            ps.setTime(6, election.getEndTime());
            ps.setBoolean(7, election.isResultsAnnounced());
            ps.setTimestamp(8, election.getNominationStartsAt());
            ps.setTimestamp(9, election.getNominationEndsAt());
            ps.setBoolean(10, election.isReadyForResults());
            ps.setInt(11, election.getId());
            ps.executeUpdate();
            connection.commit();
        }
    }

    public Election findActiveElection() throws SQLException {
        autoUpdateStatuses();
        String sql = "SELECT * FROM elections WHERE status = 'ACTIVE' ORDER BY id DESC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Election election = mapRow(rs);
                if (isWithinVotingWindow(election)) {
                    return election;
                }
            }
        }
        return null;
    }

    public Election findLatestElection() throws SQLException {
        String sql = "SELECT * FROM elections ORDER BY id DESC LIMIT 1";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    public Election findById(int electionId) throws SQLException {
        String sql = "SELECT * FROM elections WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Election> findAll() throws SQLException {
        List<Election> elections = new ArrayList<>();
        String sql = "SELECT * FROM elections ORDER BY id DESC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                elections.add(mapRow(rs));
            }
        }
        return elections;
    }

    public void updateStatus(int electionId, String status) throws SQLException {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement deactivate = connection.prepareStatement("UPDATE elections SET status = 'INACTIVE' WHERE id <> ?");
             PreparedStatement activate = connection.prepareStatement("UPDATE elections SET status = ?, results_announced = CASE WHEN ? = 'COMPLETED' THEN TRUE ELSE results_announced END WHERE id = ?")) {
            connection.setAutoCommit(false);
            if ("ACTIVE".equalsIgnoreCase(status)) {
                deactivate.setInt(1, electionId);
                deactivate.executeUpdate();
            }
            activate.setString(1, status);
            activate.setString(2, status);
            activate.setInt(3, electionId);
            activate.executeUpdate();
            connection.commit();
        } catch (SQLException ex) {
            throw ex;
        }
    }

    public void announceResults(int electionId, boolean announced) throws SQLException {
        String sql = "UPDATE elections SET results_announced = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, announced);
            ps.setInt(2, electionId);
            ps.executeUpdate();
        }
    }

    public void updateNominationWindow(int electionId, java.sql.Timestamp startsAt, java.sql.Timestamp endsAt) throws SQLException {
        String sql = "UPDATE elections SET nomination_starts_at = ?, nomination_ends_at = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, startsAt);
            ps.setTimestamp(2, endsAt);
            ps.setInt(3, electionId);
            ps.executeUpdate();
        }
    }

    public void extendEndTime(int electionId, java.sql.Time endTime) throws SQLException {
        String sql = "UPDATE elections SET end_time = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTime(1, endTime);
            ps.setInt(2, electionId);
            ps.executeUpdate();
        }
    }

    public void markReadyForResults(int electionId, boolean ready) throws SQLException {
        String sql = "UPDATE elections SET ready_for_results = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, ready);
            ps.setInt(2, electionId);
            ps.executeUpdate();
        }
    }

    public boolean checkOverlap(java.sql.Date date, java.sql.Time startTime, java.sql.Time endTime, int excludeId) throws SQLException {
        if (date == null || startTime == null || endTime == null) return false;
        String sql = "SELECT COUNT(*) FROM elections WHERE election_date = ? AND id != ? AND (start_time < ? AND end_time > ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, date);
            ps.setInt(2, excludeId);
            ps.setTime(3, endTime);
            ps.setTime(4, startTime);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void autoUpdateStatuses() throws SQLException {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement selectStmt = connection.prepareStatement("SELECT * FROM elections WHERE status = 'ACTIVE'");
             PreparedStatement updateStmt = connection.prepareStatement("UPDATE elections SET status = ? WHERE id = ?")) {
            connection.setAutoCommit(false);
            try (ResultSet rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    Election election = mapRow(rs);
                    if (!isWithinVotingWindow(election)) {
                        updateStmt.setString(1, "INACTIVE");
                        updateStmt.setInt(2, election.getId());
                        updateStmt.addBatch();
                    }
                }
            }
            updateStmt.executeBatch();
            connection.commit();
        }
    }

    private boolean isWithinVotingWindow(Election election) {
        if (election.getElectionDate() == null || election.getStartTime() == null || election.getEndTime() == null) {
            return false;
        }
        LocalDate electionDate = election.getElectionDate().toLocalDate();
        LocalTime startTime = election.getStartTime().toLocalTime();
        LocalTime endTime = election.getEndTime().toLocalTime();
        LocalDateTime startsAt = LocalDateTime.of(electionDate, startTime);
        LocalDateTime endsAt = LocalDateTime.of(electionDate, endTime);
        if (!endsAt.isAfter(startsAt)) {
            endsAt = endsAt.plusDays(1);
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(startsAt) && !now.isAfter(endsAt);
    }

    private Election mapRow(ResultSet rs) throws SQLException {
        Election election = new Election();
        election.setId(rs.getInt("id"));
        election.setTitle(rs.getString("title"));
        election.setDescription(rs.getString("description"));
        election.setStatus(rs.getString("status"));
        election.setElectionDate(rs.getDate("election_date"));
        election.setStartTime(rs.getTime("start_time"));
        election.setEndTime(rs.getTime("end_time"));
        election.setResultsAnnounced(rs.getBoolean("results_announced"));
        try { election.setNominationStartsAt(rs.getTimestamp("nomination_starts_at")); } catch (SQLException ignored) {}
        try { election.setNominationEndsAt(rs.getTimestamp("nomination_ends_at")); } catch (SQLException ignored) {}
        try { election.setReadyForResults(rs.getBoolean("ready_for_results")); } catch (SQLException ignored) {}
        return election;
    }
}
