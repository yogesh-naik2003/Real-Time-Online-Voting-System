# Real-Time Online Voting System

## Introduction

The Real-Time Online Voting System is a Java web application designed to manage digital elections through a secure admin and voter workflow. It allows voters to register, log in, view active elections, cast one vote, receive a vote receipt, submit grievances, and view election status. Administrators can approve voter registrations, create elections, manage candidates, monitor voting activity, export reports, and view live results.

The project uses Java Servlets and JSP for the web layer, MySQL for core election data, MongoDB for logs and operational records, and WebSockets for real-time dashboard updates.

## Objectives

- Provide a browser-based voting platform for voters and administrators.
- Enforce one vote per registered voter.
- Support real-time vote result updates.
- Store structured election data in MySQL.
- Store audit logs, pending registrations, active sessions, vote events, and complaints in MongoDB.
- Provide admin reports for results, voter rolls, and security audits.
- Improve safety through authentication, CSRF protection, password hashing, validation, session controls, and security headers.

## Workflow

### Voter Workflow

1. The voter opens the application and registers with name, email, mobile number, voter ID, date of birth, age, election center, city, state, and password.
2. The registration form validates required fields, email format, mobile number, voter ID, password strength, age, and captcha.
3. If ID verification is enabled, the registration is stored in MongoDB as a pending user.
4. The admin approves or rejects the pending voter from the admin dashboard.
5. After approval, the voter can log in.
6. The voter opens the user dashboard and views the active election.
7. If the voter has not voted, the voter selects a candidate and casts a vote.
8. The system records the vote in MySQL, marks the voter as already voted, logs the event in MongoDB, generates a receipt ID, and broadcasts a WebSocket update.
9. The voter can view vote history, profile status, turnout details, and grievance responses.

### Admin Workflow

1. The admin logs in using the default admin account or an existing admin account.
2. The admin dashboard displays total users, total votes, total candidates, election status, results, demographics, activity trends, logs, complaints, and pending approvals.
3. The admin approves or rejects pending voter registrations.
4. The admin creates elections with title, description, date, start time, and end time.
5. The admin activates or deactivates elections.
6. The admin adds, edits, or deletes candidates for an election.
7. The admin resolves voter grievances.
8. The admin updates system settings such as registration availability, ID verification, and maintenance mode.
9. The admin downloads reports such as final results PDF, voter roll CSV, and security audit XML.

## Working

The application follows a request-response web model with live update support:

- JSP pages render the login, registration, admin dashboard, and user dashboard screens.
- Servlet controllers receive HTTP requests from forms and dashboard actions.
- Filters protect restricted routes and validate CSRF tokens.
- DAO classes communicate with MySQL and MongoDB.
- The service layer applies voting rules such as active election checks, candidate validation, and one-vote-per-user enforcement.
- WebSocket clients connect to `/voteUpdates`.
- When a vote or election change happens, the server broadcasts an update event.
- Frontend JavaScript receives the update and refreshes dashboard data from `/api/dashboard-data`.

## System Design

### Main Modules

- **Authentication Module**: Handles login, logout, password verification, session creation, login rate limiting, and role-based redirection.
- **Registration Module**: Handles voter registration, validation, captcha checking, age verification, duplicate checks, and pending approval storage.
- **Admin Module**: Handles election management, candidate management, voter approval, settings, complaints, reports, and dashboard monitoring.
- **Voting Module**: Handles active election lookup, candidate validation, vote recording, receipt generation, and vote status updates.
- **Dashboard API Module**: Provides JSON data for admin and voter dashboards.
- **WebSocket Module**: Broadcasts vote and election update events to connected clients.
- **Security Module**: Provides CSRF protection, authentication filters, HTTP security headers, password hashing, and session controls.
- **Persistence Module**: Uses MySQL for relational election records and MongoDB for logs and semi-structured operational data.

### Data Storage Design

MySQL stores the core relational data:

- `users`: voter and admin accounts, profile details, role, and voting status.
- `elections`: election title, description, date, time range, and active/inactive status.
- `candidates`: candidate name, manifesto, and election mapping.
- `votes`: voter vote record, selected candidate, receipt ID, and vote timestamp.

MongoDB stores operational and audit data:

- `pending_users`: voter registrations waiting for admin approval.
- `election_logs`: election actions and system events.
- `vote_events`: vote event audit entries.
- `active_sessions`: active logged-in sessions.
- `security_audit_logs`: login, registration, and voting security events.
- `complaints`: user grievances and admin replies.

## Requirements

### Software Requirements

- Java 8 or later
- Maven
- Apache Tomcat or Maven Tomcat plugin
- MySQL Server
- MongoDB Server
- Web browser

### Maven Dependencies

- `javax.servlet-api`
- `javax.servlet.jsp-api`
- `javax.websocket-api`
- `jstl`
- `mysql-connector-java`
- `mongodb-driver-sync`
- `gson`
- `jbcrypt`

