<%@ page import="com.voting.model.DashboardStats" %>
<%@ page import="com.voting.model.Election" %>
<%@ page import="com.voting.model.Candidate" %>
<%@ page import="com.voting.model.User" %>
<%@ page import="com.voting.model.VoteStat" %>
<%@ page import="com.voting.util.SystemSettings" %>
<%@ page import="com.voting.util.CsrfUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.TreeSet" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    DashboardStats stats = (DashboardStats) request.getAttribute("stats");
    List<User> users = (List<User>) request.getAttribute("users");
    List<User> voters = (List<User>) request.getAttribute("voters");
    List<Candidate> candidates = (List<Candidate>) request.getAttribute("candidates");
    List<Election> elections = (List<Election>) request.getAttribute("elections");
    Election latestElection = (Election) request.getAttribute("latestElection");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> nominations = (List<Map<String, Object>>) request.getAttribute("nominations");
    @SuppressWarnings("unchecked")
    Map<Integer, List<VoteStat>> announcedResults = (Map<Integer, List<VoteStat>>) request.getAttribute("announcedResults");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> pendingUsers = (List<Map<String, Object>>) request.getAttribute("pendingUsers");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> auditLogs = (List<Map<String, Object>>) request.getAttribute("auditLogs");
    @SuppressWarnings("unchecked")
    Map<String, Object> suspiciousActivity = (Map<String, Object>) request.getAttribute("suspiciousActivity");
    @SuppressWarnings("unchecked")
    Map<String, Object> integritySummary = (Map<String, Object>) request.getAttribute("integritySummary");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> auditorNotes = (List<Map<String, Object>>) request.getAttribute("auditorNotes");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> officerReadiness = (List<Map<String, Object>>) request.getAttribute("officerReadiness");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> centerTurnout = (List<Map<String, Object>>) request.getAttribute("centerTurnout");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> officerNotes = (List<Map<String, Object>>) request.getAttribute("officerNotes");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> officerIncidents = (List<Map<String, Object>>) request.getAttribute("officerIncidents");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> officerDryRuns = (List<Map<String, Object>>) request.getAttribute("officerDryRuns");
    Election officerSelectedElection = (Election) request.getAttribute("officerSelectedElection");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> openComplaints = (List<Map<String, Object>>) request.getAttribute("openComplaints");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> allComplaints = (List<Map<String, Object>>) request.getAttribute("allComplaints");
    
    // Convert to JSON for Chart.js
    String demographicsJson = new com.google.gson.Gson().toJson(request.getAttribute("demographics"));
    String voteTrendsJson = new com.google.gson.Gson().toJson(request.getAttribute("voteTrends"));
    
    User admin = com.voting.util.SessionUtil.getLoggedInUser(request);
    String adminRole = admin != null && admin.getRole() != null ? admin.getRole() : "";
    boolean isSuperAdmin = "super_admin".equalsIgnoreCase(adminRole);
    boolean isAdminRole = "admin".equalsIgnoreCase(adminRole);
    boolean isElectionOfficer = "election_officer".equalsIgnoreCase(adminRole);
    boolean isAuditor = "auditor".equalsIgnoreCase(adminRole);
    boolean canManageElections = isSuperAdmin || isAdminRole || isElectionOfficer;
    boolean canManageVoters = isSuperAdmin || isAdminRole;
    boolean canManageSettings = isSuperAdmin;
    boolean canReviewNominations = canManageElections;
    boolean canManageEligibility = canManageElections;
    if (suspiciousActivity == null) suspiciousActivity = new java.util.LinkedHashMap<String, Object>();
    if (integritySummary == null) integritySummary = new java.util.LinkedHashMap<String, Object>();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard | State Election Commission</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/img/sec-logo.svg">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=21">
    <style>
        body[data-admin-role="election_officer"] [data-section="users"],
        body[data-admin-role="election_officer"] [data-section="approvals"],
        body[data-admin-role="election_officer"] [data-section="announcements"],
        body[data-admin-role="election_officer"] [data-section="auditor-review"],
        body[data-admin-role="election_officer"] [data-section="audit"],
        body[data-admin-role="election_officer"] [data-section="support"],
        body[data-admin-role="election_officer"] [data-section="settings"],
        body[data-admin-role="admin"] [data-section="settings"],
        body[data-admin-role="auditor"] [data-section="elections"],
        body[data-admin-role="auditor"] [data-section="candidates"],
        body[data-admin-role="auditor"] [data-section="nominations"],
        body[data-admin-role="auditor"] [data-section="eligibility"],
        body[data-admin-role="auditor"] [data-section="officer-ops"],
        body[data-admin-role="auditor"] [data-section="users"],
        body[data-admin-role="auditor"] [data-section="approvals"],
        body[data-admin-role="auditor"] [data-section="support"],
        body[data-admin-role="auditor"] [data-section="settings"] {
            display: none !important;
        }
        body[data-admin-role="auditor"] #section-announcements form,
        body[data-admin-role="auditor"] #section-announcements button,
        body[data-admin-role="auditor"] .edit-candidate-btn,
        body[data-admin-role="auditor"] .edit-election-btn {
            display: none !important;
        }
        .mini-metric {
            border: 1px solid var(--line);
            border-radius: 8px;
            padding: 14px;
            background: rgba(255,255,255,0.04);
        }
        .mini-metric span {
            display: block;
            color: var(--text-muted);
            font-size: 0.82rem;
            margin-bottom: 8px;
        }
        .mini-metric strong {
            font-size: 1.35rem;
        }
    </style>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body class="dashboard-body" data-context-path="${pageContext.request.contextPath}" data-role="admin" data-admin-role="<%= adminRole %>">
