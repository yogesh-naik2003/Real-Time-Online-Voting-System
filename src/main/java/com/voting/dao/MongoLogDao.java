package com.voting.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.voting.model.User;
import com.voting.util.ClientIpUtil;
import com.voting.util.MongoUtil;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MongoLogDao {
    private final MongoDatabase database = MongoUtil.getDatabase();

    public void logElectionAction(String action, int electionId, String details, String ipAddress) {
        insert("election_logs", new Document("action", action)
                .append("election_id", electionId)
                .append("details", details)
                .append("ip_address", ClientIpUtil.normalize(ipAddress))
                .append("timestamp", Instant.now().toString()));
    }

    public void logElectionAction(String action, int electionId, String details) {
        logElectionAction(action, electionId, details, null);
    }

    public void logVoteEvent(int userId, int candidateId) {
        insert("vote_events", new Document("user_id", userId)
                .append("candidate_id", candidateId)
                .append("timestamp", Instant.now().toString()));
    }

    public void saveActiveSession(String sessionId, int userId) {
        insert("active_sessions", new Document("session_id", sessionId)
                .append("user_id", userId)
                .append("login_time", Instant.now().toString()));
    }

    public void removeActiveSession(String sessionId) {
        database.getCollection("active_sessions").deleteOne(new Document("session_id", sessionId));
    }

    public void logSystemEvent(String action, String details, String ipAddress) {
        insert("election_logs", new Document("action", action)
                .append("details", details)
                .append("ip_address", ClientIpUtil.normalize(ipAddress))
                .append("timestamp", Instant.now().toString()));
    }

    public void logSystemEvent(String action, String details) {
        logSystemEvent(action, details, null);
    }

    public void logSecurityEvent(String action, String actor, String ipAddress, String details) {
        insert("security_audit_logs", new Document("action", action)
                .append("actor", actor == null ? "anonymous" : actor)
                .append("ip_address", ClientIpUtil.normalize(ipAddress))
                .append("details", details == null ? "" : details)
                .append("timestamp", Instant.now().toString()));
    }

    private void insert(String collectionName, Document document) {
        MongoCollection<Document> collection = database.getCollection(collectionName);
        collection.insertOne(document);
    }

    // --- PENDING USERS ---
    public void savePendingUser(User user) {
        insert("pending_users", new Document("name", user.getName())
                .append("email", user.getEmail())
                .append("mobile_number", user.getMobileNumber())
                .append("voter_id_number", user.getVoterIdNumber())
                .append("date_of_birth", user.getDateOfBirth() == null ? null : user.getDateOfBirth().toString())
                .append("age", user.getAge())
                .append("election_center", user.getElectionCenter())
                .append("city", user.getCity())
                .append("state", user.getState())
                .append("profile_photo_path", user.getProfilePhotoPath())
                .append("password", user.getPassword())
                .append("timestamp", Instant.now().toString()));
    }

    public List<Document> getPendingUsers() {
        List<Document> list = new ArrayList<>();
        database.getCollection("pending_users").find().into(list);
        return list;
    }

    public Document getPendingUser(String objectId) {
        return database.getCollection("pending_users").find(new Document("_id", new ObjectId(objectId))).first();
    }

    public void removePendingUser(String objectId) {
        database.getCollection("pending_users").deleteOne(new Document("_id", new ObjectId(objectId)));
    }

    public boolean isUserPending(String email) {
        return database.getCollection("pending_users").find(new Document("email", email)).first() != null;
    }

    public boolean isVoterIdPending(String voterIdNumber) {
        return database.getCollection("pending_users").find(new Document("voter_id_number", voterIdNumber)).first() != null;
    }

    // --- LOGS & TICKETS ---
    public List<Document> getRecentLogs() {
        List<Document> logs = new ArrayList<>();
        database.getCollection("election_logs").find().sort(new Document("timestamp", -1)).limit(50).into(logs);
        database.getCollection("security_audit_logs").find().sort(new Document("timestamp", -1)).limit(50).into(logs);
        for (Document log : logs) {
            normalizeLog(log);
        }
        logs.sort(Comparator.comparing((Document document) -> String.valueOf(document.get("timestamp")), Comparator.nullsLast(String::compareTo)).reversed());
        if (logs.size() > 50) {
            return new ArrayList<>(logs.subList(0, 50));
        }
        return logs;
    }

    public List<Document> getRecentLogs(int limit) {
        List<Document> logs = new ArrayList<>();
        database.getCollection("election_logs").find().sort(new Document("timestamp", -1)).limit(limit).into(logs);
        database.getCollection("security_audit_logs").find().sort(new Document("timestamp", -1)).limit(limit).into(logs);
        for (Document log : logs) {
            normalizeLog(log);
        }
        logs.sort(Comparator.comparing((Document document) -> String.valueOf(document.get("timestamp")), Comparator.nullsLast(String::compareTo)).reversed());
        if (logs.size() > limit) {
            return new ArrayList<>(logs.subList(0, limit));
        }
        return logs;
    }

    public Map<String, Object> getSuspiciousActivitySummary() {
        List<Document> logs = getRecentLogs(200);
        int failedLogins = 0;
        int invalidOtps = 0;
        int lockedAccounts = 0;
        int deniedAccess = 0;
        Map<String, Integer> eventsByIp = new LinkedHashMap<>();

        for (Document log : logs) {
            String action = String.valueOf(log.get("action")).toLowerCase(Locale.ROOT);
            String details = String.valueOf(log.get("details")).toLowerCase(Locale.ROOT);
            String ip = String.valueOf(log.get("ip_address"));
            if (action.contains("login_failure") || details.contains("incorrect password") || details.contains("user not found")) {
                failedLogins++;
            }
            if (action.contains("invalid_otp")) {
                invalidOtps++;
            }
            if (action.contains("lockout") || action.contains("login_locked")) {
                lockedAccounts++;
            }
            if (action.contains("forbidden") || details.contains("access denied") || details.contains("csrf")) {
                deniedAccess++;
            }
            if (ip != null && !ip.trim().isEmpty() && !"-".equals(ip)) {
                eventsByIp.put(ip, eventsByIp.getOrDefault(ip, 0) + 1);
            }
        }

        int anomalyScore = Math.min(100, (failedLogins * 8) + (invalidOtps * 10) + (lockedAccounts * 18) + (deniedAccess * 12));
        String riskLevel = anomalyScore >= 70 ? "High" : anomalyScore >= 35 ? "Medium" : "Low";
        String busiestIp = "-";
        int busiestIpEvents = 0;
        for (Map.Entry<String, Integer> entry : eventsByIp.entrySet()) {
            if (entry.getValue() > busiestIpEvents) {
                busiestIp = entry.getKey();
                busiestIpEvents = entry.getValue();
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("failedLogins", failedLogins);
        summary.put("invalidOtps", invalidOtps);
        summary.put("lockedAccounts", lockedAccounts);
        summary.put("deniedAccess", deniedAccess);
        summary.put("anomalyScore", anomalyScore);
        summary.put("riskLevel", riskLevel);
        summary.put("busiestIp", busiestIp);
        summary.put("busiestIpEvents", busiestIpEvents);
        summary.put("logWindow", logs.size());
        return summary;
    }

    public void addAuditorNote(int auditorId, String auditorName, String note, String ipAddress) {
        insert("auditor_notes", new Document("auditor_id", auditorId)
                .append("auditor_name", auditorName == null ? "Auditor" : auditorName)
                .append("note", note == null ? "" : note.trim())
                .append("ip_address", ClientIpUtil.normalize(ipAddress))
                .append("timestamp", Instant.now().toString()));
    }

    public List<Document> getRecentAuditorNotes() {
        List<Document> notes = new ArrayList<>();
        database.getCollection("auditor_notes").find().sort(new Document("timestamp", -1)).limit(25).into(notes);
        for (Document note : notes) {
            normalizeLog(note);
            if (note.get("auditor_name") == null) {
                note.put("auditor_name", note.get("actor") == null ? "Auditor" : note.get("actor"));
            }
        }
        return notes;
    }

    private void normalizeLog(Document log) {
        Object timestamp = log.get("timestamp");
        if (timestamp instanceof Date) {
            log.put("timestamp", ((Date) timestamp).toInstant().toString());
        } else if (timestamp == null) {
            log.put("timestamp", "");
        }

        if (log.get("action") == null && log.get("eventType") != null) {
            log.put("action", log.get("eventType"));
        }
        if (log.get("actor") == null && log.get("userId") != null) {
            log.put("actor", log.get("userId"));
        }
        if (log.get("ip_address") == null && log.get("ipAddress") != null) {
            log.put("ip_address", log.get("ipAddress"));
        }
        if (log.get("details") == null) {
            log.put("details", "");
        }
        if (log.get("actor") == null) {
            log.put("actor", "system");
        }
        if (log.get("ip_address") == null || String.valueOf(log.get("ip_address")).trim().isEmpty()) {
            log.put("ip_address", "-");
        } else {
            log.put("ip_address", ClientIpUtil.normalize(String.valueOf(log.get("ip_address"))));
        }
    }

    public void createComplaint(int userId, String issue) {
        insert("complaints", new Document("user_id", userId)
                .append("issue", issue)
                .append("status", "Open")
                .append("admin_reply", "")
                .append("timestamp", Instant.now().toString()));
    }

    public List<Document> getOpenComplaints() {
        List<Document> list = new ArrayList<>();
        database.getCollection("complaints").find(new Document("status", "Open")).into(list);
        return list;
    }

    public List<Document> getAllComplaints() {
        List<Document> list = new ArrayList<>();
        database.getCollection("complaints")
                .find()
                .sort(new Document("timestamp", -1))
                .into(list);
        return list;
    }

    public List<Document> getComplaintsByUser(int userId) {
        List<Document> list = new ArrayList<>();
        database.getCollection("complaints")
                .find(new Document("user_id", userId))
                .sort(new Document("timestamp", -1))
                .into(list);
        return list;
    }

    public void resolveComplaint(String objectId, String adminReply) {
        database.getCollection("complaints").updateOne(
            new Document("_id", new ObjectId(objectId)),
            new Document("$set", new Document("status", "Resolved")
                    .append("admin_reply", adminReply == null ? "" : adminReply.trim())
                    .append("resolved_at", Instant.now().toString()))
        );
    }
}
