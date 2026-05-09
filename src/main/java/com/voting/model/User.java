package com.voting.model;

public class User {
    private int id;
    private String name;
    private String email;
    private String mobileNumber;
    private String voterIdNumber;
    private java.sql.Date dateOfBirth;
    private Integer age;
    private String electionCenter;
    private String city;
    private String state;
    private String profilePhotoPath;
    private transient String password;
    private String role;
    private boolean hasVoted;
    private String otpCode;
    private java.sql.Timestamp otpExpiry;
    private int failedAttempts;
    private java.sql.Timestamp lockedUntil;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getVoterIdNumber() {
        return voterIdNumber;
    }

    public void setVoterIdNumber(String voterIdNumber) {
        this.voterIdNumber = voterIdNumber;
    }

    public java.sql.Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(java.sql.Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getElectionCenter() {
        return electionCenter;
    }

    public void setElectionCenter(String electionCenter) {
        this.electionCenter = electionCenter;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getProfilePhotoPath() {
        return profilePhotoPath;
    }

    public void setProfilePhotoPath(String profilePhotoPath) {
        this.profilePhotoPath = profilePhotoPath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public java.sql.Timestamp getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(java.sql.Timestamp otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public java.sql.Timestamp getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(java.sql.Timestamp lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public boolean isProfileComplete() {
        return name != null && !name.trim().isEmpty() &&
               mobileNumber != null && !mobileNumber.trim().isEmpty() &&
               voterIdNumber != null && !voterIdNumber.trim().isEmpty() &&
               dateOfBirth != null &&
               electionCenter != null && !electionCenter.trim().isEmpty() &&
               city != null && !city.trim().isEmpty() &&
               state != null && !state.trim().isEmpty() &&
               profilePhotoPath != null && !profilePhotoPath.trim().isEmpty();
    }
}
