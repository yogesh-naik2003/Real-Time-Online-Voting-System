package com.voting.controller;

import com.voting.dao.ElectionDao;
import com.voting.model.Election;
import com.voting.service.VotingService;
import com.voting.util.InputValidator;
import com.voting.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/elections")
public class ElectionServlet extends HttpServlet {
    private final ElectionDao electionDao = new ElectionDao();
    private final VotingService votingService = new VotingService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.canManageElections(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Election management requires Super Admin, Admin, or Election Officer access.");
            return;
        }

        String action = req.getParameter("action");
        String title = req.getParameter("title");
        String description = req.getParameter("description");
        
        String electionDateStr = req.getParameter("electionDate");
        String startTimeStr = req.getParameter("startTime");
        String endTimeStr = req.getParameter("endTime");
        String nominationStartsAtStr = req.getParameter("nominationStartsAt");
        String nominationEndsAtStr = req.getParameter("nominationEndsAt");

        try {
            if ("create".equalsIgnoreCase(action) || "update".equalsIgnoreCase(action)) {
                title = InputValidator.required(title, "Election title", 150);
                description = InputValidator.required(description, "Election description", 3000);
            }
            if ("create".equalsIgnoreCase(action)) {
                Election election = new Election();
                election.setTitle(title);
                election.setDescription(description);
                if (electionDateStr != null && !electionDateStr.isEmpty()) election.setElectionDate(java.sql.Date.valueOf(electionDateStr));
                if (startTimeStr != null && !startTimeStr.isEmpty()) election.setStartTime(java.sql.Time.valueOf(startTimeStr + (startTimeStr.length() == 5 ? ":00" : "")));
                if (endTimeStr != null && !endTimeStr.isEmpty()) election.setEndTime(java.sql.Time.valueOf(endTimeStr + (endTimeStr.length() == 5 ? ":00" : "")));
                election.setNominationStartsAt(parseLocalTimestamp(nominationStartsAtStr));
                election.setNominationEndsAt(parseLocalTimestamp(nominationEndsAtStr));
                election.setStatus("INACTIVE");
                
                try {
                    electionDao.createElection(election);
                    Election createdElection = electionDao.findLatestElection();
                    if (createdElection != null) {
                        votingService.broadcastElectionUpdate("create", createdElection.getId(), createdElection.getTitle());
                        new com.voting.dao.MongoLogDao().logSystemEvent("Election Created", "Created election: " + createdElection.getTitle(), SessionUtil.getClientIp(req));
                        req.getSession().setAttribute("successMessage", "Election '" + createdElection.getTitle() + "' deployed successfully.");
                    }
                } catch (SQLException ex) {
                    req.getSession().setAttribute("errorMessage", ex.getMessage());
                }
            } else if ("update".equalsIgnoreCase(action)) {
                Election election = electionDao.findById(Integer.parseInt(req.getParameter("electionId")));
                if (election != null) {
                    election.setTitle(title);
                    election.setDescription(description);
                    election.setStatus(req.getParameter("status"));
                    if ("COMPLETED".equalsIgnoreCase(election.getStatus())) {
                        election.setResultsAnnounced(true);
                    }
                    if (electionDateStr != null && !electionDateStr.isEmpty()) election.setElectionDate(java.sql.Date.valueOf(electionDateStr));
                    if (startTimeStr != null && !startTimeStr.isEmpty()) election.setStartTime(java.sql.Time.valueOf(startTimeStr + (startTimeStr.length() == 5 ? ":00" : "")));
                    if (endTimeStr != null && !endTimeStr.isEmpty()) election.setEndTime(java.sql.Time.valueOf(endTimeStr + (endTimeStr.length() == 5 ? ":00" : "")));
                    election.setNominationStartsAt(parseLocalTimestamp(nominationStartsAtStr));
                    election.setNominationEndsAt(parseLocalTimestamp(nominationEndsAtStr));
                    
                    try {
                        electionDao.updateElection(election);
                        votingService.broadcastElectionUpdate("update", election.getId(), election.getTitle());
                        new com.voting.dao.MongoLogDao().logSystemEvent("Election Updated", "Updated election: " + election.getTitle(), SessionUtil.getClientIp(req));
                        req.getSession().setAttribute("successMessage", "Election details updated successfully.");
                    } catch (SQLException ex) {
                        req.getSession().setAttribute("errorMessage", ex.getMessage());
                    }
                }
            } else if ("toggle".equalsIgnoreCase(action)) {
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                String status = req.getParameter("status");
                electionDao.updateStatus(electionId, status);
                new com.voting.dao.MongoLogDao().logSystemEvent("Election Status Toggled", "Status set to " + status + " for election ID: " + electionId, SessionUtil.getClientIp(req));
                if ("ACTIVE".equalsIgnoreCase(status)) {
                    Election active = electionDao.findById(electionId);
                    String titleText = active != null ? active.getTitle() : ("Election " + electionId);
                    new com.voting.dao.NotificationDao().notifyAllVoters("Election Started", titleText + " is now open for voting.", "election");
                } else if ("COMPLETED".equalsIgnoreCase(status)) {
                    new com.voting.dao.NotificationDao().notifyAllVoters("Election Completed", "Election ID " + electionId + " has been marked complete. Results will appear after announcement.", "election");
                }
                votingService.broadcastElectionUpdate(status.toLowerCase(), electionId, status);
            } else if ("extend".equalsIgnoreCase(action)) {
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                java.sql.Time newEndTime = java.sql.Time.valueOf(InputValidator.required(req.getParameter("endTime"), "New end time", 8) + (req.getParameter("endTime").length() == 5 ? ":00" : ""));
                String reason = InputValidator.required(req.getParameter("reason"), "Extension reason", 500);
                electionDao.extendEndTime(electionId, newEndTime);
                new com.voting.dao.MongoLogDao().logSystemEvent("Election Time Extended", "Election " + electionId + " extended to " + newEndTime + ". Reason: " + reason, SessionUtil.getClientIp(req));
                votingService.broadcastElectionUpdate("extend", electionId, String.valueOf(newEndTime));
            } else if ("nominationWindow".equalsIgnoreCase(action)) {
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                electionDao.updateNominationWindow(electionId, parseLocalTimestamp(nominationStartsAtStr), parseLocalTimestamp(nominationEndsAtStr));
                new com.voting.dao.MongoLogDao().logSystemEvent("Nomination Window Updated", "Election " + electionId + " nomination window updated", SessionUtil.getClientIp(req));
            }
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        } catch (Exception ex) {
            throw new ServletException("Unable to manage election", ex);
        }
    }

    private java.sql.Timestamp parseLocalTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().replace("T", " ");
        if (normalized.length() == 16) {
            normalized += ":00";
        }
        return java.sql.Timestamp.valueOf(normalized);
    }
}
