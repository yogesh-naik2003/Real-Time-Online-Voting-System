package com.voting.service;

import com.voting.dao.CandidateDao;
import com.voting.dao.CandidateNominationDao;
import com.voting.dao.ElectionEligibilityDao;
import com.voting.dao.ElectionDao;
import com.voting.dao.MongoLogDao;
import com.voting.dao.NotificationDao;
import com.voting.dao.UserDao;
import com.voting.dao.VoteDao;
import com.voting.model.Candidate;
import com.voting.model.DashboardStats;
import com.voting.model.Election;
import com.voting.model.User;
import com.voting.model.VoteStat;
import com.voting.util.EmailUtil;
import com.voting.websocket.VoteUpdateBroadcaster;

import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VotingService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDao userDao = new UserDao();
    private final ElectionDao electionDao = new ElectionDao();
    private final CandidateDao candidateDao = new CandidateDao();
    private final CandidateNominationDao nominationDao = new CandidateNominationDao();
    private final ElectionEligibilityDao eligibilityDao = new ElectionEligibilityDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final VoteDao voteDao = new VoteDao();
    private final MongoLogDao mongoLogDao = new MongoLogDao();

    public DashboardStats getDashboardStats() throws SQLException {
        Election activeElection = electionDao.findActiveElection();
        int activeVotes = activeElection == null ? 0 : voteDao.countVotesByElection(activeElection.getId());
        int activeCandidates = activeElection == null ? 0 : candidateDao.countCandidatesByElection(activeElection.getId());
        return new DashboardStats(
                userDao.countUsers(),
                activeVotes,
                activeCandidates,
                activeElection != null ? "ACTIVE" : "INACTIVE"
        );
    }

    public Map<String, Object> getUserDashboardPayload(User user) throws SQLException {
        Map<String, Object> payload = new HashMap<>();
        Election activeElection = electionDao.findActiveElection();
        User currentUser = userDao.findById(user.getId());
        List<Election> allElections = electionDao.findAll();
        Map<Integer, List<VoteStat>> announcedResults = getAnnouncedElectionResults(allElections);
        boolean hasVotedInActiveElection = activeElection != null
                && voteDao.hasUserVotedInElection(currentUser.getId(), activeElection.getId());
        payload.put("user", currentUser);
        payload.put("activeElection", activeElection);
        payload.put("elections", allElections);
        payload.put("hasVoted", hasVotedInActiveElection);
        payload.put("profileVerificationStatus", getProfileVerificationStatus(currentUser));
        payload.put("voteHistory", voteDao.getVoteHistoryByUser(currentUser.getId()));
        payload.put("complaints", mongoLogDao.getComplaintsByUser(currentUser.getId()));
        payload.put("nominations", nominationDao.findByUser(currentUser.getId()));
        payload.put("announcedResults", announcedResults);
        List<String> notifications = notificationDao.findRecentMessagesForUser(currentUser.getId(), 8);
        notifications.addAll(buildUserNotifications(activeElection, getProfileVerificationStatus(currentUser), hasVotedInActiveElection, allElections, announcedResults));
        payload.put("notifications", notifications);
        if (activeElection != null) {
            boolean eligible = eligibilityDao.isEligible(activeElection.getId(), currentUser.getId());
            payload.put("eligibleForActiveElection", eligible);
            payload.put("candidates", eligible ? candidateDao.findByElectionId(activeElection.getId()) : new ArrayList<Candidate>());
            payload.put("activeElectionEndsAt", getElectionEndsAt(activeElection));
            payload.put("turnout", getTurnoutForElection(activeElection.getId()));
            payload.put("resultsAnnounced", activeElection.isResultsAnnounced());
        } else {
            Election latestElection = electionDao.findLatestElection();
            payload.put("candidates", new ArrayList<Candidate>());
            payload.put("activeElectionEndsAt", null);
            payload.put("turnout", getEmptyTurnout());
            payload.put("resultsAnnounced", latestElection != null && latestElection.isResultsAnnounced());
            payload.put("eligibleForActiveElection", true);
            if (latestElection != null && latestElection.isResultsAnnounced()) {
                payload.put("finalResults", voteDao.getResultsByElection(latestElection.getId()));
                payload.put("latestElection", latestElection);
            }
        }
        return payload;
    }

    public Map<Integer, List<VoteStat>> getAnnouncedElectionResults() throws SQLException {
        return getAnnouncedElectionResults(electionDao.findAll());
    }

    private Map<Integer, List<VoteStat>> getAnnouncedElectionResults(List<Election> elections) throws SQLException {
        Map<Integer, List<VoteStat>> resultsByElection = new LinkedHashMap<>();
        for (Election election : elections) {
            if (election.isResultsAnnounced()) {
                resultsByElection.put(election.getId(), voteDao.getResultsByElection(election.getId()));
            }
        }
        return resultsByElection;
    }

    private List<String> buildUserNotifications(Election activeElection, String profileVerificationStatus,
                                                boolean hasVoted, List<Election> elections,
                                                Map<Integer, List<VoteStat>> announcedResults) {
        List<String> notifications = new ArrayList<>();
        if (activeElection != null) {
            notifications.add("Election '" + activeElection.getTitle() + "' is now LIVE!");
        }
        if ("Verified Voter".equals(profileVerificationStatus)) {
            notifications.add("Your profile has been successfully verified.");
        }
        if (hasVoted) {
            notifications.add("Thank you! Your vote has been securely recorded.");
        }
        for (Election election : elections) {
            if (!election.isResultsAnnounced()) {
                continue;
            }
            List<VoteStat> results = announcedResults.get(election.getId());
            VoteStat winner = results != null && !results.isEmpty() ? results.get(0) : null;
            if (winner != null && winner.getTotalVotes() > 0) {
                notifications.add("Results announced for '" + election.getTitle() + "': " + winner.getCandidateName() + " won with " + winner.getTotalVotes() + " votes.");
            } else {
                notifications.add("Results announced for '" + election.getTitle() + "'. No votes were recorded.");
            }
        }
        return notifications;
    }

    public String requestVoteOtp(User user) throws SQLException {
        String otp = String.format("%06d", RANDOM.nextInt(1000000));
        java.sql.Timestamp expiry = new java.sql.Timestamp(System.currentTimeMillis() + 5 * 60 * 1000); // 5 mins
        userDao.setOtp(user.getId(), otp, expiry);
        try {
            EmailUtil.sendOtp(user.getEmail(), user.getName(), otp);
            mongoLogDao.logSecurityEvent("otp_requested", user.getEmail(), null, "OTP emailed to registered address.");
            return maskEmail(user.getEmail());
        } catch (IllegalStateException ex) {
            userDao.setOtp(user.getId(), null, null);
            mongoLogDao.logSecurityEvent("otp_email_failed", user.getEmail(), null, ex.getMessage());
            throw ex;
        }
    }

    public String castVote(User user, int candidateId, String otp, String ipAddress, String userAgent) throws SQLException {
        Election activeElection = electionDao.findActiveElection();
        if (activeElection == null) {
            throw new IllegalStateException("There is no active election right now.");
        }
        User currentUser = userDao.findById(user.getId());
        if (voteDao.hasUserVotedInElection(currentUser.getId(), activeElection.getId())) {
            throw new IllegalStateException("You have already voted.");
        }
        if (!eligibilityDao.isEligible(activeElection.getId(), currentUser.getId())) {
            throw new IllegalStateException("You are not eligible for this election. Contact the election office.");
        }

        // OTP Verification
        if (currentUser.getOtpCode() == null || !currentUser.getOtpCode().equals(otp)) {
            mongoLogDao.logSecurityEvent("invalid_otp_attempt", user.getEmail(), ipAddress, "Invalid OTP entered: " + otp);
            throw new IllegalStateException("Invalid security code (OTP).");
        }
        if (currentUser.getOtpExpiry() == null || currentUser.getOtpExpiry().before(new java.sql.Timestamp(System.currentTimeMillis()))) {
            throw new IllegalStateException("Security code (OTP) has expired.");
        }

        List<Candidate> candidates = candidateDao.findByElectionId(activeElection.getId());
        boolean candidateExists = candidates.stream().anyMatch(candidate -> candidate.getId() == candidateId);
        if (!candidateExists) {
            throw new IllegalStateException("Selected candidate is not part of the active election.");
        }

        String receiptId = generateReceiptId(activeElection.getId(), user.getId());
        String signature = com.voting.util.DigitalSignatureUtil.generateSignature(user.getId(), candidateId, receiptId);
        
        voteDao.castVote(user.getId(), activeElection.getId(), candidateId, receiptId, ipAddress, userAgent, signature);
        userDao.updateVotingStatus(user.getId(), true);
        userDao.setOtp(user.getId(), null, null); // Clear OTP after use
        notificationDao.create(user.getId(), "Vote Recorded", "Receipt " + receiptId + " proves your vote was recorded without showing your candidate choice.", "vote");

        mongoLogDao.logVoteEvent(user.getId(), candidateId);
        mongoLogDao.logSecurityEvent("vote_cast", user.getEmail(), ipAddress, "Vote recorded with receipt " + receiptId + " and digital signature.");
        VoteUpdateBroadcaster.broadcast("vote_cast");
        return receiptId;
    }

    public void broadcastElectionUpdate(String action, int electionId, String details) {
        mongoLogDao.logElectionAction(action, electionId, details);
        VoteUpdateBroadcaster.broadcast("election_updated");
    }

    public List<VoteStat> getResultsForActiveElection() throws SQLException {
        Election activeElection = electionDao.findActiveElection();
        if (activeElection == null) {
            Election latestElection = electionDao.findLatestElection();
            return latestElection == null ? new ArrayList<VoteStat>() : voteDao.getResultsByElection(latestElection.getId());
        }
        return voteDao.getResultsByElection(activeElection.getId());
    }

    public List<Map<String, Object>> getVoteActivityTrend() throws SQLException {
        Election activeElection = electionDao.findActiveElection();
        if (activeElection == null) {
            Election latestElection = electionDao.findLatestElection();
            return latestElection == null ? new ArrayList<Map<String, Object>>() : voteDao.getHourlyActivityByElection(latestElection.getId());
        }
        return voteDao.getHourlyActivityByElection(activeElection.getId());
    }

    public Map<String, Object> getDemographicStats() throws SQLException {
        Map<String, Object> demographics = new HashMap<>();
        List<Map<String, Object>> genderData = new ArrayList<>();
        genderData.add(dataPoint("Male", 55));
        genderData.add(dataPoint("Female", 42));
        genderData.add(dataPoint("Other", 3));
        demographics.put("genderData", genderData);

        List<Map<String, Object>> ageData = new ArrayList<>();
        ageData.add(dataPoint("18-25", 25));
        ageData.add(dataPoint("26-45", 45));
        ageData.add(dataPoint("46-60", 20));
        ageData.add(dataPoint("60+", 10));
        demographics.put("ageData", ageData);
        return demographics;
    }

    private Map<String, Object> dataPoint(String label, int value) {
        Map<String, Object> point = new HashMap<>();
        point.put("label", label);
        point.put("value", value);
        return point;
    }

    private Map<String, Object> getTurnoutForElection(int electionId) throws SQLException {
        int registeredVoters = eligibilityDao.countEligibleVoters(electionId);
        int votesCast = voteDao.countVotesByElection(electionId);
        Map<String, Object> turnout = new HashMap<>();
        turnout.put("registeredVoters", registeredVoters);
        turnout.put("votesCast", votesCast);
        turnout.put("percentage", registeredVoters == 0 ? 0 : Math.round((votesCast * 10000.0) / registeredVoters) / 100.0);
        return turnout;
    }

    private Map<String, Object> getEmptyTurnout() {
        Map<String, Object> turnout = new HashMap<>();
        turnout.put("registeredVoters", 0);
        turnout.put("votesCast", 0);
        turnout.put("percentage", 0);
        return turnout;
    }

    private String getElectionEndsAt(Election election) {
        if (election.getElectionDate() == null || election.getStartTime() == null || election.getEndTime() == null) {
            return null;
        }
        Time startTime = election.getStartTime();
        Time endTime = election.getEndTime();
        LocalDateTime startsAt = LocalDateTime.of(election.getElectionDate().toLocalDate(), startTime.toLocalTime());
        LocalDateTime endsAt = LocalDateTime.of(election.getElectionDate().toLocalDate(), endTime.toLocalTime());
        if (!endsAt.isAfter(startsAt)) {
            endsAt = endsAt.plusDays(1);
        }
        return endsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String getProfileVerificationStatus(User user) {
        if (user.getVoterIdNumber() == null || user.getVoterIdNumber().trim().isEmpty()
                || user.getMobileNumber() == null || user.getMobileNumber().trim().isEmpty()
                || user.getElectionCenter() == null || user.getElectionCenter().trim().isEmpty()) {
            return "Profile Incomplete";
        }
        return "Verified Voter";
    }

    private String generateReceiptId(int electionId, int userId) {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return "VOTE-" + electionId + "-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "your registered email";
        }
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String domain = email.substring(at);
        if (name.isEmpty()) {
            return "***" + domain;
        }
        String visible = name.length() <= 2 ? name.substring(0, 1) : name.substring(0, 2);
        return visible + "***" + domain;
    }
}
