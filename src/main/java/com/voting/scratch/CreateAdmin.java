package com.voting.scratch;

import com.voting.util.DatabaseUtil;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class CreateAdmin {
    public static void main(String[] args) {
        String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "SEC Admin");
            ps.setString(2, "admin@sec.gov");
            ps.setString(3, BCrypt.hashpw("admin123", BCrypt.gensalt()));
            ps.setString(4, "admin");
            ps.executeUpdate();
            System.out.println("Admin created: admin@sec.gov / admin123");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