### Database Requirements

The application expects a MySQL database named:

```text
online_voting_system
```

Default database configuration is stored in:

```text
src/main/resources/application.properties
```

Default values:

```properties
mysql.url=jdbc:mysql://localhost:3306/online_voting_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
mysql.username=root
mysql.password=

mongodb.uri=mongodb://localhost:27017
mongodb.database=online_voting_system
```

## Architecture

The project uses a layered MVC-style architecture.

```text
Browser
  |
  | HTTP requests, form submissions, WebSocket connection
  v
JSP Views + JavaScript
  |
  v
Servlet Controllers
  |
  v
Service Layer
  |
  v
DAO Layer
  |
  +--> MySQL: users, elections, candidates, votes
  |
  +--> MongoDB: logs, pending users, sessions, complaints
```

### Layer Responsibilities

- **View Layer**: JSP files under `src/main/webapp/WEB-INF/views` display pages for users and admins.
- **Controller Layer**: Servlet classes under `controller` process requests and responses.
- **Service Layer**: `VotingService` contains main voting business rules.
- **DAO Layer**: DAO classes handle database operations.
- **Model Layer**: Model classes represent users, elections, candidates, vote stats, and dashboard stats.
- **Utility Layer**: Utility classes handle validation, password hashing, CSRF tokens, JSON, sessions, database connections, MongoDB connections, schema migration, and settings.
- **WebSocket Layer**: Sends live update events to connected dashboards.

## Implementation

### Authentication and Authorization

- `LoginServlet` validates email and password.
- Passwords are hashed with BCrypt using `PasswordUtil`.
- Older SHA-256 password hashes can be migrated after successful login.
- `LoginRateLimiter` blocks repeated failed login attempts.
- Sessions are regenerated after login to reduce session fixation risk.
- `AuthFilter` protects `/admin/*` and `/user/*` routes.
- Admin users are redirected to `/admin/dashboard`.
- Voters are redirected to `/user/dashboard`.

### Registration and Approval

- `RegisterServlet` validates voter registration details.
- Voters must be at least 18 years old.
- Duplicate email and voter ID values are checked in MySQL and MongoDB pending records.
- Captcha validation is required during registration.
- If ID verification is enabled, the voter is stored in MongoDB `pending_users`.
- `AdminActionServlet` approves pending users by moving them into the MySQL `users` table.

### Election Management

- `ElectionServlet` allows admins to create, update, activate, and deactivate elections.
- Election details include title, description, date, start time, end time, and status.
- Election changes are logged in MongoDB and broadcast through WebSocket updates.

### Candidate Management

- `CandidateServlet` allows admins to add, edit, and delete candidates.
- Each candidate belongs to an election.
- Candidate actions trigger live dashboard refresh events.

### Vote Casting

- `VoteServlet` accepts a candidate selection from a logged-in voter.
- `VotingService.castVote` checks that:
  - An active election exists.
  - The voter has not already voted.
  - The selected candidate belongs to the active election.
- The vote is inserted into the `votes` table.
- The user is marked as `has_voted = true`.
- A receipt ID is generated without exposing the selected candidate.
- Vote events are logged in MongoDB.
- A `vote_cast` event is broadcast through WebSockets.

### Live Results

- `VoteUpdatesEndpoint` exposes the WebSocket endpoint:

```text
/voteUpdates
```

- `VoteUpdateBroadcaster` sends event messages to connected sessions.
- Dashboard JavaScript listens for these events.
- Updated chart and dashboard data is fetched from:

```text
/api/dashboard-data
```

### Reports

`AdminReportServlet` provides:

- Final results report as PDF.
- Voter roll as CSV.
- Security audit report as XML.

### Security Implementation

- BCrypt password hashing.
- Login rate limiting.
- CSRF token validation for POST requests.
- Role-based route protection.
- Session timeout of 20 minutes.
- HTTP-only session cookies.
- Security headers:
  - `X-Content-Type-Options`
  - `X-Frame-Options`
  - `Referrer-Policy`
  - `Permissions-Policy`
  - `Cache-Control`
- Server-side validation for registration, profile, election, candidate, complaint, and login inputs.
- MongoDB audit logging for security-sensitive events.

## Results

The completed application provides:

- A working voter registration and approval process.
- Secure admin and voter login.
- Admin election and candidate management.
- Active election voting from the voter dashboard.
- One-vote-per-user enforcement.
- Real-time vote result updates using WebSockets.
- Dashboard charts and activity data using JSON APIs.
- User grievance submission and admin resolution.
- Exportable election reports.
- MySQL-backed structured records and MongoDB-backed audit history.

## Setup and Run

### 1. Open the Project Folder

```powershell
cd "C:\Users\Yogi\OneDrive\Desktop\Real-Time-Online-Voting-System"
```

