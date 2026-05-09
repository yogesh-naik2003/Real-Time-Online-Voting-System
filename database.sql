CREATE DATABASE IF NOT EXISTS online_voting_system;
USE online_voting_system;

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    mobile_number VARCHAR(20),
    voter_id_number VARCHAR(50) UNIQUE,
    date_of_birth DATE,
    age INT,
    election_center VARCHAR(150),
    city VARCHAR(100),
    state VARCHAR(100),
    profile_photo_path VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    role ENUM('super_admin', 'admin', 'election_officer', 'auditor', 'user') NOT NULL DEFAULT 'user',
    has_voted BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS elections (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    election_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE', 'COMPLETED') NOT NULL DEFAULT 'INACTIVE',
    results_announced BOOLEAN NOT NULL DEFAULT FALSE,
    nomination_starts_at TIMESTAMP NULL,
    nomination_ends_at TIMESTAMP NULL,
    ready_for_results BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS candidates (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    manifesto TEXT NOT NULL,
    election_id INT NOT NULL,
    photo_path VARCHAR(255),
    ballot_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_candidate_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS votes (  
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    election_id INT NOT NULL,
    candidate_id INT NOT NULL,
    receipt_id VARCHAR(40) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_votes_user_election (user_id, election_id),
    CONSTRAINT fk_vote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
);

ALTER TABLE elections MODIFY COLUMN status ENUM('ACTIVE', 'INACTIVE', 'COMPLETED') NOT NULL DEFAULT 'INACTIVE';
ALTER TABLE elections ADD COLUMN IF NOT EXISTS results_announced BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE elections ADD COLUMN IF NOT EXISTS nomination_starts_at TIMESTAMP NULL;
ALTER TABLE elections ADD COLUMN IF NOT EXISTS nomination_ends_at TIMESTAMP NULL;
ALTER TABLE elections ADD COLUMN IF NOT EXISTS ready_for_results BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS ballot_order INT NOT NULL DEFAULT 0;

ALTER TABLE users MODIFY COLUMN role ENUM('super_admin', 'admin', 'election_officer', 'auditor', 'user') NOT NULL DEFAULT 'user';

CREATE TABLE IF NOT EXISTS election_eligibility (
    election_id INT NOT NULL,
    user_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (election_id, user_id),
    CONSTRAINT fk_eligibility_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE,
    CONSTRAINT fk_eligibility_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS candidate_nominations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    election_id INT NOT NULL,
    manifesto TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    admin_note VARCHAR(500),
    document_status ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    document_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    UNIQUE KEY uq_nomination_user_election (user_id, election_id),
    CONSTRAINT fk_nomination_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_nomination_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS officer_notes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    election_id INT NOT NULL,
    officer_id INT NOT NULL,
    note VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_officer_note_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE,
    CONSTRAINT fk_officer_note_user FOREIGN KEY (officer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS election_incidents (
    id INT PRIMARY KEY AUTO_INCREMENT,
    election_id INT NOT NULL,
    officer_id INT NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'LOW',
    category VARCHAR(80) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_incident_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE,
    CONSTRAINT fk_incident_user FOREIGN KEY (officer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS election_dry_runs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    election_id INT NOT NULL,
    officer_id INT NOT NULL,
    result VARCHAR(40) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dry_run_election FOREIGN KEY (election_id) REFERENCES elections(id) ON DELETE CASCADE,
    CONSTRAINT fk_dry_run_user FOREIGN KEY (officer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(40) NOT NULL DEFAULT 'info',
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notifications_user_created (user_id, created_at),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
