package com.voting.controller;

import com.voting.model.User;
import com.voting.service.VotingService;
import com.voting.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/user/dashboard")
public class UserDashboardServlet extends HttpServlet {
    private final VotingService votingService = new VotingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "user")) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = SessionUtil.getLoggedInUser(req);
        try {
            Map<String, Object> payload = votingService.getUserDashboardPayload(user);
            req.setAttribute("payload", payload);
            req.getRequestDispatcher("/WEB-INF/views/user-dashboard.jsp").forward(req, resp);
        } catch (SQLException ex) {
            throw new ServletException("Unable to load voter dashboard", ex);
        }
    }
}
