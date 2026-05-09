package com.voting.model;

public class Candidate {
    private int id;
    private String name;
    private String manifesto;
    private int electionId;
    private String photoPath;
    private int ballotOrder;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManifesto() {
        return manifesto;
    }

    public void setManifesto(String manifesto) {
        this.manifesto = manifesto;
    }

    public int getElectionId() {
        return electionId;
    }

    public void setElectionId(int electionId) {
        this.electionId = electionId;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public int getBallotOrder() {
        return ballotOrder;
    }

    public void setBallotOrder(int ballotOrder) {
        this.ballotOrder = ballotOrder;
    }
}
