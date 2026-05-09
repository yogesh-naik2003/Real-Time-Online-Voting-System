package com.voting.controller;

import com.voting.dao.MongoLogDao;
import com.voting.dao.OfficerOpsDao;
import com.voting.dao.CandidateNominationDao;
import com.voting.dao.ElectionDao;
import com.voting.dao.UserDao;
import com.voting.model.Election;
import com.voting.model.User;
import com.voting.util.InputValidator;
import com.voting.util.SessionUtil;
import com.voting.service.VotingService;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@WebServlet("/user/actions")
@MultipartConfig(maxFileSize = 2 * 1024 * 1024)
public class UserActionServlet extends HttpServlet {
    private final MongoLogDao mongoLogDao = new MongoLogDao();
    private final UserDao userDao = new UserDao();
    private final CandidateNominationDao nominationDao = new CandidateNominationDao();
    private final ElectionDao electionDao = new ElectionDao();
    private final OfficerOpsDao officerOpsDao = new OfficerOpsDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!SessionUtil.hasRole(req, "user")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = req.getParameter("action");
        User user = SessionUtil.getLoggedInUser(req);

        if ("submitGrievance".equals(action)) {
            String issue = req.getParameter("issue");
            try {
                issue = InputValidator.required(issue, "Issue", 1000);
                mongoLogDao.createComplaint(user.getId(), issue);
                mongoLogDao.logSystemEvent("Grievance Submitted", "User " + user.getId() + " submitted a grievance.");
            } catch (IllegalArgumentException ex) {
                req.getSession().setAttribute("profileError", ex.getMessage());
            }
            resp.sendRedirect(req.getContextPath() + "/user/dashboard?grievanceSubmitted=true#grievance");
        } else if ("requestVoteOtp".equals(action)) {
            try {
                User currentUser = userDao.findById(user.getId());
                String destination = new VotingService().requestVoteOtp(currentUser);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"success\":true,\"destination\":\"" + escapeJson(destination) + "\"}");
                return; // Ajax request
            } catch (IllegalStateException ex) {
                resp.setStatus(500);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"" + escapeJson(ex.getMessage()) + "\"}");
                return;
            } catch (SQLException ex) {
                resp.setStatus(500);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"Unable to generate OTP right now.\"}");
                return;
            }
        } else if ("updateProfile".equals(action)) {
            try {
                updateProfile(req, user);
                User refreshedUser = userDao.findById(user.getId());
                req.getSession().setAttribute("user", refreshedUser);
                mongoLogDao.logSystemEvent("Profile Updated", "User " + user.getId() + " updated profile details.");
                resp.sendRedirect(req.getContextPath() + "/user/dashboard?profileUpdated=true#my-profile");
            } catch (IllegalArgumentException ex) {
                req.getSession().setAttribute("profileError", ex.getMessage());
                resp.sendRedirect(req.getContextPath() + "/user/dashboard#my-profile");
            } catch (SQLException ex) {
                throw new ServletException("Unable to update profile", ex);
            }
        } else if ("submitNomination".equals(action)) {
            try {
                int electionId = Integer.parseInt(req.getParameter("electionId"));
                Election election = electionDao.findById(electionId);
                if (election == null) {
                    throw new IllegalArgumentException("Please select a valid election.");
                }
                if ("COMPLETED".equalsIgnoreCase(election.getStatus())) {
                    throw new IllegalArgumentException("Candidate nomination is closed for completed elections.");
                }
                if (!officerOpsDao.isNominationOpen(election)) {
                    throw new IllegalArgumentException("Candidate nomination is not open for this election right now.");
                }
                String manifesto = InputValidator.required(req.getParameter("manifesto"), "Manifesto", 2000);
                nominationDao.createNomination(user.getId(), electionId, manifesto);
                mongoLogDao.logSystemEvent("Candidate Nomination Submitted", "User " + user.getId() + " applied for election " + electionId, SessionUtil.getClientIp(req));
                resp.sendRedirect(req.getContextPath() + "/user/dashboard?nominationSubmitted=true#nomination");
            } catch (IllegalArgumentException ex) {
                req.getSession().setAttribute("profileError", ex.getMessage());
                resp.sendRedirect(req.getContextPath() + "/user/dashboard#nomination");
            } catch (SQLException ex) {
                throw new ServletException("Unable to submit candidate nomination", ex);
            }
        } else {
            resp.sendRedirect(req.getContextPath() + "/user/dashboard");
        }
    }

    private void updateProfile(HttpServletRequest req, User sessionUser) throws IOException, ServletException, SQLException {
        User existingUser = userDao.findById(sessionUser.getId());
        if (existingUser == null) {
            throw new IllegalArgumentException("Unable to find your profile.");
        }

        String name = InputValidator.required(req.getParameter("name"), "Full name", 100);
        String email = InputValidator.email(req.getParameter("email")).toLowerCase(Locale.ROOT);
        String mobileNumber = InputValidator.mobile(req.getParameter("mobileNumber"));
        String voterIdNumber = InputValidator.voterId(req.getParameter("voterIdNumber")).toUpperCase(Locale.ROOT);
        String dateOfBirthValue = required(req, "dateOfBirth", "Date of birth is required.");
        String electionCenter = InputValidator.required(req.getParameter("electionCenter"), "Election center", 150);
        String city = InputValidator.required(req.getParameter("city"), "City", 100);
        String state = InputValidator.required(req.getParameter("state"), "State", 100);

        User emailOwner = userDao.findByEmail(email);
        if (emailOwner != null && emailOwner.getId() != existingUser.getId()) {
            throw new IllegalArgumentException("This email is already used by another account.");
        }
        User voterIdOwner = userDao.findByVoterIdNumber(voterIdNumber);
        if (voterIdOwner != null && voterIdOwner.getId() != existingUser.getId()) {
            throw new IllegalArgumentException("This voter ID is already used by another account.");
        }

        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(dateOfBirthValue);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Please provide a valid date of birth.");
        }

        int calculatedAge = Period.between(dateOfBirth, LocalDate.now()).getYears();
        if (dateOfBirth.isAfter(LocalDate.now()) || calculatedAge < 18) {
            throw new IllegalArgumentException("Voter must be at least 18 years old.");
        }

        String profilePhotoPath = existingUser.getProfilePhotoPath();
        Part photoPart = req.getPart("profilePhoto");
        if (photoPart != null && photoPart.getSize() > 0) {
            String contentType = photoPart.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new IllegalArgumentException("Profile photo must be an image file.");
            }
            String extension = getExtension(photoPart);
            String fileName = "user-" + existingUser.getId() + "-" + System.currentTimeMillis() + extension;
            Path uploadDir = Paths.get(getServletContext().getRealPath("/uploads/profile"));
            Files.createDirectories(uploadDir);
            photoPart.write(uploadDir.resolve(fileName).toString());
            profilePhotoPath = "/uploads/profile/" + fileName;
        }

        existingUser.setName(name);
        existingUser.setEmail(email);
        existingUser.setMobileNumber(mobileNumber);
        existingUser.setVoterIdNumber(voterIdNumber);
        existingUser.setDateOfBirth(java.sql.Date.valueOf(dateOfBirth));
        existingUser.setElectionCenter(electionCenter);
        existingUser.setCity(city);
        existingUser.setState(state);
        existingUser.setProfilePhotoPath(profilePhotoPath);
        existingUser.setAge(calculatedAge);
        userDao.updateProfile(existingUser);
    }

    private String required(HttpServletRequest req, String parameter, String message) {
        String value = req.getParameter(parameter);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String getExtension(Part part) {
        String submittedFileName = getSubmittedFileName(part);
        if (submittedFileName == null) {
            return ".jpg";
        }
        String lowerName = submittedFileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".png")) return ".png";
        if (lowerName.endsWith(".gif")) return ".gif";
        if (lowerName.endsWith(".webp")) return ".webp";
        return ".jpg";
    }

    private String getSubmittedFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) {
            return null;
        }
        String[] tokens = contentDisposition.split(";");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.startsWith("filename=")) {
                return trimmed.substring("filename=".length()).trim().replace("\"", "");
            }
        }
        return null;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
