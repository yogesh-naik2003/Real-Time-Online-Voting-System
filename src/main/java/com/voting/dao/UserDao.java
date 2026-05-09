package com.voting.dao;

import com.voting.model.User;
import com.voting.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    public void createUser(User user) throws SQLException {
        String sql = "INSERT INTO users(name, email, mobile_number, voter_id_number, date_of_birth, age, election_center, city, state, profile_photo_path, password, role, has_voted) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMobileNumber());
            ps.setString(4, user.getVoterIdNumber());
            ps.setDate(5, user.getDateOfBirth());
            if (user.getAge() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, user.getAge());
            }
            ps.setString(7, user.getElectionCenter());
            ps.setString(8, user.getCity());
            ps.setString(9, user.getState());
            ps.setString(10, user.getProfilePhotoPath());
            ps.setString(11, user.getPassword());
            ps.setString(12, user.getRole());
            ps.setBoolean(13, user.isHasVoted());
            ps.executeUpdate();
        }
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public User findByVoterIdNumber(String voterIdNumber) throws SQLException {
        String sql = "SELECT * FROM users WHERE voter_id_number = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, voterIdNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id DESC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    public List<User> findAllVoters() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'user' ORDER BY name ASC";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    public void updateVotingStatus(int userId, boolean hasVoted) throws SQLException {
        String sql = "UPDATE users SET has_voted = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, hasVoted);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void resetAllVotingStatus() throws SQLException {
        String sql = "UPDATE users SET has_voted = FALSE WHERE role = 'user'";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateProfile(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ?, mobile_number = ?, voter_id_number = ?, date_of_birth = ?, age = ?, election_center = ?, city = ?, state = ?, profile_photo_path = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getMobileNumber());
            ps.setString(4, user.getVoterIdNumber());
            ps.setDate(5, user.getDateOfBirth());
            if (user.getAge() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, user.getAge());
            }
            ps.setString(7, user.getElectionCenter());
            ps.setString(8, user.getCity());
            ps.setString(9, user.getState());
            ps.setString(10, user.getProfilePhotoPath());
            ps.setInt(11, user.getId());
            ps.executeUpdate();
        }
    }

    public int countUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'user'";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countVoters() throws SQLException {
        return countUsers();
    }

    public void updateRole(int userId, String role) throws SQLException {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void incrementFailedAttempts(int userId) throws SQLException {
        // First, get the current count
        String selectSql = "SELECT failed_attempts FROM users WHERE id = ?";
        int currentAttempts = 0;
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentAttempts = rs.getInt(1);
                }
            }
        }

        currentAttempts++;
        java.sql.Timestamp lockedUntil = null;
        if (currentAttempts >= 5) {
            lockedUntil = new java.sql.Timestamp(System.currentTimeMillis() + 15 * 60 * 1000);
        }

        String updateSql = "UPDATE users SET failed_attempts = ?, locked_until = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setInt(1, currentAttempts);
            ps.setTimestamp(2, lockedUntil);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void unlockAllAccounts() throws SQLException {
        String sql = "UPDATE users SET failed_attempts = 0, locked_until = NULL";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public void resetFailedAttempts(int userId) throws SQLException {
        String sql = "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void setOtp(int userId, String otp, java.sql.Timestamp expiry) throws SQLException {
        String sql = "UPDATE users SET otp_code = ?, otp_expiry = ? WHERE id = ?";
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, otp);
            ps.setTimestamp(2, expiry);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setMobileNumber(rs.getString("mobile_number"));
        user.setVoterIdNumber(rs.getString("voter_id_number"));
        user.setDateOfBirth(rs.getDate("date_of_birth"));
        int age = rs.getInt("age");
        user.setAge(rs.wasNull() ? null : age);
        user.setElectionCenter(rs.getString("election_center"));
        user.setCity(rs.getString("city"));
        user.setState(rs.getString("state"));
        user.setProfilePhotoPath(rs.getString("profile_photo_path"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setHasVoted(rs.getBoolean("has_voted"));
        
        try { user.setOtpCode(rs.getString("otp_code")); } catch (SQLException e) {}
        try { user.setOtpExpiry(rs.getTimestamp("otp_expiry")); } catch (SQLException e) {}
        try { user.setFailedAttempts(rs.getInt("failed_attempts")); } catch (SQLException e) {}
        try { user.setLockedUntil(rs.getTimestamp("locked_until")); } catch (SQLException e) {}
        
        return user;
    }
}
