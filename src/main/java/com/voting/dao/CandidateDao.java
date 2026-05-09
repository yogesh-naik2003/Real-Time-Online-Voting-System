package com.voting.dao;

import com.voting.model.Candidate;
import com.voting.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CandidateDao {
    public void createCandidate(Candidate candidate) throws SQLException {
        String sql = "INSERT INTO candidates(name, manifesto, election_id, photo_path, ballot_order) VALUES(?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, candidate.getName());
            ps.setString(2, candidate.getManifesto());
            ps.setInt(3, candidate.getElectionId());
            ps.setString(4, candidate.getPhotoPath());
            ps.setInt(5, candidate.getBallotOrder());
            ps.executeUpdate();
        }
    }

    public void updateCandidate(Candidate candidate) throws SQLException {
        String sql = "UPDATE candidates SET name = ?, manifesto = ?, election_id = ?, photo_path = COALESCE(NULLIF(?, ''), photo_path), ballot_order = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, candidate.getName());
            ps.setString(2, candidate.getManifesto());
            ps.setInt(3, candidate.getElectionId());
            ps.setString(4, candidate.getPhotoPath());
            ps.setInt(5, candidate.getBallotOrder());
            ps.setInt(6, candidate.getId());
            ps.executeUpdate();
        }
    }

    public void deleteCandidate(int candidateId) throws SQLException {
        String sql = "DELETE FROM candidates WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            ps.executeUpdate();
        }
    }

    public Candidate findById(int id) throws SQLException {
        String sql = "SELECT * FROM candidates WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Candidate> findByElectionId(int electionId) throws SQLException {
        List<Candidate> candidates = new ArrayList<>();
        String sql = "SELECT * FROM candidates WHERE election_id = ? ORDER BY ballot_order ASC, id ASC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    candidates.add(mapRow(rs));
                }
            }
        }
        return candidates;
    }

    public List<Candidate> findAll() throws SQLException {
        List<Candidate> candidates = new ArrayList<>();
        String sql = "SELECT * FROM candidates ORDER BY election_id DESC, ballot_order ASC, id DESC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                candidates.add(mapRow(rs));
            }
        }
        return candidates;
    }

    public int countCandidates() throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidates";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countCandidatesByElection(int electionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidates WHERE election_id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, electionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void updateBallotOrder(int candidateId, int ballotOrder) throws SQLException {
        String sql = "UPDATE candidates SET ballot_order = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, ballotOrder);
            ps.setInt(2, candidateId);
            ps.executeUpdate();
        }
    }

    private Candidate mapRow(ResultSet rs) throws SQLException {
        Candidate candidate = new Candidate();
        candidate.setId(rs.getInt("id"));
        candidate.setName(rs.getString("name"));
        candidate.setManifesto(rs.getString("manifesto"));
        candidate.setElectionId(rs.getInt("election_id"));
        candidate.setPhotoPath(rs.getString("photo_path"));
        try { candidate.setBallotOrder(rs.getInt("ballot_order")); } catch (SQLException ignored) {}
        return candidate;
    }
}
