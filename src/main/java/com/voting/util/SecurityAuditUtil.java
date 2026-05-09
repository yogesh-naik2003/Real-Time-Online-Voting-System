package com.voting.util;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import java.time.Instant;

/**
 * Utility for recording security-sensitive events to the MongoDB audit log.
 * Tracks logins, failures, rate limit hits, and data modifications.
 */
public class SecurityAuditUtil {

    private static final String COLLECTION_NAME = "security_audit_logs";

    public static void log(String eventType, String userId, String ipAddress, String details) {
        try {
            MongoDatabase db = MongoUtil.getDatabase();
            if (db == null) return;
            
            MongoCollection<Document> collection = db.getCollection(COLLECTION_NAME);
            
            Document logEntry = new Document()
                .append("timestamp", Instant.now().toString())
                .append("action", eventType)
                .append("actor", userId != null ? userId : "ANONYMOUS")
                .append("ip_address", ClientIpUtil.normalize(ipAddress))
                .append("eventType", eventType)
                .append("userId", userId != null ? userId : "ANONYMOUS")
                .append("ipAddress", ipAddress)
                .append("details", details)
                .append("severity", determineSeverity(eventType));
                
            collection.insertOne(logEntry);
        } catch (Exception e) {
            // Silently fail to avoid disrupting the main application flow, 
            // but in production, we would use a fallback logging mechanism.
            System.err.println("Audit Log Failure: " + e.getMessage());
        }
    }

    private static String determineSeverity(String eventType) {
        switch (eventType) {
            case "LOGIN_FAILURE":
            case "RATE_LIMIT_HIT":
                return "MEDIUM";
            case "UNAUTHORIZED_ACCESS":
            case "SQL_INJECTION_ATTEMPT":
                return "HIGH";
            case "VOTE_CAST":
                return "LOW";
            default:
                return "INFO";
        }
    }
}
