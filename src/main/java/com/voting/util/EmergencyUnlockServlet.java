package com.voting.util;

import com.voting.dao.UserDao;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/emergency-unlock")
public class EmergencyUnlockServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String key = req.getParameter("key");
        // Simple security: only allow from localhost or with a specific key
        boolean isLocal = "127.0.0.1".equals(req.getRemoteAddr()) || "0:0:0:0:0:0:0:1".equals(req.getRemoteAddr());
        
        if (isLocal || "admin_restore_2024".equals(key)) {
            try {
                userDao.unlockAllAccounts();
                resp.getWriter().println("All accounts have been unlocked successfully.");
            } catch (SQLException e) {
                resp.setStatus(500);
                resp.getWriter().println("Error unlocking accounts: " + e.getMessage());
            }
        } else {
            resp.setStatus(403);
            resp.getWriter().println("Access denied. This endpoint is only available from localhost.");
        }
    }
}
