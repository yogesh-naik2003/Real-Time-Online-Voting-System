package com.voting.service;

import com.voting.model.Vote;
import com.voting.util.SecurityAuditUtil;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI-Inspired Fraud Detection Service.
 * Analyzes voting patterns (velocity, IP distribution, timing) to flag suspicious activity.
 */
public class FraudDetectionService {

    // Simple heuristic: If an IP casts more than 3 votes within 1 minute, flag it.
    private static final int THRESHOLD_VOTES_PER_IP = 3;
    private static final long TIME_WINDOW_MS = 60000; // 1 minute
    
    private final Map<String, IPVoteTracker> ipTrackers = new ConcurrentHashMap<>();

    public void analyzeVote(Vote vote, String ipAddress) {
        long currentTime = System.currentTimeMillis();
        
        IPVoteTracker tracker = ipTrackers.computeIfAbsent(ipAddress, k -> new IPVoteTracker(currentTime));
        
        if (currentTime - tracker.windowStart > TIME_WINDOW_MS) {
            tracker.count.set(1);
            tracker.windowStart = currentTime;
        } else {
            int currentCount = tracker.count.incrementAndGet();
            if (currentCount > THRESHOLD_VOTES_PER_IP) {
                flagFraud(vote, ipAddress, "High-velocity voting detected from IP: " + currentCount + " votes in 1min");
            }
        }

        // Additional heuristic: Voting during "Impossible hours" (e.g., 3:00 AM - 4:00 AM) if applicable
        // Or checking if the same voter ID is used from different IPs (though voterId should be unique)
    }

    private void flagFraud(Vote vote, String ipAddress, String reason) {
        System.err.println("FRAUD ALERT: " + reason + " | IP: " + ipAddress);
        SecurityAuditUtil.log("FRAUD_ATTEMPT", vote.getVoterId(), ipAddress, reason);
        // In a real AI implementation, we would update a "risk_score" in the database
    }

    private static class IPVoteTracker {
        AtomicInteger count = new AtomicInteger(0);
        long windowStart;

        IPVoteTracker(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
