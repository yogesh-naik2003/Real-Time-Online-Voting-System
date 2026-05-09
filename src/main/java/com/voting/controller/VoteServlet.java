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

@WebServlet("/user/vote")
public class VoteServlet extends HttpServlet {
    private final VotingService votingService = new VotingService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "user")) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = SessionUtil.getLoggedInUser(req);
        String candidateId = req.getParameter("candidateId");
        String otp = req.getParameter("otp");
        String ipAddress = SessionUtil.getClientIp(req);
        String userAgent = req.getHeader("User-Agent");

        try {
            if (candidateId == null || otp == null) {
                throw new IllegalStateException("Missing candidate selection or security code.");
            }
            String receiptId = votingService.castVote(user, Integer.parseInt(candidateId), otp, ipAddress, userAgent);
            req.getSession().setAttribute("voteReceipt", receiptId);
            resp.sendRedirect(req.getContextPath() + "/user/dashboard?voted=true");
        } catch (IllegalStateException | NumberFormatException ex) {
            req.getSession().setAttribute("voteError", ex.getMessage());
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
        } catch (SQLException ex) {
            throw new ServletException("Unable to cast vote", ex);
        }
    }
}
