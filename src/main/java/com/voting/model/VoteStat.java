package com.voting.model;

public class VoteStat {
    private int candidateId;
    private String candidateName;
    private int totalVotes;

    public VoteStat(int candidateId, String candidateName, int totalVotes) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.totalVotes = totalVotes;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public int getTotalVotes() {
        return totalVotes;
    }
}
