package com.voting.model;

public class Election {
    private int id;
    private String title;
    private String description;
    private String status;
    private java.sql.Date electionDate;
    private java.sql.Time startTime;
    private java.sql.Time endTime;
    private boolean resultsAnnounced;
    private java.sql.Timestamp nominationStartsAt;
    private java.sql.Timestamp nominationEndsAt;
    private boolean readyForResults;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.sql.Date getElectionDate() {
        return electionDate;
    }

    public void setElectionDate(java.sql.Date electionDate) {
        this.electionDate = electionDate;
    }

    public java.sql.Time getStartTime() {
        return startTime;
    }

    public void setStartTime(java.sql.Time startTime) {
        this.startTime = startTime;
    }

    public java.sql.Time getEndTime() {
        return endTime;
    }

    public void setEndTime(java.sql.Time endTime) {
        this.endTime = endTime;
    }

    public boolean isResultsAnnounced() {
        return resultsAnnounced;
    }

    public void setResultsAnnounced(boolean resultsAnnounced) {
        this.resultsAnnounced = resultsAnnounced;
    }

    public java.sql.Timestamp getNominationStartsAt() {
        return nominationStartsAt;
    }

    public void setNominationStartsAt(java.sql.Timestamp nominationStartsAt) {
        this.nominationStartsAt = nominationStartsAt;
    }

    public java.sql.Timestamp getNominationEndsAt() {
        return nominationEndsAt;
    }

    public void setNominationEndsAt(java.sql.Timestamp nominationEndsAt) {
        this.nominationEndsAt = nominationEndsAt;
    }

    public boolean isReadyForResults() {
        return readyForResults;
    }

    public void setReadyForResults(boolean readyForResults) {
        this.readyForResults = readyForResults;
    }
}