### 2. Start Required Services

Make sure MySQL Server and MongoDB Server are running.

### 3. Create the MySQL Database

Run:

```powershell
mysql -u root -p < database.sql
```

If your MySQL root user has no password:

```powershell
mysql -u root < database.sql
```

### 4. Build the Project

```powershell
mvn clean package
```

The WAR file is generated at:

```text
target/real-time-online-voting-system.war
```

### 5. Run the Project

```powershell
mvn tomcat7:run
```

Open:

```text
http://localhost:8082/
```

To stop the server:

```text
Ctrl + C
```

## Default Login Details

### Admin

```text
Email: admin@voting.local
Password: Admin@123
```

### Demo Voter

```text
Email: voter@voting.local
Password: User@123
```

These accounts are created automatically during application startup if they do not already exist.

## File Structure

```text
Real-Time-Online-Voting-System/
|
|-- README.md
|-- pom.xml
|-- database.sql
|-- cookies.txt
|
|-- src/
|   |-- main/
|       |-- java/
|       |   |-- com/
|       |       |-- voting/
|       |           |-- config/
|       |           |   |-- AppConfig.java
|       |           |   |-- AppContextListener.java
|       |           |
|       |           |-- controller/
|       |           |   |-- AdminActionServlet.java
|       |           |   |-- AdminDashboardServlet.java
|       |           |   |-- AdminReportServlet.java
|       |           |   |-- CandidateServlet.java
|       |           |   |-- ElectionServlet.java
|       |           |   |-- HomeServlet.java
|       |           |   |-- LoginServlet.java
|       |           |   |-- LogoutServlet.java
|       |           |   |-- RegisterServlet.java
|       |           |   |-- ResultsApiServlet.java
|       |           |   |-- UserActionServlet.java
|       |           |   |-- UserDashboardServlet.java
|       |           |   |-- VoteServlet.java
|       |           |
|       |           |-- dao/
|       |           |   |-- CandidateDao.java
|       |           |   |-- ElectionDao.java
|       |           |   |-- MongoLogDao.java
|       |           |   |-- UserDao.java
|       |           |   |-- VoteDao.java
|       |           |
|       |           |-- filter/
|       |           |   |-- AuthFilter.java
|       |           |   |-- CsrfFilter.java
|       |           |   |-- SecurityHeadersFilter.java
|       |           |
|       |           |-- model/
|       |           |   |-- Candidate.java
|       |           |   |-- DashboardStats.java
|       |           |   |-- Election.java
|       |           |   |-- User.java
|       |           |   |-- VoteStat.java
|       |           |
|       |           |-- service/
|       |           |   |-- VotingService.java
|       |           |
|       |           |-- util/
|       |           |   |-- CsrfUtil.java
|       |           |   |-- DatabaseUtil.java
|       |           |   |-- InputValidator.java
|       |           |   |-- JsonUtil.java
|       |           |   |-- LoginRateLimiter.java
|       |           |   |-- MongoUtil.java
|       |           |   |-- PasswordUtil.java
|       |           |   |-- SchemaMigrator.java
|       |           |   |-- SessionUtil.java
|       |           |   |-- SystemSettings.java
|       |           |
|       |           |-- websocket/
|       |               |-- VoteUpdateBroadcaster.java
|       |               |-- VoteUpdatesEndpoint.java
|       |
|       |-- resources/
|       |   |-- application.properties
|       |
|       |-- webapp/
|           |-- index.jsp
|           |-- assets/
|           |   |-- css/
|           |   |   |-- style.css
|           |   |-- js/
|           |       |-- admin.js
|           |       |-- theme.js
|           |       |-- user.js
|           |
|           |-- uploads/
|           |   |-- profile/
|           |
|           |-- WEB-INF/
|               |-- web.xml
|               |-- views/
|                   |-- admin-dashboard.jsp
|                   |-- index.jsp
|                   |-- login.jsp
|                   |-- register.jsp
|                   |-- user-dashboard.jsp
|
|-- target/
    |-- real-time-online-voting-system.war
```

## Common Problems

### Port 8082 Is Already in Use

Change the port in `pom.xml`:

```xml
<port>8082</port>
```

For example:

```xml
<port>8083</port>
```

Then run:

```powershell
mvn tomcat7:run
```

### MySQL Login Error

Edit:

```text
src/main/resources/application.properties
```

Make sure `mysql.username` and `mysql.password` match your local MySQL setup.

### Database Does Not Exist

Run:

```powershell
mysql -u root -p < database.sql
```

### MongoDB Connection Error

Start MongoDB locally, then restart the application.

## Useful Commands

Clean and build:

```powershell
mvn clean package
```

Run locally:

```powershell
mvn tomcat7:run
```

Build without running tests:

```powershell
mvn clean package -DskipTests
```