<div class="dashboard-shell">
    <aside class="sidebar">
        <div class="sidebar-header">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2.5">
                <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z"/>
                <path d="M9 12L11 14L15 10"/>
            </svg>
            <h2>SEC PORTAL</h2>
        </div>
        <nav id="sidebarNav">
            <a href="#overview" class="active" data-section="overview">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
                Overview
            </a>
            <a href="#elections" data-section="elections">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                Elections
            </a>
            <a href="#candidates" data-section="candidates">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
                Candidates
            </a>
            <a href="#nominations" data-section="nominations">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"></path><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"></path></svg>
                Nominations
            </a>
            <a href="#eligibility" data-section="eligibility">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 11l3 3L22 4"></path><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"></path></svg>
                Eligibility
            </a>
            <a href="#officer-ops" data-section="officer-ops">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12h18"></path><path d="M12 3v18"></path><path d="M5 5l14 14"></path><path d="M19 5L5 19"></path></svg>
                Officer Ops
            </a>
            <a href="#users" data-section="users">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                Users
            </a>
            <a href="#approvals" data-section="approvals">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                Voter Approvals
            </a>
            <a href="#announcements" data-section="announcements">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                Announcements
            </a>
            <a href="#audit" data-section="audit">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
                Audit Logs
            </a>
            <a href="#auditor-review" data-section="auditor-review">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 12l2 2 4-4"></path><path d="M21 12c-1.4 4-4.5 7-9 7s-7.6-3-9-7c1.4-4 4.5-7 9-7s7.6 3 9 7z"></path></svg>
                Auditor Review
            </a>
            <a href="#support" data-section="support">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path></svg>
                Complaints
            </a>
            <a href="#reports" data-section="reports">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
                Reports
            </a>
            <a href="#settings" data-section="settings">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
                Settings
            </a>
        </nav>
        <div class="sidebar-footer">
            <a href="${pageContext.request.contextPath}/logout" class="logout-link">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                <span>Logout Account</span>
            </a>
        </div>
    </aside>

    <main class="dashboard-content">
        <header class="section-header">
            <div style="display: flex; justify-content: space-between; align-items: flex-end;">
                <div>
                    <p>Live Administration Center</p>
                    <h1>SEC Control Panel</h1>
                </div>
                <div style="display: flex; gap: 16px; align-items: center;">
                    <div id="currentDate" class="date-display"></div>
                    <button class="btn btn-outline" id="themeToggle">🌙 Mode</button>
                    
                    <%-- Admin Profile Widget --%>
                    <div class="user-profile-widget" style="display: flex; align-items: center; gap: 12px; background: rgba(255,255,255,0.05); padding: 6px 14px; border-radius: 50px; border: 1px solid var(--line);">
                        <div style="text-align: right; line-height: 1.2;">
                            <div style="font-weight: 700; font-size: 0.85rem;"><%= admin.getName() %></div>
                            <div class="muted" style="font-size: 0.7rem;">Admin ID: #<%= admin.getId() %></div>
                        </div>
                        <% if (admin.getProfilePhotoPath() != null && !admin.getProfilePhotoPath().isEmpty()) { %>
                            <img src="${pageContext.request.contextPath}<%= admin.getProfilePhotoPath() %>" style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover; border: 2px solid var(--primary);">
                        <% } else { %>
                            <div style="width: 32px; height: 32px; border-radius: 50%; background: var(--primary); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.8rem; color: #fff;">
                                <%= admin.getName().substring(0,1).toUpperCase() %>
                            </div>
                        <% } %>
                    </div>
                </div>
            </div>
        </header>

        <% String error = (String) request.getSession().getAttribute("errorMessage");
           if (error != null) {
               request.getSession().removeAttribute("errorMessage");
        %>
        <div class="alert error">
            <span><%= error %></span>
        </div>
        <% } %>

        <% String success = (String) request.getSession().getAttribute("successMessage");
           if (success != null) {
               request.getSession().removeAttribute("successMessage");
        %>
        <div class="alert success">
            <span><%= success %></span>
        </div>
        <% } %>

        <!-- OVERVIEW SECTION -->
        <div id="section-overview" class="tab-section active" data-section="overview">
            <section class="stats-grid">
                <article class="stat-card">
                    <span>Total Registered Voters</span>
                    <strong id="totalUsers"><%= stats.getTotalUsers() %></strong>
                </article>
                <article class="stat-card">
                    <span>Current Election Votes</span>
                    <strong id="totalVotes"><%= stats.getTotalVotes() %></strong>
                </article>
                <article class="stat-card">
                    <span>Current Election Candidates</span>
                    <strong id="totalCandidates"><%= stats.getTotalCandidates() %></strong>
                </article>
                <article class="stat-card">
                    <span>System Status</span>
                    <strong id="electionStatus" style="color: var(--secondary)"><%= stats.getElectionStatus() %></strong>
                </article>
            </section>

            <div class="panel">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
                    <h3>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                        Voter Turnout Real-Time
                    </h3>
                    <span id="turnoutPercentage" style="font-size: 1.5rem; font-weight: 800; color: var(--primary)">0%</span>
                </div>
                <div style="background: rgba(255,255,255,0.05); height: 12px; border-radius: 6px; overflow: hidden;">
                    <div id="turnoutFill" style="width: 0%; height: 100%; background: linear-gradient(to right, var(--primary), var(--secondary)); transition: width 1s ease;"></div>
                </div>
                <p style="color: var(--text-muted); margin-top: 16px; font-size: 0.9rem;">Automatic updates based on verified blockchain transactions.</p>
            </div>

            <div style="display: grid; grid-template-columns: 1.5fr 1fr; gap: 40px; margin-top: 40px;">
                <div class="panel">
                    <h3>Voting Activity Trend</h3>
                    <div style="height: 300px;">
                        <canvas id="votingActivityChart"></canvas>
                    </div>
                </div>
                <div class="panel">
                    <h3>Voter Demographics</h3>
                    <div style="height: 300px;">
                        <canvas id="demographicsChart"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- ELECTIONS SECTION -->
        <div id="section-elections" class="tab-section" data-section="elections">
            <div style="display: grid; grid-template-columns: 1fr 2fr; gap: 40px;">
                <div class="panel">
                    <h3>Create New Election</h3>
                    <form method="post" action="${pageContext.request.contextPath}/admin/elections" style="display: flex; flex-direction: column; gap: 16px;">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="create">
                        <input type="text" name="title" placeholder="Election Title" required style="width: 100%; padding: 12px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 10px; color: #fff;">
                        <textarea name="description" placeholder="Description" required style="width: 100%; padding: 12px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 10px; color: #fff; height: 100px;"></textarea>
                        <input type="date" name="electionDate" required style="width: 100%; padding: 12px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 10px; color: #fff;">
                        <div style="display: flex; gap: 12px;">
                            <input type="time" name="startTime" required style="flex: 1; padding: 12px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 10px; color: #fff;">
                            <input type="time" name="endTime" required style="flex: 1; padding: 12px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 10px; color: #fff;">
                        </div>
                        <label class="muted" style="font-size:0.8rem;">Nomination Window</label>
                        <div style="display: flex; gap: 12px;">
                            <input type="datetime-local" name="nominationStartsAt" style="flex: 1; padding: 12px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 10px; color: #fff;">
                            <input type="datetime-local" name="nominationEndsAt" style="flex: 1; padding: 12px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 10px; color: #fff;">
                        </div>
                        <button class="btn btn-primary" type="submit">Deploy Election</button>
                    </form>
                </div>

                <div class="panel">
                    <h3>Active & Scheduled Elections</h3>
                    <div style="display: grid; gap: 16px;">
                        <% if (elections != null) { for (Election el : elections) { %>
                        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--line); padding: 20px; border-radius: 16px; display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <h4 style="font-size: 1.1rem; margin-bottom: 4px;"><%= el.getTitle() %></h4>
                                <p style="color: var(--text-muted); font-size: 0.85rem;"><%= el.getElectionDate() %> • <%= el.getStartTime() %> - <%= el.getEndTime() %></p>
                            </div>
                            <div style="display: flex; gap: 12px; align-items: center;">
                                <% if ("COMPLETED".equalsIgnoreCase(el.getStatus())) { %>
                                    <button class="btn btn-outline" disabled style="color: var(--text-muted); opacity: 0.5; cursor: not-allowed; border-color: var(--line);">Completed</button>
                                <% } else { %>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/elections">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="action" value="toggle">
                                        <input type="hidden" name="electionId" value="<%= el.getId() %>">
                                        <input type="hidden" name="status" value="<%= "ACTIVE".equals(el.getStatus()) ? "INACTIVE" : "ACTIVE" %>">
                                        <button class="btn btn-outline" type="submit" style="color: <%= "ACTIVE".equals(el.getStatus()) ? "var(--danger)" : "var(--secondary)" %>">
                                            <%= "ACTIVE".equals(el.getStatus()) ? "Pause / Halt" : "Start / Resume" %>
                                        </button>
                                    </form>
                                <% } %>
                                <button class="btn btn-outline edit-election-btn"
                                        data-id="<%= el.getId() %>"
                                        data-title="<%= el.getTitle().replace("\"", "&quot;") %>"
                                        data-description="<%= el.getDescription().replace("\"", "&quot;").replace("\n", "&#10;") %>"
                                        data-date="<%= el.getElectionDate() %>"
                                        data-start="<%= el.getStartTime() %>"
                                        data-end="<%= el.getEndTime() %>"
                                        data-nomination-start="<%= el.getNominationStartsAt() != null ? el.getNominationStartsAt().toString().replace(" ", "T").substring(0,16) : "" %>"
                                        data-nomination-end="<%= el.getNominationEndsAt() != null ? el.getNominationEndsAt().toString().replace(" ", "T").substring(0,16) : "" %>"
                                        data-status="<%= el.getStatus() %>">Edit</button>
                            </div>
                            <form method="post" action="${pageContext.request.contextPath}/admin/elections" style="display:flex;gap:8px;align-items:center;margin-top:12px;flex-wrap:wrap;">
                                <%= CsrfUtil.hiddenInput(request) %>
                                <input type="hidden" name="action" value="extend">
                                <input type="hidden" name="electionId" value="<%= el.getId() %>">
                                <input type="time" name="endTime" required style="padding:8px;background:#0f172a;border:1px solid var(--line);border-radius:8px;color:#fff;">
                                <input type="text" name="reason" placeholder="Extension reason" required style="padding:8px;background:rgba(0,0,0,0.2);border:1px solid var(--line);border-radius:8px;color:#fff;">
                                <button class="btn btn-outline btn-sm">Extend</button>
                            </form>
                        </div>
                        <% }} %>
                    </div>
                </div>
            </div>
        </div>

        <!-- CANDIDATES SECTION -->
        <div id="section-candidates" class="tab-section" data-section="candidates">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 40px; margin-bottom: 40px;">
                <div class="panel">
                    <h3>Add New Candidate</h3>
                    <form method="post" action="${pageContext.request.contextPath}/admin/candidates"
                          enctype="multipart/form-data"
                          style="display: flex; flex-direction: column; gap: 16px;">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="add">

                        <%-- Photo upload preview --%>
                        <div style="display:flex;flex-direction:column;align-items:center;gap:12px;padding:16px;background:rgba(0,0,0,0.15);border:1px dashed var(--line);border-radius:12px;">
                            <img id="candidatePhotoPreview" src="" alt=""
                                 style="width:96px;height:96px;border-radius:50%;object-fit:cover;border:2px solid var(--primary);display:none;">
                            <div id="candidatePhotoPlaceholder"
                                 style="width:96px;height:96px;border-radius:50%;background:rgba(99,102,241,0.15);display:flex;align-items:center;justify-content:center;font-size:2rem;">📷</div>
                            <label style="cursor:pointer;">
                                <span class="btn btn-outline" style="font-size:0.85rem;">Upload Photo</span>
                                <input type="file" name="photo" accept="image/*" style="display:none;" id="candidatePhotoInput"
                                       onchange="previewCandidatePhoto(this)">
                            </label>
                            <small style="color:var(--text-muted);font-size:0.75rem;">JPG/PNG/WEBP · Max 5MB</small>
                        </div>

                        <input type="text" name="name" placeholder="Full Name" required
                               style="width:100%;padding:12px;background:rgba(0,0,0,0.2);border:1px solid var(--line);border-radius:10px;color:#fff;">
                        <textarea name="manifesto" placeholder="Campaign Manifesto" required
                                  style="width:100%;padding:12px;background:rgba(0,0,0,0.2);border:1px solid var(--line);border-radius:10px;color:#fff;height:100px;"></textarea>
                        <input type="number" name="ballotOrder" min="0" value="0" placeholder="Ballot Order"
                               style="width:100%;padding:12px;background:rgba(0,0,0,0.2);border:1px solid var(--line);border-radius:10px;color:#fff;">
                        <select name="electionId" required
                                style="width:100%;padding:12px;background:#0f172a;border:1px solid var(--line);border-radius:10px;color:#fff;">
                            <% if (elections != null) { for (Election election : elections) { %>
                            <option value="<%= election.getId() %>"><%= election.getTitle() %></option>
                            <% }} %>
                        </select>
                        <button class="btn btn-primary" type="submit">Register Candidate</button>
                    </form>
                </div>
                <div class="panel">
                    <h3>Real-Time Live Standings</h3>
                    <div style="height: 300px;">
                        <canvas id="adminResultsChart"></canvas>
                    </div>
                </div>
            </div>

            <div class="panel">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
                    <h3>Candidate Directory</h3>
                    <input type="text" id="candidateSearch" placeholder="Filter by name..." style="padding: 8px 16px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 8px; color: #fff;">
                </div>
                <div class="table-wrapper">
                    <table id="candidateTable">
                        <thead>
                            <tr>
                                <th>Photo</th>
                                <th>Name</th>
                                <th>Election</th>
                                <th>Manifesto Snippet</th>
                                <th>Order</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (candidates != null) { for (Candidate c : candidates) { %>
                            <tr>
                                <td>
                                    <% if (c.getPhotoPath() != null && !c.getPhotoPath().isEmpty()) { %>
                                    <img src="${pageContext.request.contextPath}<%= c.getPhotoPath() %>"
                                         alt="<%= c.getName() %>"
                                         style="width:48px;height:48px;border-radius:50%;object-fit:cover;border:2px solid var(--line);">
                                    <% } else { %>
                                    <div style="width:48px;height:48px;border-radius:50%;background:rgba(99,102,241,0.2);display:flex;align-items:center;justify-content:center;font-size:1.3rem;">👤</div>
                                    <% } %>
                                </td>
                                <td><strong><%= c.getName() %></strong></td>
                                <td><%= c.getElectionId() %></td>
                                <td><%= c.getManifesto().length() > 50 ? c.getManifesto().substring(0, 50) + "..." : c.getManifesto() %></td>
                                <td><%= c.getBallotOrder() %></td>
                                <td style="display:flex;gap:8px;align-items:center;">
                                    <%-- Edit button --%>
                                    <% String candidatePhotoUrl = (c.getPhotoPath() != null && !c.getPhotoPath().isEmpty()) ? request.getContextPath() + c.getPhotoPath() : ""; %>
                                    <button class="btn btn-outline btn-sm edit-candidate-btn"
                                            data-id="<%= c.getId() %>"
                                            data-name="<%= c.getName().replace("\"", "&quot;") %>"
                                            data-manifesto="<%= c.getManifesto().replace("\"", "&quot;").replace("\n", "&#10;") %>"
                                            data-election-id="<%= c.getElectionId() %>"
                                            data-ballot-order="<%= c.getBallotOrder() %>"
                                            data-photo="<%= candidatePhotoUrl %>"
                                            style="color:var(--primary);">Edit</button>
                                    <%-- Delete button --%>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/candidates" style="margin:0;" enctype="multipart/form-data">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="candidateId" value="<%= c.getId() %>">
                                        <input type="hidden" name="electionId" value="<%= c.getElectionId() %>">
                                        <button class="btn btn-outline btn-sm" style="color:var(--danger);">Remove</button>
                                    </form>
                                </td>
                            </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="section-nominations" class="tab-section" data-section="nominations">
            <div class="panel">
                <h3>Candidate Nomination Workflow</h3>
                <p class="muted" style="margin-bottom: 20px;">Users can apply as candidates. Approving a request creates the candidate record for that election.</p>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr><th>Applicant</th><th>Election</th><th>Manifesto</th><th>Documents</th><th>Status</th><th>Action</th></tr>
                        </thead>
                        <tbody>
                        <% if (nominations != null && !nominations.isEmpty()) { for (Map<String, Object> n : nominations) { 
                            String nStatus = n.get("status") != null ? n.get("status").toString() : "PENDING";
                        %>
                            <tr>
                                <td><strong><%= n.get("userName") %></strong><br><small class="muted"><%= n.get("email") %></small></td>
                                <td><%= n.get("electionTitle") %></td>
                                <td><%= n.get("manifesto") %></td>
                                <td>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:grid;gap:6px;min-width:180px;">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="action" value="verifyNominationDocs">
                                        <input type="hidden" name="nominationId" value="<%= n.get("id") %>">
                                        <select name="documentStatus" style="padding:6px;border-radius:8px;border:1px solid var(--line);background:#0f172a;color:#fff;">
                                            <option value="PENDING" <%= "PENDING".equals(String.valueOf(n.get("documentStatus"))) ? "selected" : "" %>>Pending</option>
                                            <option value="VERIFIED" <%= "VERIFIED".equals(String.valueOf(n.get("documentStatus"))) ? "selected" : "" %>>Verified</option>
                                            <option value="REJECTED" <%= "REJECTED".equals(String.valueOf(n.get("documentStatus"))) ? "selected" : "" %>>Rejected</option>
                                        </select>
                                        <input name="documentNote" value="<%= n.get("documentNote") != null ? n.get("documentNote") : "" %>" placeholder="Document note" style="padding:6px;border-radius:8px;border:1px solid var(--line);background:rgba(0,0,0,0.2);color:#fff;">
                                        <button class="btn btn-outline btn-sm">Save Docs</button>
                                    </form>
                                </td>
                                <td><span class="status-pill <%= "APPROVED".equals(nStatus) ? "active" : "inactive" %>"><%= nStatus %></span></td>
                                <td>
                                    <% if ("PENDING".equals(nStatus)) { %>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:flex;gap:8px;flex-wrap:wrap;">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="action" value="reviewNomination">
                                        <input type="hidden" name="nominationId" value="<%= n.get("id") %>">
                                        <input type="text" name="adminNote" placeholder="Optional note" style="padding:8px;border-radius:8px;border:1px solid var(--line);background:rgba(0,0,0,0.2);color:#fff;">
                                        <button class="btn btn-primary btn-sm" name="decision" value="APPROVED">Approve</button>
                                        <button class="btn btn-outline btn-sm" name="decision" value="REJECTED" style="color:var(--danger);">Reject</button>
                                    </form>
                                    <% } else { %>
                                    <span class="muted"><%= n.get("adminNote") != null ? n.get("adminNote") : "Reviewed" %></span>
                                    <% } %>
                                </td>
                            </tr>
                        <% }} else { %>
                            <tr><td colspan="6" style="text-align:center;color:var(--text-muted);padding:28px;">No candidate nominations yet.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="section-eligibility" class="tab-section" data-section="eligibility">
            <div class="panel">
                <h3>Election-Specific Voter Eligibility</h3>
                <p class="muted" style="margin-bottom: 20px;">If an election has no selected voters here, all verified voters remain eligible. Adding one voter creates a restricted eligibility list for that election.</p>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr><th>Voter</th><th>Election</th><th>Action</th></tr>
                        </thead>
                        <tbody>
                        <% if (voters != null && !voters.isEmpty() && elections != null && !elections.isEmpty()) { for (User voter : voters) { %>
                            <tr>
                                <td><strong><%= voter.getName() %></strong><br><small class="muted"><%= voter.getVoterIdNumber() %> · <%= voter.getCity() %></small></td>
                                <td>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="action" value="setEligibility">
                                        <input type="hidden" name="userId" value="<%= voter.getId() %>">
                                        <select name="electionId" style="padding:8px;border-radius:8px;border:1px solid var(--line);background:#0f172a;color:#fff;">
                                            <% for (Election election : elections) { %>
                                            <option value="<%= election.getId() %>"><%= election.getTitle() %></option>
                                            <% } %>
                                        </select>
                                </td>
                                <td>
                                        <button class="btn btn-primary btn-sm" name="eligible" value="true">Allow</button>
                                        <button class="btn btn-outline btn-sm" name="eligible" value="false" style="color:var(--danger);">Remove</button>
                                    </form>
                                </td>
                            </tr>
                        <% }} else { %>
                            <tr><td colspan="3" style="text-align:center;color:var(--text-muted);padding:28px;">Create elections and approve voters before managing eligibility.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="section-officer-ops" class="tab-section" data-section="officer-ops">
            <section class="stats-grid">
                <article class="stat-card">
                    <span>Officer Election</span>
                    <strong style="font-size:1.05rem;"><%= officerSelectedElection != null ? officerSelectedElection.getTitle() : "None" %></strong>
                </article>
                <article class="stat-card">
                    <span>Pre-result Status</span>
                    <strong style="color:<%= officerSelectedElection != null && officerSelectedElection.isReadyForResults() ? "var(--secondary)" : "var(--accent)" %>"><%= officerSelectedElection != null && officerSelectedElection.isReadyForResults() ? "READY" : "PENDING" %></strong>
                </article>
                <article class="stat-card">
                    <span>Incident Reports</span>
                    <strong><%= officerIncidents != null ? officerIncidents.size() : 0 %></strong>
                </article>
                <article class="stat-card">
                    <span>Dry Runs</span>
                    <strong><%= officerDryRuns != null ? officerDryRuns.size() : 0 %></strong>
                </article>
            </section>

            <div class="panel">
                <h3>Election Readiness Checklist</h3>
                <div class="table-wrapper">
                    <table>
                        <thead><tr><th>Election</th><th>Candidates</th><th>Eligible Voters</th><th>Pending Nominations</th><th>Time</th><th>Nomination Window</th><th>Ready</th></tr></thead>
                        <tbody>
                        <% if (officerReadiness != null && !officerReadiness.isEmpty()) { for (Map<String, Object> row : officerReadiness) { %>
                            <tr>
                                <td>#<%= row.get("electionId") %> <%= row.get("title") %></td>
                                <td><%= row.get("candidateCount") %></td>
                                <td><%= row.get("eligibleCount") %></td>
                                <td><%= row.get("pendingNominations") %></td>
                                <td><%= Boolean.TRUE.equals(row.get("timeWindowValid")) ? "OK" : "Review" %></td>
                                <td><%= Boolean.TRUE.equals(row.get("nominationWindowValid")) ? "OK" : "Review" %></td>
                                <td><strong style="color:<%= Boolean.TRUE.equals(row.get("ready")) ? "var(--secondary)" : "var(--accent)" %>"><%= Boolean.TRUE.equals(row.get("ready")) ? "READY" : "CHECK" %></strong></td>
                            </tr>
                        <% }} else { %>
                            <tr><td colspan="7" style="text-align:center;color:var(--text-muted);padding:24px;">No elections available.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>

            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:24px;margin-top:24px;">
                <div class="panel">
                    <h3>Bulk Eligibility Import</h3>
                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:grid;gap:12px;margin-top:16px;">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="bulkEligibility">
                        <select name="electionId" required style="padding:10px;background:#0f172a;border:1px solid var(--line);border-radius:8px;color:#fff;">
                            <% if (elections != null) { for (Election election : elections) { %>
                            <option value="<%= election.getId() %>"><%= election.getTitle() %></option>
                            <% }} %>
                        </select>
                        <textarea name="voterIdentifiers" required placeholder="Paste voter IDs or emails, one per line or comma separated" style="min-height:110px;padding:10px;background:rgba(0,0,0,0.2);border:1px solid var(--line);border-radius:8px;color:var(--text-main);"></textarea>
                        <button class="btn btn-primary">Import Eligibility</button>
                    </form>
                </div>

                <div class="panel">
                    <h3>Live Election Control</h3>
                    <div style="display:grid;gap:12px;margin-top:16px;">
                        <% if (elections != null) { for (Election election : elections) { if (!"COMPLETED".equalsIgnoreCase(election.getStatus())) { %>
                        <form method="post" action="${pageContext.request.contextPath}/admin/elections" style="display:flex;gap:8px;align-items:center;justify-content:space-between;border:1px solid var(--line);border-radius:8px;padding:10px;">
                            <%= CsrfUtil.hiddenInput(request) %>
                            <input type="hidden" name="action" value="toggle">
                            <input type="hidden" name="electionId" value="<%= election.getId() %>">
                            <input type="hidden" name="status" value="<%= "ACTIVE".equals(election.getStatus()) ? "INACTIVE" : "ACTIVE" %>">
                            <span><%= election.getTitle() %></span>
                            <button class="btn btn-outline btn-sm"><%= "ACTIVE".equals(election.getStatus()) ? "Pause / Halt" : "Start / Resume" %></button>
                        </form>
                        <% }}} %>
                    </div>
                </div>

                <div class="panel">
                    <h3>Dry Run & Pre-result Validation</h3>
                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:grid;gap:12px;margin-top:16px;">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="runDryRun">
                        <select name="electionId" required style="padding:10px;background:#0f172a;border:1px solid var(--line);border-radius:8px;color:#fff;">
                            <% if (elections != null) { for (Election election : elections) { %>
                            <option value="<%= election.getId() %>"><%= election.getTitle() %></option>
                            <% }} %>
                        </select>
                        <button class="btn btn-outline">Run Ballot Dry Run</button>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:grid;gap:12px;margin-top:12px;">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="markReadyForResults">
                        <select name="electionId" required style="padding:10px;background:#0f172a;border:1px solid var(--line);border-radius:8px;color:#fff;">
                            <% if (elections != null) { for (Election election : elections) { %>
                            <option value="<%= election.getId() %>"><%= election.getTitle() %></option>
                            <% }} %>
                        </select>
                        <button class="btn btn-primary">Validate For Results</button>
                    </form>
                </div>
            </div>

            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:24px;margin-top:24px;">
                <div class="panel">
                    <h3>Center-wise Turnout</h3>
                    <div class="table-wrapper">
                        <table>
                            <thead><tr><th>Center</th><th>Eligible</th><th>Votes</th><th>Turnout</th></tr></thead>
                            <tbody>
                            <% if (centerTurnout != null && !centerTurnout.isEmpty()) { for (Map<String, Object> row : centerTurnout) { %>
                                <tr><td><%= row.get("centerName") %></td><td><%= row.get("eligibleCount") %></td><td><%= row.get("votesCast") %></td><td><%= row.get("turnoutPercentage") %>%</td></tr>
                            <% }} else { %>
                                <tr><td colspan="4" style="text-align:center;color:var(--text-muted);padding:20px;">No center turnout yet.</td></tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="panel">
                    <h3>Incident Reporting</h3>
                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:grid;gap:10px;margin-top:14px;">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="addIncident">
                        <select name="electionId" required style="padding:10px;background:#0f172a;border:1px solid var(--line);border-radius:8px;color:#fff;">
                            <% if (elections != null) { for (Election election : elections) { %>
                            <option value="<%= election.getId() %>"><%= election.getTitle() %></option>
                            <% }} %>
                        </select>
                        <select name="severity" style="padding:10px;background:#0f172a;border:1px solid var(--line);border-radius:8px;color:#fff;"><option>LOW</option><option>MEDIUM</option><option>HIGH</option></select>
                        <input name="category" required placeholder="Camera, OTP, turnout, device..." style="padding:10px;background:rgba(0,0,0,0.2);border:1px solid var(--line);border-radius:8px;color:#fff;">
                        <textarea name="description" required placeholder="Incident details" style="min-height:80px;padding:10px;background:rgba(0,0,0,0.2);border:1px solid var(--line);border-radius:8px;color:#fff;"></textarea>
                        <button class="btn btn-primary">Save Incident</button>
                    </form>
                </div>
            </div>

            <div class="panel" style="margin-top:24px;">
                <h3>Officer Task Notes</h3>
                <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:flex;gap:10px;flex-wrap:wrap;margin:16px 0;">
                    <%= CsrfUtil.hiddenInput(request) %>
                    <input type="hidden" name="action" value="addOfficerNote">
                    <select name="electionId" required style="padding:10px;background:#0f172a;border:1px solid var(--line);border-radius:8px;color:#fff;">
                        <% if (elections != null) { for (Election election : elections) { %>
                        <option value="<%= election.getId() %>"><%= election.getTitle() %></option>
                        <% }} %>
                    </select>
                    <input name="officerNote" required placeholder="Task note..." style="flex:1;min-width:240px;padding:10px;background:rgba(0,0,0,0.2);border:1px solid var(--line);border-radius:8px;color:#fff;">
                    <button class="btn btn-primary">Add Note</button>
                </form>
                <div class="table-wrapper">
                    <table>
                        <thead><tr><th>Time</th><th>Election</th><th>Officer</th><th>Note</th></tr></thead>
                        <tbody>
                        <% if (officerNotes != null && !officerNotes.isEmpty()) { for (Map<String, Object> row : officerNotes) { %>
                            <tr><td><%= row.get("created_at") %></td><td><%= row.get("election_title") %></td><td><%= row.get("officer_name") %></td><td><%= row.get("note") %></td></tr>
                        <% }} else { %>
                            <tr><td colspan="4" style="text-align:center;color:var(--text-muted);padding:20px;">No officer notes yet.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- USERS SECTION -->
        <div id="section-users" class="tab-section" data-section="users">
            <div class="panel">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; gap: 16px; flex-wrap: wrap;">
                    <h3>Voter Registry</h3>
                    <div style="display: flex; gap: 12px; flex-wrap: wrap; align-items: center;">
                        <input type="text" id="userSearch" placeholder="Search voters..." style="padding: 8px 16px; min-width: 220px; background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 8px; color: #fff;">
                        <select id="userRoleFilter" style="padding: 8px 12px; background: #0f172a; border: 1px solid var(--line); border-radius: 8px; color: #fff;">
                            <option value="">All roles</option>
                            <option value="user">Users</option>
                            <option value="super_admin">Super Admins</option>
                            <option value="admin">Admins</option>
                            <option value="election_officer">Election Officers</option>
                            <option value="auditor">Auditors</option>
                        </select>
                        <select id="userStatusFilter" style="padding: 8px 12px; background: #0f172a; border: 1px solid var(--line); border-radius: 8px; color: #fff;">
                            <option value="">All statuses</option>
                            <option value="VOTED">Voted</option>
                            <option value="PENDING">Pending</option>
                            <option value="SYSTEM">System</option>
                        </select>
                        <select id="userCenterFilter" style="padding: 8px 12px; background: #0f172a; border: 1px solid var(--line); border-radius: 8px; color: #fff; max-width: 220px;">
                            <option value="">All centers</option>
                            <% if (users != null) {
                                Set<String> centers = new TreeSet<String>();
                                for (User centerUser : users) {
                                    if (centerUser.getElectionCenter() != null && !centerUser.getElectionCenter().trim().isEmpty()) {
                                        centers.add(centerUser.getElectionCenter().trim());
                                    }
                                }
                                for (String center : centers) {
                            %>
                            <option value="<%= center.replace("\"", "&quot;") %>"><%= center %></option>
                            <%  }
                               } %>
                        </select>
                        <button class="btn btn-outline btn-sm" type="button" id="resetUserFilters">Reset</button>
                        <button class="btn btn-outline" onclick="exportTableToCSV('usersTable', 'voters.csv')">Export CSV</button>
                    </div>
                </div>
                <p class="muted" id="userFilterCount" style="margin: -8px 0 16px 0; font-size: 0.85rem;"></p>
                <div class="table-wrapper">
                    <table id="usersTable">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Voter ID</th>
                                <th>Center</th>
                                <th>Status</th>
                                <th>Role</th>
                            </tr>
                        </thead>
                        <tbody id="usersTableBody">
                            <% if (users != null) { for (User u : users) { %>
                            <tr data-role="<%= u.getRole() != null ? u.getRole() : "" %>"
                                data-status="<%= !"user".equalsIgnoreCase(u.getRole()) ? "SYSTEM" : (u.isHasVoted() ? "VOTED" : "PENDING") %>"
                                data-center="<%= u.getElectionCenter() != null ? u.getElectionCenter().replace("\"", "&quot;") : "" %>">
                                <td><%= u.getName() %><br><small style="color: var(--text-muted)"><%= u.getEmail() %></small></td>
                                <td><%= u.getVoterIdNumber() != null ? u.getVoterIdNumber() : "-" %></td>
                                <td><%= u.getElectionCenter() != null ? u.getElectionCenter() : "-" %></td>
                                <td>
                                    <% if ("admin".equalsIgnoreCase(u.getRole())) { %>
                                        <span style="color: var(--accent)">SYSTEM</span>
                                    <% } else { %>
                                        <span style="color: <%= u.isHasVoted() ? "var(--secondary)" : "var(--text-muted)" %>">
                                            <%= u.isHasVoted() ? "VOTED" : "PENDING" %>
                                        </span>
                                    <% } %>
                                </td>
                                <td>
                                    <% if (com.voting.util.SessionUtil.isSuperAdmin(request) && u.getId() != admin.getId()) { %>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:flex;gap:8px;align-items:center;">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="action" value="updateRole">
                                        <input type="hidden" name="userId" value="<%= u.getId() %>">
                                        <select name="role" style="padding:6px;border-radius:8px;border:1px solid var(--line);background:#0f172a;color:#fff;">
                                            <option value="user" <%= "user".equals(u.getRole()) ? "selected" : "" %>>User</option>
                                            <option value="auditor" <%= "auditor".equals(u.getRole()) ? "selected" : "" %>>Auditor</option>
                                            <option value="election_officer" <%= "election_officer".equals(u.getRole()) ? "selected" : "" %>>Election Officer</option>
                                            <option value="admin" <%= "admin".equals(u.getRole()) ? "selected" : "" %>>Admin</option>
                                            <option value="super_admin" <%= "super_admin".equals(u.getRole()) ? "selected" : "" %>>Super Admin</option>
                                        </select>
                                        <button class="btn btn-sm btn-outline">Save</button>
                                    </form>
                                    <% } else { %>
                                    <%= u.getRole() %>
                                    <% } %>
                                </td>
                            </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="section-approvals" class="tab-section" data-section="approvals">
            <div class="panel">
                <h3>Voter Verification Queue</h3>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr><th>Name</th><th>Voter ID</th><th>Submitted</th><th>Action</th></tr>
                        </thead>
                        <tbody>
                            <% if (pendingUsers != null && !pendingUsers.isEmpty()) { for (Map<String, Object> p : pendingUsers) {
                                String pId = p.get("_id") != null ? p.get("_id").toString() : "";
                                String pTimestamp = p.get("timestamp") != null ? p.get("timestamp").toString() : "";
                                String pTimestampShort = pTimestamp.length() >= 10 ? pTimestamp.substring(0, 10) : pTimestamp;
                            %>
                            <tr>
                                <td><%= p.get("name") %><br><small><%= p.get("email") %></small></td>
                                <td><%= p.get("voter_id_number") %></td>
                                <td><%= pTimestampShort %></td>
                                <td>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display: flex; gap: 8px;">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="objectId" value="<%= pId %>">
                                        <button name="action" value="approveUser" class="btn btn-primary btn-sm">Verify</button>
                                        <button name="action" value="rejectUser" class="btn btn-outline btn-sm" style="color: var(--danger)">Reject</button>
                                    </form>
                                </td>
                            </tr>
                            <% }} else { %>
                            <tr><td colspan="4" style="text-align: center; padding: 40px; color: var(--text-muted);">Queue is empty. All voters verified.</td></tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="section-announcements" class="tab-section" data-section="announcements">
            <div class="panel">
                <h3>Election Announcements</h3>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr><th>Election</th><th>Date</th><th>Winner</th><th>Candidate Vote Count</th><th>Status</th><th>Action</th></tr>
                        </thead>
                        <tbody>
                            <% boolean hasAnnouncements = false;
                               if (elections != null) { for (Election el : elections) { if (el.isResultsAnnounced()) {
                                   hasAnnouncements = true;
                                   List<VoteStat> resultRows = announcedResults != null ? announcedResults.get(el.getId()) : null;
                                   VoteStat winner = resultRows != null && !resultRows.isEmpty() ? resultRows.get(0) : null;
                            %>
                            <tr>
                                <td><strong><%= el.getTitle() %></strong></td>
                                <td><%= el.getElectionDate() %></td>
                                <td>
                                    <% if (winner != null && winner.getTotalVotes() > 0) { %>
                                    <strong style="color: var(--secondary);"><%= winner.getCandidateName() %></strong>
                                    <div class="muted" style="font-size: 0.75rem;"><%= winner.getTotalVotes() %> votes</div>
                                    <% } else { %>
                                    <span class="muted">No votes recorded</span>
                                    <% } %>
                                </td>
                                <td>
                                    <% if (resultRows != null && !resultRows.isEmpty()) { %>
                                    <div style="display: grid; gap: 6px;">
                                        <% for (VoteStat row : resultRows) { %>
                                        <div style="display: flex; justify-content: space-between; gap: 16px; font-size: 0.85rem;">
                                            <span><%= row.getCandidateName() %></span>
                                            <strong><%= row.getTotalVotes() %></strong>
                                        </div>
                                        <% } %>
                                    </div>
                                    <% } else { %>
                                    <span class="muted">No candidates</span>
                                    <% } %>
                                </td>
                                <td><span style="color: var(--secondary)">ANNOUNCED</span></td>
                                <td>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/actions">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="action" value="announceResults">
                                        <input type="hidden" name="electionId" value="<%= el.getId() %>">
                                        <input type="hidden" name="announced" value="false">
                                        <button class="btn btn-outline btn-sm" style="color: var(--danger)">Withdraw</button>
                                    </form>
                                </td>
                            </tr>
                            <% }}}
                               if (!hasAnnouncements) { %>
                            <tr><td colspan="6" style="text-align:center; color: var(--text-muted); padding: 28px;">No election results announced yet.</td></tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="section-auditor-review" class="tab-section" data-section="auditor-review">
            <section class="stats-grid">
                <article class="stat-card">
                    <span>Anomaly Score</span>
                    <strong style="color:<%= "High".equals(String.valueOf(suspiciousActivity.get("riskLevel"))) ? "var(--danger)" : ("Medium".equals(String.valueOf(suspiciousActivity.get("riskLevel"))) ? "var(--accent)" : "var(--secondary)") %>"><%= suspiciousActivity.getOrDefault("anomalyScore", 0) %>/100</strong>
                    <small class="muted"><%= suspiciousActivity.getOrDefault("riskLevel", "Low") %> risk</small>
                </article>
                <article class="stat-card">
                    <span>Failed Logins</span>
                    <strong><%= suspiciousActivity.getOrDefault("failedLogins", 0) %></strong>
                    <small class="muted">recent audit window</small>
                </article>
                <article class="stat-card">
                    <span>Invalid OTP Attempts</span>
                    <strong><%= suspiciousActivity.getOrDefault("invalidOtps", 0) %></strong>
                    <small class="muted">recent audit window</small>
                </article>
                <article class="stat-card">
                    <span>Integrity Status</span>
                    <strong style="color:<%= Boolean.TRUE.equals(integritySummary.get("verified")) ? "var(--secondary)" : "var(--danger)" %>"><%= Boolean.TRUE.equals(integritySummary.get("verified")) ? "VERIFIED" : "REVIEW" %></strong>
                    <small class="muted"><%= integritySummary.getOrDefault("totalVotes", 0) %> vote records</small>
                </article>
            </section>

            <div style="display:grid; grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr); gap:24px;">
                <div class="panel">
                    <h3>Suspicious Activity Panel</h3>
                    <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap:16px; margin-top:18px;">
                        <div class="mini-metric"><span>Denied / CSRF Events</span><strong><%= suspiciousActivity.getOrDefault("deniedAccess", 0) %></strong></div>
                        <div class="mini-metric"><span>Account Lockouts</span><strong><%= suspiciousActivity.getOrDefault("lockedAccounts", 0) %></strong></div>
                        <div class="mini-metric"><span>Busiest IP</span><strong style="font-size:1rem;"><%= suspiciousActivity.getOrDefault("busiestIp", "-") %></strong></div>
                        <div class="mini-metric"><span>Logs Scanned</span><strong><%= suspiciousActivity.getOrDefault("logWindow", 0) %></strong></div>
                    </div>
                </div>

                <div class="panel">
                    <h3>Integrity Verification</h3>
                    <div style="display:grid; gap:10px; margin-top:18px;">
                        <div style="display:flex; justify-content:space-between;"><span>Missing receipts</span><strong><%= integritySummary.getOrDefault("missingReceipts", 0) %></strong></div>
                        <div style="display:flex; justify-content:space-between;"><span>Missing signatures</span><strong><%= integritySummary.getOrDefault("missingSignatures", 0) %></strong></div>
                        <div style="display:flex; justify-content:space-between;"><span>Duplicate receipts</span><strong><%= integritySummary.getOrDefault("duplicateReceipts", 0) %></strong></div>
                    </div>
                </div>
            </div>

            <div class="panel" style="margin-top:24px;">
                <h3>Read-Only Election Timeline</h3>
                <div class="table-wrapper">
                    <table>
                        <thead><tr><th>Election</th><th>Date</th><th>Voting Window</th><th>Status</th><th>Results</th></tr></thead>
                        <tbody>
                            <% if (elections != null) { for (Election el : elections) { %>
                            <tr>
                                <td>#<%= el.getId() %> <%= el.getTitle() %></td>
                                <td><%= el.getElectionDate() %></td>
                                <td><%= el.getStartTime() %> - <%= el.getEndTime() %></td>
                                <td><span class="status-pill"><%= el.getStatus() %></span></td>
                                <td><%= el.isResultsAnnounced() ? "Announced" : "Pending" %></td>
                            </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>
            </div>

            <div style="display:grid; grid-template-columns: minmax(0, 1fr) minmax(280px, 0.9fr); gap:24px; margin-top:24px;">
                <div class="panel">
                    <h3>Auditor Notes</h3>
                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display:grid; gap:12px; margin:16px 0 22px;">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="addAuditorNote">
                        <textarea name="auditorNote" placeholder="Record review observations or evidence references..." required style="min-height:92px; background:rgba(0,0,0,0.2); border:1px solid var(--line); border-radius:8px; color:var(--text-main); padding:12px;"></textarea>
                        <button class="btn btn-primary" type="submit" style="justify-self:start;">Save Note</button>
                    </form>
                    <div style="display:grid; gap:12px;">
                        <% if (auditorNotes != null && !auditorNotes.isEmpty()) { for (Map<String, Object> note : auditorNotes) { %>
                        <div style="border:1px solid var(--line); border-radius:8px; padding:12px;">
                            <div class="muted" style="font-size:0.8rem;"><%= note.get("timestamp") %> by <%= note.get("auditor_name") %></div>
                            <div style="margin-top:6px;"><%= note.get("note") %></div>
                        </div>
                        <% }} else { %>
                        <p class="muted">No auditor notes recorded yet.</p>
                        <% } %>
                    </div>
                </div>

                <div class="panel">
                    <h3>Compliance & Evidence</h3>
                    <div style="display:grid; gap:14px; margin-top:18px;">
                        <a href="${pageContext.request.contextPath}/admin/reports?type=complianceReport" class="btn btn-outline" style="text-decoration:none; justify-content:center;">Download Compliance Report</a>
                        <a href="${pageContext.request.contextPath}/admin/reports?type=evidenceCsv" class="btn btn-outline" style="text-decoration:none; justify-content:center;">Download Evidence CSV</a>
                        <a href="${pageContext.request.contextPath}/admin/reports?type=securityAudit" class="btn btn-outline" style="text-decoration:none; justify-content:center;">Download Audit XML</a>
                    </div>
                </div>
            </div>
        </div>

        <div id="section-audit" class="tab-section" data-section="audit">
            <div class="panel">
                <h3>System Immutable Logs</h3>
                <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap:12px; margin:18px 0;">
                    <input id="auditSearch" type="search" placeholder="Search event, actor, IP..." style="background:rgba(0,0,0,0.2); border:1px solid var(--line); border-radius:8px; color:var(--text-main); padding:10px;">
                    <input id="auditActorFilter" type="search" placeholder="Actor only" style="background:rgba(0,0,0,0.2); border:1px solid var(--line); border-radius:8px; color:var(--text-main); padding:10px;">
                    <input id="auditIpFilter" type="search" placeholder="IP only" style="background:rgba(0,0,0,0.2); border:1px solid var(--line); border-radius:8px; color:var(--text-main); padding:10px;">
                    <button id="resetAuditFilters" class="btn btn-outline" type="button">Reset Filters</button>
                </div>
                <p id="auditFilterCount" class="muted" style="margin:0 0 12px;"></p>
                <div class="table-wrapper">
                    <table id="auditTable">
                        <thead>
                            <tr><th>Time</th><th>Event</th><th>Actor</th><th>IP</th></tr>
                        </thead>
                        <tbody>
                            <% if (auditLogs != null) { for (Map<String, Object> log : auditLogs) {
                                String logTs = log.get("timestamp") != null ? log.get("timestamp").toString() : "";
                                String logTsDisplay = logTs.length() >= 19 ? logTs.substring(0,19).replace("T", " ") : logTs;
                                String logAction = log.get("action") != null ? log.get("action").toString() : "system_event";
                                String logDetails = log.get("details") != null ? log.get("details").toString() : "";
                                String logActor = log.get("actor") != null ? log.get("actor").toString() : "system";
                                String logIp = log.get("ip_address") != null ? log.get("ip_address").toString() : "-";
                            %>
                            <tr data-actor="<%= logActor.toLowerCase() %>" data-ip="<%= logIp.toLowerCase() %>" data-event="<%= logAction.toLowerCase() %>">
                                <td style="font-family: monospace; font-size: 0.85rem;"><%= logTsDisplay %></td>
                                <td><%= logAction %><%= logDetails.isEmpty() ? "" : ": " + logDetails %></td>
                                <td><%= logActor %></td>
                                <td><%= logIp %></td>
                            </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="section-support" class="tab-section" data-section="support">
            <div class="panel">
                <h3>User Grievance Reports</h3>
                <div class="table-wrapper">
                    <table>
                        <thead>
                            <tr><th>Ticket</th><th>User ID</th><th>Message</th><th>Action</th></tr>
                        </thead>
                        <tbody>
                            <% if (allComplaints != null) { for (Map<String, Object> t : allComplaints) {
                                String tId = t.get("_id") != null ? t.get("_id").toString() : "";
                                String tShortId = tId.length() > 18 ? tId.substring(18) : tId;
                                String tStatus = t.get("status") != null ? t.get("status").toString() : "Open";
                            %>
                            <tr>
                                <td>#<%= tShortId %></td>
                                <td><%= t.get("user_id") %></td>
                                <td><%= t.get("issue") %></td>
                                <td>
                                    <% if (!"Resolved".equals(tStatus)) { %>
                                    <form method="post" action="${pageContext.request.contextPath}/admin/actions" style="display: flex; flex-direction: column; gap: 8px;">
                                        <%= CsrfUtil.hiddenInput(request) %>
                                        <input type="hidden" name="action" value="resolveComplaint">
                                        <input type="hidden" name="objectId" value="<%= tId %>">
                                        <input type="hidden" name="returnTo" value="support">
                                        <textarea name="adminReply" placeholder="Resolution note..." style="background: rgba(0,0,0,0.2); border: 1px solid var(--line); border-radius: 8px; color: #fff; padding: 8px;"></textarea>
                                        <button class="btn btn-primary btn-sm">Resolve</button>
                                    </form>
                                    <% } else { %>
                                    <span style="color: var(--secondary)">RESOLVED</span>
                                    <% } %>
                                </td>
                            </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <div id="section-reports" class="tab-section" data-section="reports">
            <div class="panel">
                <h3>Export Official Documentation</h3>
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 24px; margin-top: 24px;">
                    <a href="${pageContext.request.contextPath}/admin/reports?type=finalResults" class="panel" style="text-decoration: none; text-align: center; border-color: var(--primary);">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline></svg>
                        <h4 style="margin-top: 16px;">Final Results (PDF)</h4>
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/reports?type=voterRoll" class="panel" style="text-decoration: none; text-align: center;">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--secondary)" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle></svg>
                        <h4 style="margin-top: 16px;">Voter Roll (CSV)</h4>
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/reports?type=resultsCsv" class="panel" style="text-decoration: none; text-align: center;">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2"><path d="M3 3v18h18"></path><path d="M7 15l4-4 3 3 5-7"></path></svg>
                        <h4 style="margin-top: 16px;">Results (CSV)</h4>
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/reports?type=securityAudit" class="panel" style="text-decoration: none; text-align: center;">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect><path d="M7 11V7a5 5 0 0 1 10 0v4"></path></svg>
                        <h4 style="margin-top: 16px;">Security Audit (XML)</h4>
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/reports?type=complianceReport" class="panel" style="text-decoration: none; text-align: center;">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--secondary)" stroke-width="2"><path d="M9 12l2 2 4-4"></path><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
                        <h4 style="margin-top: 16px;">Compliance Report (PDF)</h4>
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/reports?type=evidenceCsv" class="panel" style="text-decoration: none; text-align: center;">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                        <h4 style="margin-top: 16px;">Evidence Download (CSV)</h4>
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/reports?type=centerTurnout" class="panel" style="text-decoration: none; text-align: center;">
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--secondary)" stroke-width="2"><path d="M3 3v18h18"></path><path d="M8 17V9"></path><path d="M13 17V5"></path><path d="M18 17v-6"></path></svg>
                        <h4 style="margin-top: 16px;">Center Turnout (CSV)</h4>
                    </a>
                </div>
            </div>
        </div>

        <div id="section-settings" class="tab-section" data-section="settings">
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 40px;">
                <div class="panel">
                    <h3>System Configuration</h3>
                    <form method="post" action="${pageContext.request.contextPath}/admin/actions">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="updateSettings">
                        <div style="display: flex; flex-direction: column; gap: 24px; margin-top: 20px;">
                            <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--line); padding-bottom: 16px;">
                                <div>
                                    <strong>Allow New Registrations</strong>
                                    <p style="color: var(--text-muted); font-size: 0.85rem;">Users can register for the portal.</p>
                                </div>
                                <input type="checkbox" name="allowReg" style="width: 20px; height: 20px;" <%= SystemSettings.allowRegistrations ? "checked" : "" %>>
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--line); padding-bottom: 16px;">
                                <div>
                                    <strong>Require ID Verification</strong>
                                    <p style="color: var(--text-muted); font-size: 0.85rem;">Mandate manual admin approval for voters.</p>
                                </div>
                                <input type="checkbox" name="requireId" style="width: 20px; height: 20px;" <%= SystemSettings.requireIdVerification ? "checked" : "" %>>
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <strong>Maintenance Mode</strong>
                                    <p style="color: var(--text-muted); font-size: 0.85rem;">Lock out all non-admin users.</p>
                                </div>
                                <input type="checkbox" name="maintenance" style="width: 20px; height: 20px;" <%= SystemSettings.maintenanceMode ? "checked" : "" %>>
                            </div>
                            <button class="btn btn-primary" type="submit" style="align-self: flex-start; margin-top: 16px;">Save System Policy</button>
                        </div>
                    </form>
                </div>

                <div class="panel">
                    <h3>Security & Access</h3>
                    <div style="display: flex; flex-direction: column; gap: 24px; margin-top: 20px;">
                        <div style="border-bottom: 1px solid var(--line); padding-bottom: 24px;">
                            <strong>Reset Account Lockouts</strong>
                            <p style="color: var(--text-muted); font-size: 0.85rem; margin: 8px 0 16px 0;">Unlock all accounts blocked due to multiple failed login attempts.</p>
                            <form method="post" action="${pageContext.request.contextPath}/admin/dashboard">
                                <%= CsrfUtil.hiddenInput(request) %>
                                <input type="hidden" name="action" value="unlockAll">
                                <button class="btn btn-outline" type="submit" style="color: var(--accent); border-color: var(--accent);">Emergency Unlock All</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </main>
</div>

<script>
    window.DEMOGRAPHICS_DATA = <%= demographicsJson %>;
    window.VOTE_TRENDS_DATA = <%= voteTrendsJson %>;
</script>
<script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/admin.js?v=19"></script>
<script>
function previewCandidatePhoto(input) {
    const preview = document.getElementById('candidatePhotoPreview');
    const placeholder = document.getElementById('candidatePhotoPlaceholder');
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = e => {
            preview.src = e.target.result;
            preview.style.display = 'block';
            placeholder.style.display = 'none';
        };
        reader.readAsDataURL(input.files[0]);
    }
}
</script>

<!-- ===== EDIT CANDIDATE MODAL ===== -->
<div id="editCandidateModal"
     style="display:none;position:fixed;inset:0;z-index:9999;background:rgba(0,0,0,0.7);backdrop-filter:blur(6px);align-items:center;justify-content:center;">
    <div style="background:#1e293b;border:1px solid rgba(255,255,255,0.1);border-radius:24px;padding:40px;width:100%;max-width:520px;position:relative;box-shadow:0 30px 80px rgba(0,0,0,0.5);">
        <button id="closeEditModal"
                style="position:absolute;top:16px;right:16px;background:transparent;border:none;color:#94a3b8;font-size:1.5rem;cursor:pointer;line-height:1;">&times;</button>
        <h3 style="font-size:1.4rem;font-weight:800;margin-bottom:24px;letter-spacing:-0.02em;">Edit Candidate</h3>

        <form id="editCandidateForm" method="post"
              action="${pageContext.request.contextPath}/admin/candidates"
              enctype="multipart/form-data"
              style="display:flex;flex-direction:column;gap:18px;">
            <%= CsrfUtil.hiddenInput(request) %>
            <input type="hidden" name="action" value="edit">
            <input type="hidden" name="candidateId" id="editCandidateId">

            <%-- Photo preview --%>
            <div style="display:flex;align-items:center;gap:20px;padding:16px;background:rgba(0,0,0,0.2);border:1px dashed rgba(255,255,255,0.1);border-radius:12px;">
                <div style="position:relative;">
                    <img id="editPhotoPreview" src="" alt=""
                         style="width:80px;height:80px;border-radius:50%;object-fit:cover;border:2px solid var(--primary);display:none;">
                    <div id="editPhotoInitial"
                         style="width:80px;height:80px;border-radius:50%;background:rgba(99,102,241,0.2);display:flex;align-items:center;justify-content:center;font-size:2rem;font-weight:bold;color:#818cf8;"></div>
                </div>
                <div>
                    <label style="cursor:pointer;">
                        <span class="btn btn-outline" style="font-size:0.85rem;">Change Photo</span>
                        <input type="file" name="photo" accept="image/*" style="display:none;"
                               onchange="previewEditPhoto(this)">
                    </label>
                    <p style="color:#64748b;font-size:0.78rem;margin-top:8px;">Leave blank to keep existing photo</p>
                </div>
            </div>

            <input type="text" name="name" id="editCandidateName" placeholder="Full Name" required
                   style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;font-size:1rem;">

            <textarea name="manifesto" id="editCandidateManifesto" placeholder="Campaign Manifesto" required
                      style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;height:110px;font-family:inherit;font-size:0.95rem;resize:vertical;"></textarea>

            <input type="number" name="ballotOrder" id="editCandidateBallotOrder" min="0" placeholder="Ballot Order"
                   style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;font-size:1rem;">

            <select name="electionId" id="editCandidateElection" required
                    style="width:100%;padding:13px 16px;background:#0f172a;border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;">
                <% if (elections != null) { for (Election el2 : elections) { %>
                <option value="<%= el2.getId() %>"><%= el2.getTitle() %></option>
                <% }} %>
            </select>

            <div style="display:flex;gap:12px;">
                <button type="submit" class="btn btn-primary" style="flex:1;justify-content:center;padding:13px;">Save Changes</button>
                <button type="button" id="cancelEditBtn" class="btn btn-outline" style="padding:13px 20px;">Cancel</button>
            </div>
        </form>
    </div>
</div>

<!-- ===== EDIT ELECTION MODAL ===== -->
<div id="editElectionModal"
     style="display:none;position:fixed;inset:0;z-index:9999;background:rgba(0,0,0,0.7);backdrop-filter:blur(6px);align-items:center;justify-content:center;">
    <div style="background:#1e293b;border:1px solid rgba(255,255,255,0.1);border-radius:24px;padding:40px;width:100%;max-width:520px;position:relative;box-shadow:0 30px 80px rgba(0,0,0,0.5);">
        <button id="closeElectionModal"
                style="position:absolute;top:16px;right:16px;background:transparent;border:none;color:#94a3b8;font-size:1.5rem;cursor:pointer;line-height:1;">&times;</button>
        <h3 style="font-size:1.4rem;font-weight:800;margin-bottom:24px;letter-spacing:-0.02em;">Edit Election</h3>

        <form id="editElectionForm" method="post"
              action="${pageContext.request.contextPath}/admin/elections"
              style="display:flex;flex-direction:column;gap:18px;">
            <%= CsrfUtil.hiddenInput(request) %>
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="electionId" id="editElectionId">

            <input type="text" name="title" id="editElectionTitle" placeholder="Election Title" required
                   style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;font-size:1rem;">

            <textarea name="description" id="editElectionDescription" placeholder="Election Description" required
                      style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;height:110px;font-family:inherit;font-size:0.95rem;resize:vertical;"></textarea>

            <input type="date" name="electionDate" id="editElectionDate" required
                   style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;">

            <div style="display:flex;gap:12px;">
                <div style="flex:1;">
                    <label style="display:block;margin-bottom:8px;font-size:0.8rem;color:#94a3b8;">Start Time</label>
                    <input type="time" name="startTime" id="editElectionStart" required
                           style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;">
                </div>
                <div style="flex:1;">
                    <label style="display:block;margin-bottom:8px;font-size:0.8rem;color:#94a3b8;">End Time</label>
                    <input type="time" name="endTime" id="editElectionEnd" required
                           style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;">
                </div>
            </div>

            <div style="display:flex;gap:12px;">
                <div style="flex:1;">
                    <label style="display:block;margin-bottom:8px;font-size:0.8rem;color:#94a3b8;">Nomination Opens</label>
                    <input type="datetime-local" name="nominationStartsAt" id="editNominationStart"
                           style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;">
                </div>
                <div style="flex:1;">
                    <label style="display:block;margin-bottom:8px;font-size:0.8rem;color:#94a3b8;">Nomination Closes</label>
                    <input type="datetime-local" name="nominationEndsAt" id="editNominationEnd"
                           style="width:100%;padding:13px 16px;background:rgba(0,0,0,0.25);border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;">
                </div>
            </div>

            <select name="status" id="editElectionStatus" required
                    style="width:100%;padding:13px 16px;background:#0f172a;border:1px solid rgba(255,255,255,0.1);border-radius:12px;color:#fff;">
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
                <option value="COMPLETED">COMPLETED</option>
            </select>

            <div style="display:flex;gap:12px;">
                <button type="submit" class="btn btn-primary" style="flex:1;justify-content:center;padding:13px;">Save Changes</button>
                <button type="button" id="cancelElectionEditBtn" class="btn btn-outline" style="padding:13px 20px;">Cancel</button>
            </div>
        </form>
    </div>
</div>

<script>
// Edit modal wiring
(function () {
    const modal    = document.getElementById('editCandidateModal');
    const closeBtn = document.getElementById('closeEditModal');
    const cancelBtn= document.getElementById('cancelEditBtn');

    function openModal(btn) {
        const id         = btn.dataset.id;
        const name       = btn.dataset.name;
        const manifesto  = btn.dataset.manifesto.replace(/&#10;/g, '\n');
        const electionId = btn.dataset.electionId;
        const photo      = btn.dataset.photo;
        const ballotOrder = btn.dataset.ballotOrder || "0";

        document.getElementById('editCandidateId').value       = id;
        document.getElementById('editCandidateName').value     = name;
        document.getElementById('editCandidateManifesto').value= manifesto;
        document.getElementById('editCandidateBallotOrder').value = ballotOrder;

        const elSelect = document.getElementById('editCandidateElection');
        for (let i = 0; i < elSelect.options.length; i++) {
            elSelect.options[i].selected = (elSelect.options[i].value === electionId);
        }

        const photoPreview = document.getElementById('editPhotoPreview');
        const photoInitial = document.getElementById('editPhotoInitial');
        if (photo && photo.trim() !== '') {
            photoPreview.src = photo;
            photoPreview.style.display = 'block';
            photoInitial.style.display = 'none';
        } else {
            photoPreview.style.display = 'none';
            photoInitial.style.display = 'flex';
            photoInitial.textContent   = (name || 'C')[0].toUpperCase();
        }

        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function closeModal() {
        modal.style.display = 'none';
        document.body.style.overflow = '';
    }

    document.querySelectorAll('.edit-candidate-btn').forEach(btn => {
        btn.addEventListener('click', () => openModal(btn));
    });

    closeBtn.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', e => { if (e.target === modal) closeModal(); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });
}());

// Election Edit Modal Wiring
(function () {
    const modal    = document.getElementById('editElectionModal');
    const closeBtn = document.getElementById('closeElectionModal');
    const cancelBtn= document.getElementById('cancelElectionEditBtn');

    function openModal(btn) {
        document.getElementById('editElectionId').value          = btn.dataset.id;
        document.getElementById('editElectionTitle').value       = btn.dataset.title;
        document.getElementById('editElectionDescription').value = btn.dataset.description.replace(/&#10;/g, '\n');
        document.getElementById('editElectionDate').value        = btn.dataset.date;
        
        // Ensure time format is HH:mm for the time input
        const startTime = btn.dataset.start;
        const endTime = btn.dataset.end;
        document.getElementById('editElectionStart').value       = startTime ? startTime.substring(0, 5) : "";
        document.getElementById('editElectionEnd').value         = endTime ? endTime.substring(0, 5) : "";
        document.getElementById('editNominationStart').value     = btn.dataset.nominationStart || "";
        document.getElementById('editNominationEnd').value       = btn.dataset.nominationEnd || "";
        
        const statusSelect = document.getElementById('editElectionStatus');
        statusSelect.value = btn.dataset.status;
        
        // If already completed, lock the status to prevent restarting
        if (btn.dataset.status && btn.dataset.status.toUpperCase() === 'COMPLETED') {
            statusSelect.style.opacity = '0.5';
            statusSelect.style.pointerEvents = 'none';
        } else {
            statusSelect.style.opacity = '1';
            statusSelect.style.pointerEvents = 'auto';
        }

        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function closeModal() {
        modal.style.display = 'none';
        document.body.style.overflow = '';
    }

    document.querySelectorAll('.edit-election-btn').forEach(btn => {
        btn.addEventListener('click', () => openModal(btn));
    });

    closeBtn.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', e => { if (e.target === modal) closeModal(); });
    document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });
}());

function previewEditPhoto(input) {
    const preview = document.getElementById('editPhotoPreview');
    const initial = document.getElementById('editPhotoInitial');
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = e => {
            preview.src = e.target.result;
            preview.style.display = 'block';
            initial.style.display = 'none';
        };
        reader.readAsDataURL(input.files[0]);
    }
}
</script>
</body>
</html>
