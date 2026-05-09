package com.voting.model;

public class DashboardStats {
    private int totalUsers;
    private int totalVotes;
    private int totalCandidates;
    private String electionStatus;

    public DashboardStats(int totalUsers, int totalVotes, int totalCandidates, String electionStatus) {
        this.totalUsers = totalUsers;
        this.totalVotes = totalVotes;
        this.totalCandidates = totalCandidates;
        this.electionStatus = electionStatus;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public int getTotalCandidates() {
        return totalCandidates;
    }

    public String getElectionStatus() {
        return electionStatus;
    }
}
