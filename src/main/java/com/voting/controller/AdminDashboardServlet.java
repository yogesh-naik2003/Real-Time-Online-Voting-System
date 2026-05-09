package com.voting.controller;

import com.voting.dao.CandidateDao;
import com.voting.dao.CandidateNominationDao;
import com.voting.dao.ElectionDao;
import com.voting.dao.ElectionEligibilityDao;
import com.voting.dao.MongoLogDao;
import com.voting.dao.OfficerOpsDao;
import com.voting.dao.UserDao;
import com.voting.dao.VoteDao;
import com.voting.service.VotingService;
import com.voting.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {
    private final VotingService votingService = new VotingService();
    private final UserDao userDao = new UserDao();
    private final CandidateDao candidateDao = new CandidateDao();
    private final ElectionDao electionDao = new ElectionDao();
    private final CandidateNominationDao nominationDao = new CandidateNominationDao();
    private final ElectionEligibilityDao eligibilityDao = new ElectionEligibilityDao();
    private final VoteDao voteDao = new VoteDao();
    private final OfficerOpsDao officerOpsDao = new OfficerOpsDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "admin")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required.");
            return;
        }

        String action = req.getParameter("action");
        if ("unlockAll".equals(action)) {
            if (!SessionUtil.canManageSettings(req)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Only Super Admin can unlock all accounts.");
                return;
            }
            try {
                userDao.unlockAllAccounts();
                req.getSession().setAttribute("successMessage", "All user accounts have been successfully unlocked.");
            } catch (SQLException e) {
                req.getSession().setAttribute("errorMessage", "Failed to unlock accounts: " + e.getMessage());
            }
        }
        resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "admin")) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            req.setAttribute("stats", votingService.getDashboardStats());
            req.setAttribute("users", userDao.findAll());
            req.setAttribute("voters", userDao.findAllVoters());
            req.setAttribute("candidates", candidateDao.findAll());
            req.setAttribute("nominations", nominationDao.findAll());
            
            // New Advanced Analytics Data
            req.setAttribute("demographics", votingService.getDemographicStats());
            req.setAttribute("voteTrends", votingService.getVoteActivityTrend());
            req.setAttribute("announcedResults", votingService.getAnnouncedElectionResults());
            
            MongoLogDao mongoDao = new MongoLogDao();
            req.setAttribute("pendingUsers", mongoDao.getPendingUsers());
            req.setAttribute("auditLogs", mongoDao.getRecentLogs());
            req.setAttribute("suspiciousActivity", mongoDao.getSuspiciousActivitySummary());
            req.setAttribute("auditorNotes", mongoDao.getRecentAuditorNotes());
            req.setAttribute("integritySummary", voteDao.getIntegritySummary());
            req.setAttribute("openComplaints", mongoDao.getOpenComplaints());
            req.setAttribute("allComplaints", mongoDao.getAllComplaints());
            
            req.setAttribute("elections", electionDao.findAll());
            req.setAttribute("latestElection", electionDao.findLatestElection());
            java.util.List<com.voting.model.Election> allElections = electionDao.findAll();
            req.setAttribute("officerReadiness", officerOpsDao.getReadinessChecklist(allElections));
            com.voting.model.Election officerElection = electionDao.findActiveElection();
            if (officerElection == null) {
                officerElection = electionDao.findLatestElection();
            }
            req.setAttribute("officerSelectedElection", officerElection);
            req.setAttribute("centerTurnout", officerElection == null ? new java.util.ArrayList<java.util.Map<String, Object>>() : officerOpsDao.getCenterTurnout(officerElection.getId()));
            req.setAttribute("officerNotes", officerOpsDao.getOfficerNotes());
            req.setAttribute("officerIncidents", officerOpsDao.getIncidents());
            req.setAttribute("officerDryRuns", officerOpsDao.getDryRuns());
            if (electionDao.findLatestElection() != null) {
                req.setAttribute("latestElectionHasEligibilityList", eligibilityDao.hasEligibilityList(electionDao.findLatestElection().getId()));
            }
            req.getRequestDispatcher("/WEB-INF/views/admin-dashboard.jsp").forward(req, resp);
        } catch (SQLException ex) {
            throw new ServletException("Unable to load admin dashboard", ex);
        }
    }
}
