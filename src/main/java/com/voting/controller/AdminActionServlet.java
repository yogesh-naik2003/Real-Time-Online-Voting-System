package com.voting.controller;

import com.voting.dao.MongoLogDao;
import com.voting.dao.CandidateDao;
import com.voting.dao.CandidateNominationDao;
import com.voting.dao.ElectionEligibilityDao;
import com.voting.dao.ElectionDao;
import com.voting.dao.NotificationDao;
import com.voting.dao.OfficerOpsDao;
import com.voting.dao.UserDao;
import com.voting.dao.VoteDao;
import com.voting.model.Candidate;
import com.voting.model.User;
import com.voting.util.InputValidator;
import com.voting.util.SessionUtil;
import com.voting.util.SystemSettings;
import org.bson.Document;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/admin/actions")
public class AdminActionServlet extends HttpServlet {
    private final MongoLogDao mongoLogDao = new MongoLogDao();
    private final UserDao userDao = new UserDao();
    private final CandidateNominationDao nominationDao = new CandidateNominationDao();
    private final CandidateDao candidateDao = new CandidateDao();
    private final ElectionEligibilityDao eligibilityDao = new ElectionEligibilityDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final ElectionDao electionDao = new ElectionDao();
    private final OfficerOpsDao officerOpsDao = new OfficerOpsDao();
    private final VoteDao voteDao = new VoteDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "admin")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = req.getParameter("action");
        try {
            if ("approveUser".equals(action)) {
                if (!SessionUtil.canManageVoters(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Voter approvals require Super Admin or Admin access.");
                    return;
                }
                String objectId = req.getParameter("objectId");
                Document pendingUser = mongoLogDao.getPendingUser(objectId);
                if (pendingUser != null) {
                    User user = new User();
                    user.setName(String.valueOf(pendingUser.get("name")));
                    user.setEmail(String.valueOf(pendingUser.get("email")));
                    user.setMobileNumber(String.valueOf(pendingUser.get("mobile_number")));
                    user.setVoterIdNumber(String.valueOf(pendingUser.get("voter_id_number")));
                    String dob = pendingUser.get("date_of_birth") != null ? String.valueOf(pendingUser.get("date_of_birth")) : null;
                    if (dob != null && !dob.trim().isEmpty()) {
                        user.setDateOfBirth(java.sql.Date.valueOf(dob));
                    }
                    user.setAge(pendingUser.get("age") != null ? Integer.parseInt(String.valueOf(pendingUser.get("age"))) : null);
                    user.setElectionCenter(String.valueOf(pendingUser.get("election_center")));
                    user.setCity(String.valueOf(pendingUser.get("city")));
                    user.setState(String.valueOf(pendingUser.get("state")));
                    user.setProfilePhotoPath(String.valueOf(pendingUser.get("profile_photo_path")));
                    user.setPassword(String.valueOf(pendingUser.get("password")));
                    user.setRole("user");
                    user.setHasVoted(false);
                    userDao.createUser(user);
                    mongoLogDao.removePendingUser(objectId);
                    mongoLogDao.logSystemEvent("Voter Approved", "Approved registration for " + user.getEmail(), SessionUtil.getClientIp(req));
                }
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#approvals");
            } else if ("rejectUser".equals(action)) {
                if (!SessionUtil.canManageVoters(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Voter approvals require Super Admin or Admin access.");
                    return;
                }
                String objectId = req.getParameter("objectId");
                mongoLogDao.removePendingUser(objectId);
                mongoLogDao.logSystemEvent("Voter Rejected", "Rejected registration " + objectId, SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#approvals");
            } else if ("resolveComplaint".equals(action)) {
                if (!SessionUtil.canResolveComplaints(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Complaint resolution requires Super Admin or Admin access.");
                    return;
                }
                String objectId = req.getParameter("objectId");
                String adminReply = InputValidator.required(req.getParameter("adminReply"), "Admin reply", 1000);
                String returnTo = req.getParameter("returnTo");
                mongoLogDao.resolveComplaint(objectId, adminReply);
                mongoLogDao.logSystemEvent("Ticket Resolved", "Resolved support ticket " + objectId, SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#" + ("reports".equals(returnTo) ? "reports" : "support"));
            } else if ("updateSettings".equals(action)) {
                if (!SessionUtil.canManageSettings(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Only Super Admin can update system settings.");
                    return;
                }
                SystemSettings.allowRegistrations = "on".equals(req.getParameter("allowReg"));
                SystemSettings.requireIdVerification = "on".equals(req.getParameter("requireId"));
                SystemSettings.maintenanceMode = "on".equals(req.getParameter("maintenance"));
                mongoLogDao.logSystemEvent("Settings Updated", "Admin updated global system settings.", SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#settings");
            } else if ("announceResults".equals(action)) {
                if (!SessionUtil.canManageVoters(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Result announcements require Super Admin or Admin access.");
                    return;
                }
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                boolean announced = "true".equals(req.getParameter("announced"));
                new com.voting.dao.ElectionDao().announceResults(electionId, announced);
                if (announced) {
                    userDao.resetAllVotingStatus();
                    notificationDao.notifyAllVoters("Results Announced", "Official results are available for election ID " + electionId + ".", "results");
                }
                mongoLogDao.logSystemEvent("Results Announced", "Admin " + (announced ? "announced" : "withdrew") + " results for election " + electionId, SessionUtil.getClientIp(req));
                com.voting.websocket.VoteUpdateBroadcaster.broadcast("election_updated");
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#elections");
            } else if ("setEligibility".equals(action)) {
                if (!SessionUtil.canManageEligibility(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Eligibility management requires Super Admin, Admin, or Election Officer access.");
                    return;
                }
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                int userId = Integer.parseInt(req.getParameter("userId"));
                boolean eligible = "true".equals(req.getParameter("eligible"));
                eligibilityDao.setEligibility(electionId, userId, eligible);
                notificationDao.create(userId, "Election Eligibility", eligible ? "You are eligible for election ID " + electionId + "." : "Your eligibility was removed for election ID " + electionId + ".", "eligibility");
                mongoLogDao.logSystemEvent("Eligibility Updated", "Election " + electionId + ", user " + userId + ", eligible=" + eligible, SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#eligibility");
            } else if ("reviewNomination".equals(action)) {
                if (!SessionUtil.canReviewNominations(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Nomination review requires Super Admin, Admin, or Election Officer access.");
                    return;
                }
                int nominationId = Integer.parseInt(req.getParameter("nominationId"));
                String decision = req.getParameter("decision");
                String note = req.getParameter("adminNote");
                java.util.Map<String, Object> nomination = nominationDao.findById(nominationId);
                if (nomination == null) {
                    throw new IllegalArgumentException("Nomination not found.");
                }
                if ("APPROVED".equals(decision)) {
                    Candidate candidate = new Candidate();
                    candidate.setName(String.valueOf(nomination.get("userName")));
                    candidate.setManifesto(String.valueOf(nomination.get("manifesto")));
                    candidate.setElectionId(Integer.parseInt(String.valueOf(nomination.get("electionId"))));
                    candidate.setPhotoPath(nomination.get("profilePhotoPath") == null ? null : String.valueOf(nomination.get("profilePhotoPath")));
                    candidateDao.createCandidate(candidate);
                }
                nominationDao.updateStatus(nominationId, "APPROVED".equals(decision) ? "APPROVED" : "REJECTED", note);
                int userId = Integer.parseInt(String.valueOf(nomination.get("userId")));
                notificationDao.create(userId, "Candidate Nomination " + ("APPROVED".equals(decision) ? "Approved" : "Rejected"),
                        "Your nomination for " + nomination.get("electionTitle") + " was " + ("APPROVED".equals(decision) ? "approved." : "rejected."), "nomination");
                mongoLogDao.logSystemEvent("Nomination Reviewed", "Nomination " + nominationId + " set to " + decision, SessionUtil.getClientIp(req));
                com.voting.websocket.VoteUpdateBroadcaster.broadcast("election_updated");
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#nominations");
            } else if ("updateRole".equals(action)) {
                if (!SessionUtil.isSuperAdmin(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Only super admins can update admin roles.");
                    return;
                }
                int userId = Integer.parseInt(req.getParameter("userId"));
                String role = req.getParameter("role");
                userDao.updateRole(userId, role);
                mongoLogDao.logSystemEvent("Role Updated", "User " + userId + " role changed to " + role, SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#users");
            } else if ("addAuditorNote".equals(action)) {
                if (!SessionUtil.hasRole(req, "admin")) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Auditor notes require admin dashboard access.");
                    return;
                }
                User admin = SessionUtil.getLoggedInUser(req);
                String note = InputValidator.required(req.getParameter("auditorNote"), "Auditor note", 1000);
                mongoLogDao.addAuditorNote(admin.getId(), admin.getName(), note, SessionUtil.getClientIp(req));
                mongoLogDao.logSystemEvent("Auditor Note Added", "Review note added by " + admin.getEmail(), SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#auditor-review");
            } else if ("verifyNominationDocs".equals(action)) {
                if (!SessionUtil.canManageElections(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Document verification requires election officer access.");
                    return;
                }
                int nominationId = Integer.parseInt(req.getParameter("nominationId"));
                String status = req.getParameter("documentStatus");
                String note = InputValidator.required(req.getParameter("documentNote"), "Document note", 500);
                nominationDao.updateDocumentStatus(nominationId, status, note);
                mongoLogDao.logSystemEvent("Nomination Documents Reviewed", "Nomination " + nominationId + " document status " + status, SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#nominations");
            } else if ("bulkEligibility".equals(action)) {
                if (!SessionUtil.canManageEligibility(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Eligibility import requires election officer access.");
                    return;
                }
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                String csv = InputValidator.required(req.getParameter("voterIdentifiers"), "Voter IDs or emails", 5000);
                java.util.List<String> identifiers = new java.util.ArrayList<>();
                for (String token : csv.split("[,\\r\\n]+")) {
                    if (!token.trim().isEmpty()) identifiers.add(token.trim());
                }
                int added = eligibilityDao.bulkAllowByVoterIdsOrEmails(electionId, identifiers);
                req.getSession().setAttribute("successMessage", "Bulk eligibility import completed. Added " + added + " voter(s).");
                mongoLogDao.logSystemEvent("Bulk Eligibility Import", "Election " + electionId + ", added " + added + " voters", SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#officer-ops");
            } else if ("addOfficerNote".equals(action)) {
                if (!SessionUtil.canManageElections(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Officer notes require election officer access.");
                    return;
                }
                User admin = SessionUtil.getLoggedInUser(req);
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                String note = InputValidator.required(req.getParameter("officerNote"), "Officer note", 1000);
                officerOpsDao.addOfficerNote(electionId, admin.getId(), note);
                mongoLogDao.logSystemEvent("Officer Note Added", "Election " + electionId + " note added by " + admin.getEmail(), SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#officer-ops");
            } else if ("addIncident".equals(action)) {
                if (!SessionUtil.canManageElections(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Incident reporting requires election officer access.");
                    return;
                }
                User admin = SessionUtil.getLoggedInUser(req);
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                String severity = req.getParameter("severity");
                String category = InputValidator.required(req.getParameter("category"), "Incident category", 80);
                String description = InputValidator.required(req.getParameter("description"), "Incident description", 1000);
                officerOpsDao.addIncident(electionId, admin.getId(), severity, category, description);
                mongoLogDao.logSystemEvent("Election Incident Reported", "Election " + electionId + ", " + severity + ": " + category, SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#officer-ops");
            } else if ("runDryRun".equals(action)) {
                if (!SessionUtil.canManageElections(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Dry run requires election officer access.");
                    return;
                }
                User admin = SessionUtil.getLoggedInUser(req);
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                int candidateCount = candidateDao.countCandidatesByElection(electionId);
                int eligibleCount = eligibilityDao.countEligibleVoters(electionId);
                String result = candidateCount > 0 && eligibleCount > 0 ? "PASS" : "REVIEW";
                String details = "Candidates: " + candidateCount + ", eligible voters: " + eligibleCount + ". No real votes were written.";
                officerOpsDao.addDryRun(electionId, admin.getId(), result, details);
                mongoLogDao.logSystemEvent("Election Dry Run", "Election " + electionId + " result " + result, SessionUtil.getClientIp(req));
                req.getSession().setAttribute("successMessage", "Dry run completed: " + result + ". " + details);
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#officer-ops");
            } else if ("markReadyForResults".equals(action)) {
                if (!SessionUtil.canManageElections(req)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Pre-result validation requires election officer access.");
                    return;
                }
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                java.util.Map<String, Object> integrity = voteDao.getIntegritySummary();
                boolean ready = Boolean.TRUE.equals(integrity.get("verified"));
                electionDao.markReadyForResults(electionId, ready);
                mongoLogDao.logSystemEvent("Pre-result Validation", "Election " + electionId + " ready_for_results=" + ready, SessionUtil.getClientIp(req));
                req.getSession().setAttribute(ready ? "successMessage" : "errorMessage", ready ? "Election marked ready for result announcement." : "Integrity review required before result announcement.");
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard#officer-ops");
            }
        } catch (SQLException ex) {
            throw new ServletException("Database error during admin action", ex);
        } catch (IllegalArgumentException ex) {
            req.getSession().setAttribute("errorMessage", ex.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }
}
