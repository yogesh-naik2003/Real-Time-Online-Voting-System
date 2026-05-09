<%@ page import="com.voting.model.User" %>
<%@ page import="com.voting.model.Election" %>
<%@ page import="com.voting.model.Candidate" %>
<%@ page import="com.voting.model.VoteStat" %>

<%@ page import="com.voting.util.CsrfUtil" %>
<%@ page import="com.voting.util.WebUrlUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Collections" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    Map<String, Object> payload = (Map<String, Object>) request.getAttribute("payload");
    User user = (User) payload.get("user");
    Election activeElection = (Election) payload.get("activeElection");
    List<Candidate> candidates = (List<Candidate>) payload.get("candidates");
    List<Map<String, Object>> voteHistory = (List<Map<String, Object>>) payload.get("voteHistory");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> complaints = (List<Map<String, Object>>) payload.get("complaints");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> nominations = (List<Map<String, Object>>) payload.get("nominations");
    @SuppressWarnings("unchecked")
    Map<Integer, List<VoteStat>> announcedResults = (Map<Integer, List<VoteStat>>) payload.get("announcedResults");
    Map<String, Object> turnout = (Map<String, Object>) payload.get("turnout");
    boolean hasVoted = (Boolean) payload.get("hasVoted");
    boolean eligibleForActiveElection = payload.get("eligibleForActiveElection") == null || (Boolean) payload.get("eligibleForActiveElection");
    String profileVerificationStatus = (String) payload.get("profileVerificationStatus");
    String activeElectionEndsAt = (String) payload.get("activeElectionEndsAt");
    boolean resultsAnnounced = (Boolean) payload.get("resultsAnnounced");
    List<VoteStat> finalResults = (List<VoteStat>) payload.get("finalResults");
    Election latestElection = (Election) payload.get("latestElection");
    @SuppressWarnings("unchecked")
    List<String> notifications = (List<String>) payload.get("notifications");
    if (notifications == null) notifications = new ArrayList<String>();
    
    String voteReceipt = (String) session.getAttribute("voteReceipt");
    boolean isProfileComplete = user.isProfileComplete();
    if (voteReceipt != null) {
        session.removeAttribute("voteReceipt");
    }
    String voteError = (String) session.getAttribute("voteError");
    if (voteError != null) {
        session.removeAttribute("voteError");
    }
    String profileError = (String) session.getAttribute("profileError");
    if (profileError != null) {
        session.removeAttribute("profileError");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>User Dashboard | State Election Commission</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/img/sec-logo.svg">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=22">
</head>
<body class="dashboard-body" data-context-path="${pageContext.request.contextPath}" data-role="user">
<div class="dashboard-shell">
    <aside class="sidebar" style="display: flex; flex-direction: column;">
        <div>
            <span class="eyebrow">Voter Panel</span>
            <h2>SEC Portal</h2>
        </div>
        <nav id="sidebarNav">
            <a href="#voting-center" class="active">Voting Center</a>
            <a href="#announcements">Announcements</a>
            <a href="#vote-history">Vote History</a>
            <a href="#nomination">Candidate Nomination</a>
            <a href="#my-profile">My Profile</a>
            <a href="#grievance">Help & Grievance</a>
            <a href="#education">Voter Education</a>
            <a href="#rules">Rules</a>
        </nav>
        <div style="margin-top: auto; padding-top: 24px; border-top: 1px solid var(--line);">
            <a href="${pageContext.request.contextPath}/logout" style="display: flex; align-items: center; gap: 12px; color: var(--danger); font-weight: 600; padding: 12px 14px; border-radius: 14px; transition: background 0.2s;" onmouseover="this.style.background='rgba(239, 68, 68, 0.1)'" onmouseout="this.style.background='transparent'">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                Logout Account
            </a>
        </div>
    </aside>

    <main class="dashboard-main">
        <header class="dashboard-header">
            <div>
                <span class="eyebrow">Voter Interface</span>
                <h1>Election center</h1>
            </div>
            <div style="display: flex; gap: 16px; align-items: center;">
                <div class="notification-center">
                    <button class="btn btn-outline notification-button" id="notifBtn" type="button" aria-label="Open notifications" aria-expanded="false" aria-controls="notifDropdown">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                        <span id="notifBadge" class="notification-badge <%= notifications.isEmpty() ? "hidden" : "" %>"></span>
                    </button>
                    <div id="notifDropdown" class="panel notification-dropdown" role="dialog" aria-label="Notifications">
                        <h4>Notifications</h4>
                        <div id="notifList" class="notification-list">
                            <% for (String n : notifications) { %>
                            <div class="notification-item"><%= n %></div>
                            <% } if (notifications.isEmpty()) { %>
                            <p class="muted notification-empty">No new alerts.</p>
                            <% } %>
                        </div>
                    </div>
                </div>
                <button class="btn btn-outline user-theme-toggle" id="themeToggle" type="button">Light Mode</button>
                
                <%-- User Profile Widget --%>
                <div class="user-profile-widget" style="display: flex; align-items: center; gap: 12px; background: rgba(255,255,255,0.05); padding: 6px 14px; border-radius: 50px; border: 1px solid var(--line);">
                    <div style="text-align: right; line-height: 1.2;">
                        <div style="font-weight: 700; font-size: 0.85rem; color: #fff;"><%= user.getName() %></div>
                        <div class="muted" style="font-size: 0.7rem;">Voter ID: <%= user.getVoterIdNumber() %></div>
                    </div>
                    <% if (user.getProfilePhotoPath() != null && !user.getProfilePhotoPath().isEmpty()) { %>
                        <img src="${pageContext.request.contextPath}<%= user.getProfilePhotoPath() %>" style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover; border: 2px solid var(--primary);">
                    <% } else { %>
                        <div style="width: 32px; height: 32px; border-radius: 50%; background: var(--primary); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.8rem; color: #fff;">
                            <%= user.getName().substring(0,1).toUpperCase() %>
                        </div>
                    <% } %>
                </div>
            </div>
        </header>

        <% if (request.getParameter("voted") != null) { %>
        <div class="alert success" style="margin-bottom: 24px;">
            Vote submitted successfully.
            <% if (voteReceipt != null) { %>
            Receipt ID: <strong><%= voteReceipt %></strong>
            <% } %>
        </div>
        <% } %>
        <% if (request.getParameter("grievanceSubmitted") != null) { %>
        <div class="alert success" style="margin-bottom: 24px;">Your grievance has been submitted to the administration.</div>
        <% } %>
        <% if (request.getParameter("profileUpdated") != null) { %>
        <div class="alert success" style="margin-bottom: 24px;">Profile updated successfully.</div>
        <% } %>
        <% if (request.getParameter("nominationSubmitted") != null) { %>
        <div class="alert success" style="margin-bottom: 24px;">Candidate nomination submitted for admin review.</div>
        <% } %>
        <% if (voteError != null) { %>
        <div class="alert error" style="margin-bottom: 24px;"><%= voteError %></div>
        <% } %>
        <% if (profileError != null) { %>
        <div class="alert error" style="margin-bottom: 24px;"><%= profileError %></div>
        <% } %>

        <div id="section-voting-center" class="tab-section active">
            <% if (!isProfileComplete) { %>
            <div class="panel" style="text-align: center; padding: 40px; margin-bottom: 32px; border: 1px solid var(--danger); background: rgba(239, 68, 68, 0.05);">
                <div style="font-size: 3rem; margin-bottom: 16px;">⚠️</div>
                <h2 style="color: var(--danger); margin-bottom: 8px;">Incomplete Profile</h2>
                <p class="muted">You must complete your profile (Photo, Voter ID, Address, etc.) in the <strong>My Profile</strong> section before you can cast your vote.</p>
                <div style="margin-top: 20px;">
                    <a href="#my-profile" class="btn btn-primary" onclick="document.querySelector('a[href=\'#my-profile\']').click()">Go to My Profile</a>
                </div>
            </div>
            <% } else if (hasVoted) { %>
            <div class="panel" style="text-align: center; padding: 40px; margin-bottom: 32px; background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(16, 185, 129, 0.05)); border: 1px solid rgba(16, 185, 129, 0.2);">
                <div style="font-size: 4rem; margin-bottom: 16px;">🇮🇳</div>
                <h2 style="color: var(--secondary); margin-bottom: 8px;">You've Done Your Part!</h2>
                <p class="muted">Thank you for participating in the democratic process. Your vote has been securely recorded in the blockchain.</p>
                <div style="margin-top: 24px; display: flex; gap: 12px; justify-content: center;">
                    <span class="status-pill active">Verified Vote</span>
                    <span class="status-pill active" style="background: rgba(99, 102, 241, 0.1); color: var(--primary);">Blockchain Secured</span>
                </div>
            </div>
            <% } else if (!eligibleForActiveElection) { %>
            <div class="panel" style="text-align: center; padding: 40px; margin-bottom: 32px; border: 1px solid var(--accent); background: rgba(245, 158, 11, 0.08);">
                <h2 style="color: var(--accent); margin-bottom: 8px;">Not Eligible for This Election</h2>
                <p class="muted">Your account is verified, but this election has a restricted voter list and your voter ID is not currently included.</p>
            </div>
            <% } else if (activeElection == null && !resultsAnnounced) { %>
            <div class="panel" style="text-align: center; padding: 60px 20px; margin-bottom: 32px;">
                <div style="background: rgba(99, 102, 241, 0.1); width: 80px; height: 80px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 24px;">
                    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                </div>
                <h2>Waiting for active election</h2>
                <p class="muted">No active election is available right now. This page updates live when the admin starts one.</p>
            </div>
            <% } %>

            <section class="dashboard-grid user-grid">
                <%-- Active Election Status --%>
                <div class="panel">
                    <h3>Active Election</h3>
                    <div id="activeElectionCard">
                        <% if (activeElection != null) { %>
                        <h2><%= activeElection.getTitle() %></h2>
                        <p><%= activeElection.getDescription() %></p>
                        <span class="status-pill active" id="electionStatusPill"><%= activeElection.getStatus() %></span>
                        <div class="countdown-card">
                            <span class="muted">Voting closes in</span>
                            <strong id="electionCountdown" data-ends-at="<%= activeElectionEndsAt != null ? activeElectionEndsAt : "" %>">--:--:--</strong>
                        </div>
                        <% } else if (!resultsAnnounced) { %>
                        <p>No active election is available right now. This card updates live when the admin starts one.</p>
                        <% } else { %>
                        <p class="muted">The recent election has concluded and results are published.</p>
                        <% } %>
                    </div>
                </div>

                <%-- Results Panel (Conditional) --%>
                <% if (resultsAnnounced && (finalResults != null || activeElection != null)) { %>
                <div class="panel results-panel animate-in">
                    <div class="panel-header-row">
                        <h3>Election Results</h3>
                        <span class="status-pill active">Official</span>
                    </div>
                    <div class="results-summary">
                        <h2 style="margin-bottom: 8px;"><%= latestElection != null ? latestElection.getTitle() : (activeElection != null ? activeElection.getTitle() : "Recent Election") %></h2>
                        <p class="muted" style="margin-bottom: 24px;">Results have been verified and announced by the Commission.</p>
                        
                        <div class="results-list" style="display: flex; flex-direction: column; gap: 16px;">
                            <% 
                               int totalVotes = 0;
                               if (finalResults != null) {
                                   for (com.voting.model.VoteStat stat : finalResults) totalVotes += stat.getTotalVotes();
                                   for (int i = 0; i < finalResults.size(); i++) {
                                       com.voting.model.VoteStat stat = finalResults.get(i);
                                       double pct = totalVotes == 0 ? 0 : (stat.getTotalVotes() * 100.0 / totalVotes);
                            %>
                            <div class="result-item">
                                <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                                    <strong><%= stat.getCandidateName() %></strong>
                                    <span><%= stat.getTotalVotes() %> votes (<%= String.format("%.1f", pct) %>%)</span>
                                </div>
                                <div class="progress-bar-bg" style="height: 8px;">
                                    <div class="progress-bar-fill" style="width: <%= pct %>%; background: <%= i == 0 ? "var(--success)" : "var(--primary)" %>"></div>
                                </div>
                            </div>
                            <%     }
                               } else { %>
                               <p class="muted">Live results are currently being processed.</p>
                            <% } %>
                        </div>
                    </div>
                </div>
                <% } %>

                <%-- Voting Panel --%>
                <div class="panel">
                    <div class="panel-header-row">
                        <h3>Cast Your Vote</h3>
                        <% if (activeElection != null) { %>
                        <input type="text" id="candidateSearch" class="table-search" placeholder="Search candidates">
                        <% } %>
                    </div>
                    <form method="post" action="${pageContext.request.contextPath}/user/vote" id="voteForm" class="stack-form">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <div id="candidateList" class="candidate-list">
                            <% if (candidates != null && !candidates.isEmpty()) { %>
                            <% for (Candidate candidate : candidates) { %>
                            <label class="candidate-option"
                                   data-name="<%= candidate.getName() %>"
                                   data-manifesto="<%= candidate.getManifesto() %>"
                                   data-election-id="<%= candidate.getElectionId() %>"
                                   data-photo="<%= candidate.getPhotoPath() != null && !candidate.getPhotoPath().isEmpty() ? request.getContextPath() + candidate.getPhotoPath() : "" %>">
                                <input type="radio" name="candidateId" value="<%= candidate.getId() %>" <%= hasVoted ? "disabled" : "" %> required>
                                <% if (candidate.getPhotoPath() != null && !candidate.getPhotoPath().isEmpty()) { %>
                                <img src="${pageContext.request.contextPath}<%= candidate.getPhotoPath() %>"
                                     alt="<%= candidate.getName() %>"
                                     style="width:56px;height:56px;border-radius:50%;object-fit:cover;border:2px solid var(--line);flex-shrink:0;">
                                <% } else { %>
                                <div style="width:56px;height:56px;border-radius:50%;background:linear-gradient(135deg,rgba(99,102,241,0.3),rgba(16,185,129,0.2));display:flex;align-items:center;justify-content:center;font-size:1.5rem;font-weight:bold;color:#fff;flex-shrink:0;">
                                    <%= candidate.getName().substring(0,1).toUpperCase() %>
                                </div>
                                <% } %>
                                <span>
                                    <strong><%= candidate.getName() %></strong>
                                    <small><%= candidate.getManifesto() %></small>
                                </span>
                                <button type="button" class="btn btn-sm btn-outline candidate-details-btn">Details</button>
                            </label>
                            <% } %>
                            <% } else { %>
                            <p class="muted">Candidates will appear here once an election is active.</p>
                            <% } %>
                        </div>
                        <div id="otpSection" style="margin-top: 16px; padding: 24px; border: 1px solid var(--line); border-radius: 16px; background: rgba(255,255,255,0.02); <%= hasVoted || activeElection == null || !isProfileComplete ? "display:none;" : "" %>">
                            <div class="panel-header-row" style="margin-bottom: 20px;">
                                <h4 style="margin: 0;">Biometric & Security Verification</h4>
                                <span class="status-pill active" id="faceStatus">Waiting for Camera</span>
                            </div>
                            
                            <%-- Face Verification UI --%>
                            <div id="faceAuthContainer" style="margin-bottom: 24px; text-align: center;">
                                <div style="position: relative; width: 240px; height: 180px; margin: 0 auto 16px; border-radius: 12px; overflow: hidden; background: #000; border: 2px solid var(--line);" id="videoWrapper">
                                    <video id="webcam" autoplay muted playsinline style="width: 100%; height: 100%; object-fit: cover;"></video>
                                    <div id="faceScannerOverlay" style="position: absolute; inset: 0; border: 2px solid var(--primary); opacity: 0.5; display: none; animation: pulse 1.5s infinite;"></div>
                                    <canvas id="faceCanvas" style="position: absolute; inset: 0; pointer-events: none;"></canvas>
                                </div>
                                <button type="button" class="btn btn-sm btn-outline" id="startCameraBtn">Initialize Camera</button>
                                <button type="button" class="btn btn-sm btn-primary" id="verifyFaceBtn" style="display: none;">Verify Face Identity</button>
                                <p class="muted" style="font-size: 0.75rem; margin-top: 12px;">We compare your live feed with your profile photo: <br><strong><%= user.getName() %></strong></p>
                            </div>

                            <div style="padding-top: 20px; border-top: 1px solid var(--line);">
                                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                                    <label style="font-weight: 600; margin: 0;">2FA OTP Code</label>
                                    <button type="button" class="btn btn-sm btn-outline" id="requestOtpBtn">Get OTP</button>
                                </div>
                                <input type="text" name="otp" id="otpInput" placeholder="Enter 6-digit code" maxlength="6" style="width: 100%; text-align: center; font-size: 1.2rem; letter-spacing: 4px;" <%= hasVoted ? "disabled" : "" %>>
                            </div>
                        </div>
                        <button class="btn btn-primary" type="submit" style="margin-top: 16px;" <%= hasVoted || activeElection == null || !isProfileComplete || !eligibleForActiveElection ? "disabled" : "" %>> 
                            <%= hasVoted ? "Vote Already Recorded" : (!isProfileComplete ? "Complete Profile to Vote" : (!eligibleForActiveElection ? "Not Eligible for This Election" : "Confirm & Submit Vote")) %> 
                        </button>
                    </form>
                </div>

                <%-- Turnout and Slip Section --%>
                <div class="panel turnout-panel">
                    <div class="turnout-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
                        <h3 style="margin:0;">Live Turnout</h3>
                        <span id="userTurnoutPercentage" style="font-weight: 800; color: var(--primary); font-size: 1.2rem;"><%= turnout != null ? turnout.get("percentage") : 0 %>%</span>
                    </div>
                    <div class="progress-bar-bg" style="margin-bottom: 16px;">
                        <div class="progress-bar-fill" id="userTurnoutFill" style="width: <%= turnout != null ? turnout.get("percentage") : 0 %>%;"></div>
                    </div>
                    <p class="muted slim" style="font-size: 0.85rem;">
                        <span id="userTurnoutVotes"><%= turnout != null ? turnout.get("votesCast") : 0 %></span>
                        of
                        <span id="userTurnoutRegistered"><%= turnout != null ? turnout.get("registeredVoters") : 0 %></span>
                        registered voters have voted.
                    </p>
                    <div style="margin-top: 20px; padding: 12px; background: rgba(99, 102, 241, 0.1); border-radius: 12px; border: 1px solid rgba(99, 102, 241, 0.2);">
                        <p style="font-size: 0.8rem; margin: 0; color: var(--primary); font-weight: 600;">Election Insight</p>
                        <p style="font-size: 0.75rem; margin: 4px 0 0 0;" class="muted">Peak voting time detected: 10:00 AM - 12:00 PM.</p>
                    </div>
                </div>

                <div class="panel">
                    <h3>Voter ID Card</h3>
                    <div class="voter-card-visual" style="background: linear-gradient(135deg, #1e293b, #0f172a); border: 1px solid rgba(255,255,255,0.1); border-radius: 20px; padding: 24px; position: relative; overflow: hidden; margin-bottom: 20px;">
                        <div style="position: absolute; top: -20px; right: -20px; width: 100px; height: 100px; background: var(--primary-glow); filter: blur(40px); opacity: 0.4;"></div>
                        <div style="display: flex; gap: 16px; align-items: center; margin-bottom: 20px;">
                            <img src="${pageContext.request.contextPath}/assets/img/sec-logo.svg" style="width: 32px; height: 32px;">
                            <div style="font-size: 0.7rem; letter-spacing: 1px; text-transform: uppercase; font-weight: 800; color: #fff; opacity: 0.8;">State Election Commission</div>
                        </div>
                        <div style="display: flex; gap: 20px; align-items: flex-start;">
                            <div style="width: 70px; height: 85px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; overflow: hidden;">
                                <% if (user.getProfilePhotoPath() != null) { %><img class="voter-profile-photo" src="${pageContext.request.contextPath}<%= user.getProfilePhotoPath() %>" style="width:100%; height:100%; object-fit:cover;"><% } %>
                            </div>
                            <div style="flex: 1;">
                                <div style="font-size: 0.9rem; font-weight: 800; color: #fff; margin-bottom: 4px;"><%= user.getName() %></div>
                                <div style="font-size: 0.7rem; color: var(--text-muted);">Voter ID: <span style="color: #fff;"><%= user.getVoterIdNumber() %></span></div>
                                <div style="font-size: 0.7rem; color: var(--text-muted); margin-top: 8px;">Center: <span style="color: #fff;"><%= user.getElectionCenter() %></span></div>
                            </div>
                            <div style="width: 60px; height: 60px; background: #fff; border-radius: 8px; padding: 4px; display: flex; align-items: center; justify-content: center;">
                                <%-- Simulated QR Code --%>
                                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 2px; width: 100%; height: 100%;">
                                    <% for(int i=0; i<9; i++) { %><div style="background: <%= Math.random() > 0.5 ? "#000" : "#eee" %>;"></div><% } %>
                                </div>
                            </div>
                        </div>
                        <div style="margin-top: 16px; padding-top: 12px; border-top: 1px solid rgba(255,255,255,0.05); display: flex; justify-content: space-between; align-items: center;">
                            <span class="status-pill active" style="font-size: 0.6rem;">Verified</span>
                            <span style="font-size: 0.6rem; color: var(--text-muted);">SEC DIGITAL ASSET</span>
                        </div>
                    </div>
                    <button class="btn btn-outline" type="button" id="downloadSlipBtn" style="width:100%; justify-content: center;">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:8px;"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                        Download Voter Slip
                    </button>
                    <div id="voterSlipData"
                         data-name="<%= user.getName() != null ? user.getName() : "" %>"
                         data-voter-id="<%= user.getVoterIdNumber() != null ? user.getVoterIdNumber() : "" %>"
                         data-mobile="<%= user.getMobileNumber() != null ? user.getMobileNumber() : "" %>"
                         data-email="<%= user.getEmail() != null ? user.getEmail() : "" %>"
                         data-center="<%= user.getElectionCenter() != null ? user.getElectionCenter() : "" %>"
                         data-city="<%= user.getCity() != null ? user.getCity() : "" %>"
                         data-state="<%= user.getState() != null ? user.getState() : "" %>"
                         <% 
                            String photoPath = user.getProfilePhotoPath();
                            String absolutePhotoUrl = "";
                            String absoluteQrUrl = "";
                            String absoluteVerifyUrl = "";
                            if (photoPath != null && !photoPath.isEmpty()) {
                                String scheme = request.getScheme();
                                String serverName = request.getServerName();
                                int serverPort = request.getServerPort();
                                String cp = request.getContextPath();
                                
                                StringBuilder sb = new StringBuilder();
                                sb.append(scheme).append("://").append(serverName);
                                if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
                                    sb.append(":").append(serverPort);
                                }
                                sb.append(cp);
                                if (!photoPath.startsWith("/")) sb.append("/");
                                sb.append(photoPath);
                                absolutePhotoUrl = sb.toString();
                            }
                            String base = WebUrlUtil.baseUrl(request);
                            absoluteQrUrl = request.getContextPath() + "/user/voter-slip-qr?ts=" + System.currentTimeMillis();
                            absoluteVerifyUrl = base + "/verify-voter-slip?userId=" + user.getId();
                         %>
                         data-photo="<%= absolutePhotoUrl %>"
                         data-qr-url="<%= absoluteQrUrl %>"
                         data-verify-url="<%= absoluteVerifyUrl %>"
                         data-vote-status="<%= hasVoted ? "VOTED" : "NOT VOTED" %>"
                         data-election="<%= activeElection != null ? activeElection.getTitle() : "No active election" %>"></div>
                </div>
            </section>
        </div>

        <div id="section-announcements" class="tab-section">
            <section class="panel">
                <div class="panel-header-row">
                    <h3>Official Announcements</h3>
                    <p class="muted">Published results and commission statements.</p>
                </div>
                <div class="announcement-list">
                    <% 
                       List<Election> allElections = (List<Election>) payload.get("elections");
                       boolean foundAnyAnn = false;
                       if (allElections != null) {
                           for (Election el : allElections) {
                               if (el.isResultsAnnounced()) {
                                   foundAnyAnn = true;
                                   List<VoteStat> resultRows = announcedResults != null ? announcedResults.get(el.getId()) : null;
                                   VoteStat winner = resultRows != null && !resultRows.isEmpty() ? resultRows.get(0) : null;
                    %>
                    <div class="announcement-card animate-in" style="border-left: 4px solid var(--success); padding: 20px; background: var(--bg-card); border-radius: 12px; margin-bottom: 16px;">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                            <h3 style="margin: 0;"><%= el.getTitle() %> Results</h3>
                            <span class="status-pill active">Official Announcement</span>
                        </div>
                        <p><%= el.getDescription() %></p>
                        <p class="muted">Election Date: <%= el.getElectionDate() %></p>
                        <div style="margin-top: 16px; padding: 14px; border: 1px solid var(--line); border-radius: 10px; background: rgba(16, 185, 129, 0.08);">
                            <div class="label" style="margin-bottom: 6px;">Winner</div>
                            <% if (winner != null && winner.getTotalVotes() > 0) { %>
                            <strong style="color: var(--secondary); font-size: 1.05rem;"><%= winner.getCandidateName() %></strong>
                            <span class="muted"> - <%= winner.getTotalVotes() %> votes</span>
                            <% } else { %>
                            <strong class="muted">No winner yet because no votes were recorded.</strong>
                            <% } %>
                        </div>
                        <div style="margin-top: 14px;">
                            <div class="label" style="margin-bottom: 8px;">Candidate Vote Count</div>
                            <% if (resultRows != null && !resultRows.isEmpty()) { %>
                            <div style="display: grid; gap: 8px;">
                                <% for (VoteStat row : resultRows) { %>
                                <div style="display: flex; justify-content: space-between; gap: 16px; padding: 8px 10px; border: 1px solid var(--line); border-radius: 8px;">
                                    <span><%= row.getCandidateName() %></span>
                                    <strong><%= row.getTotalVotes() %> votes</strong>
                                </div>
                                <% } %>
                            </div>
                            <% } else { %>
                            <p class="muted">No candidates found for this election.</p>
                            <% } %>
                        </div>
                        <div style="margin-top: 16px;">
                            <button class="btn btn-sm btn-outline view-results-btn" data-election-id="<%= el.getId() %>">View Detail Results</button>
                        </div>
                    </div>
                    <% 
                               }
                           }
                       }
                       if (!foundAnyAnn) {
                    %>
                    <div class="panel" style="text-align: center; padding: 40px;">
                        <p class="muted">No official announcements published yet.</p>
                    </div>
                    <% } %>
                </div>
            </section>
        </div>

        <div id="section-vote-history" class="tab-section">
            <section class="panel">
                <h3>Voting History</h3>
                <p class="muted">Your receipt proves that a vote was recorded. Candidate choices are not shown here.</p>
                <div class="table-wrap">
                    <table>
                        <thead>
                        <tr>
                            <th>Election</th>
                            <th>Date</th>
                            <th>Receipt ID</th>
                            <th>Digital Signature</th>
                            <th>Status</th>
                            <th>Recorded At</th>
                        </tr>
                        </thead>
                        <tbody id="voteHistoryBody">
                        <% if (voteHistory != null && !voteHistory.isEmpty()) { for (Map<String, Object> item : voteHistory) { %>
                        <tr>
                            <td><%= item.get("title") %></td>
                            <td><%= item.get("electionDate") %></td>
                            <td>
                                <strong><%= item.get("receiptId") != null ? item.get("receiptId") : "Legacy vote" %></strong>
                                <% if (item.get("receiptId") != null) { %>
                                <br><a class="muted" href="${pageContext.request.contextPath}/user/receipt?id=<%= item.get("receiptId") %>" target="_blank">Open receipt</a>
                                <% } %>
                            </td>
                            <td style="font-family: monospace; font-size: 0.7rem; color: var(--success);"><%= item.get("digitalSignature") != null ? item.get("digitalSignature") : "N/A" %></td>
                            <td>Voted</td>
                            <td><%= item.get("createdAt") %></td>
                        </tr>
                        <% }} else { %>
                        <tr><td colspan="5" class="muted">No voting history yet.</td></tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </section>
        </div>

        <div id="section-nomination" class="tab-section">
            <section class="dashboard-grid">
                <div class="panel">
                    <h3>Apply as Candidate</h3>
                    <p class="muted" style="margin-bottom: 20px;">Submit your nomination for an election. Admin approval is required before you appear on the ballot.</p>
                    <% 
                       List<Election> nominationElections = (List<Election>) payload.get("elections");
                       boolean hasOpenNominationElection = false;
                       if (nominationElections != null) {
                           for (Election election : nominationElections) {
                               if (!"COMPLETED".equalsIgnoreCase(election.getStatus())) {
                                   hasOpenNominationElection = true;
                                   break;
                               }
                           }
                       }
                    %>
                    <% if (!hasOpenNominationElection) { %>
                    <div class="alert error" style="margin-bottom: 18px;">
                        Candidate nominations are closed because there is no scheduled or active election right now.
                    </div>
                    <% } %>
                    <form method="post" action="${pageContext.request.contextPath}/user/actions" class="stack-form">
                        <%= CsrfUtil.hiddenInput(request) %>
                        <input type="hidden" name="action" value="submitNomination">
                        <label>Election
                            <select name="electionId" required <%= !hasOpenNominationElection ? "disabled" : "" %> style="width:100%;padding:12px;background:#0f172a;border:1px solid var(--line);border-radius:10px;color:#fff;">
                                <% if (hasOpenNominationElection && nominationElections != null) { for (Election election : nominationElections) {
                                       if (!"COMPLETED".equalsIgnoreCase(election.getStatus())) {
                                %>
                                <option value="<%= election.getId() %>"><%= election.getTitle() %> (<%= election.getStatus() %>)</option>
                                <%     }
                                      }
                                   } else {
                                %>
                                <option value="">No open elections</option>
                                <%
                                   } 
                                %>
                            </select>
                        </label>
                        <label>Manifesto
                            <textarea name="manifesto" rows="6" required <%= !hasOpenNominationElection ? "disabled" : "" %> placeholder="Describe your campaign promises and priorities." style="resize: vertical; padding: 12px; border-radius: 8px; border: 1px solid var(--line); background: var(--bg-card); color: var(--text-color); font-family: inherit;"></textarea>
                        </label>
                        <button class="btn btn-primary" type="submit" <%= !hasOpenNominationElection ? "disabled" : "" %>>Submit Nomination</button>
                    </form>
                </div>
                <div class="panel">
                    <h3>My Nomination Status</h3>
                    <div style="display:grid;gap:12px;">
                        <% if (nominations != null && !nominations.isEmpty()) { for (Map<String, Object> n : nominations) { 
                            String nStatus = n.get("status") != null ? n.get("status").toString() : "PENDING";
                        %>
                        <div class="result-item">
                            <div style="display:flex;justify-content:space-between;gap:12px;">
                                <strong><%= n.get("electionTitle") %></strong>
                                <span class="status-pill <%= "APPROVED".equals(nStatus) ? "active" : "inactive" %>"><%= nStatus %></span>
                            </div>
                            <p class="muted" style="margin-top:8px;"><%= n.get("adminNote") != null ? n.get("adminNote") : n.get("manifesto") %></p>
                        </div>
                        <% }} else { %>
                        <p class="muted">No nomination applications submitted yet.</p>
                        <% } %>
                    </div>
                </div>
            </section>
        </div>

        <div id="section-my-profile" class="tab-section">
            <section class="panel" style="max-width: 860px;">
                <div style="display: flex; align-items: center; gap: 24px; border-bottom: 1px solid var(--line); padding-bottom: 24px; margin-bottom: 24px; flex-wrap: wrap;">
                    <% if (user.getProfilePhotoPath() != null && !user.getProfilePhotoPath().trim().isEmpty()) { %>
                    <img src="${pageContext.request.contextPath}<%= user.getProfilePhotoPath() %>" alt="Profile photo" style="width: 96px; height: 96px; object-fit: cover; border-radius: 50%; border: 2px solid var(--line);">
                    <% } else { %>
                    <div style="width: 96px; height: 96px; background: var(--primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 36px; font-weight: bold;">
                        <%= user.getName() != null && !user.getName().isEmpty() ? user.getName().substring(0, 1).toUpperCase() : "U" %>
                    </div>
                    <% } %>
                    <div>
                        <h2 style="margin: 0 0 8px 0;"><%= user.getName() %></h2>
                        <span class="status-pill <%= hasVoted ? "active" : "inactive" %>" style="margin-left: 0;">
                            <%= hasVoted ? "Voted in Current Election" : "Has Not Voted" %>
                        </span>
                        <span class="status-pill <%= "Verified Voter".equals(profileVerificationStatus) ? "active" : "inactive" %>" style="margin-left: 8px;">
                            <%= profileVerificationStatus %>
                        </span>
                    </div>
                </div>

                <form method="post" action="${pageContext.request.contextPath}/user/actions" enctype="multipart/form-data" class="auth-form register-grid">
                    <%= CsrfUtil.hiddenInput(request) %>
                    <input type="hidden" name="action" value="updateProfile">
                    <label>Full Name
                        <input type="text" name="name" value="<%= user.getName() != null ? user.getName() : "" %>" required>
                    </label>
                    <label>Email
                        <input type="email" name="email" value="<%= user.getEmail() != null ? user.getEmail() : "" %>" required>
                    </label>
                    <label>Mobile Number
                        <input type="tel" name="mobileNumber" value="<%= user.getMobileNumber() != null ? user.getMobileNumber() : "" %>" pattern="[0-9]{10}" required>
                    </label>
                    <label>Voter ID Number
                        <input type="text" name="voterIdNumber" value="<%= user.getVoterIdNumber() != null ? user.getVoterIdNumber() : "" %>" required>
                    </label>
                    <label>Date of Birth
                        <input type="date" name="dateOfBirth" value="<%= user.getDateOfBirth() != null ? user.getDateOfBirth() : "" %>" required>
                    </label>
                    <label>Age
                        <input type="number" name="age" value="<%= user.getAge() != null ? user.getAge() : "" %>" min="18" max="120" readonly>
                    </label>
                    <label>Election Center
                        <input type="text" name="electionCenter" value="<%= user.getElectionCenter() != null ? user.getElectionCenter() : "" %>" required>
                    </label>
                    <label>City
                        <input type="text" name="city" value="<%= user.getCity() != null ? user.getCity() : "" %>" required>
                    </label>
                    <label>State
                        <input type="text" name="state" value="<%= user.getState() != null ? user.getState() : "" %>" required>
                    </label>
                    <label>Profile Photo
                        <input type="file" name="profilePhoto" accept="image/*">
                    </label>
                    <div style="display: flex; justify-content: space-between; gap: 16px; grid-column: 1 / -1; border-top: 1px solid var(--line); padding-top: 16px; flex-wrap: wrap;">
                        <span class="muted">System ID: <strong><%= user.getId() %></strong></span>
                        <span class="muted">Role: <strong style="text-transform: capitalize;"><%= user.getRole() %></strong></span>
                    </div>
                    <button class="btn btn-primary" type="submit">Save Profile</button>
                </form>
            </section>
        </div>

        <div id="section-education" class="tab-section">
            <section class="dashboard-grid">
                <div class="panel">
                    <h3>How to Vote</h3>
                    <div style="display: flex; flex-direction: column; gap: 20px;">
                        <div style="display: flex; gap: 16px;">
                            <div style="width: 32px; height: 32px; background: var(--primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; flex-shrink: 0;">1</div>
                            <div>
                                <h4 style="margin: 0 0 4px 0;">Verify your Identity</h4>
                                <p class="muted" style="font-size: 0.9rem;">Ensure your profile photo and voter ID are correctly uploaded in the "My Profile" section.</p>
                            </div>
                        </div>
                        <div style="display: flex; gap: 16px;">
                            <div style="width: 32px; height: 32px; background: var(--primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; flex-shrink: 0;">2</div>
                            <div>
                                <h4 style="margin: 0 0 4px 0;">Select Candidate</h4>
                                <p class="muted" style="font-size: 0.9rem;">Review candidates in the "Voting Center." Use the "Details" button to read their manifestos.</p>
                            </div>
                        </div>
                        <div style="display: flex; gap: 16px;">
                            <div style="width: 32px; height: 32px; background: var(--primary); color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; flex-shrink: 0;">3</div>
                            <div>
                                <h4 style="margin: 0 0 4px 0;">Secure OTP Verification</h4>
                                <p class="muted" style="font-size: 0.9rem;">Request a security code. This two-factor authentication ensures only you can cast your vote.</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="panel">
                    <h3>Frequently Asked Questions</h3>
                    <div style="display: flex; flex-direction: column; gap: 16px;">
                        <details style="background: rgba(255,255,255,0.03); border-radius: 12px; padding: 12px; border: 1px solid var(--line);">
                            <summary style="font-weight: 600; cursor: pointer; padding: 4px;">Is my vote anonymous?</summary>
                            <p class="muted" style="font-size: 0.85rem; margin-top: 8px;">Yes. While the system records that you have voted, your specific candidate choice is encrypted and stored separately.</p>
                        </details>
                        <details style="background: rgba(255,255,255,0.03); border-radius: 12px; padding: 12px; border: 1px solid var(--line);">
                            <summary style="font-weight: 600; cursor: pointer; padding: 4px;">Can I change my vote?</summary>
                            <p class="muted" style="font-size: 0.85rem; margin-top: 8px;">No. Once a vote is securely committed to the blockchain, it is final and cannot be modified or deleted.</p>
                        </details>
                    </div>
                </div>
            </section>
        </div>

        <div id="section-grievance" class="tab-section">
            <section class="dashboard-grid">
            <div class="panel">
                <h3>Submit a Grievance</h3>
                <p class="muted" style="margin-bottom: 24px;">If you are facing issues with the portal or want to report suspicious activity, please submit a grievance ticket below.</p>
                <form method="post" action="${pageContext.request.contextPath}/user/actions" class="stack-form">
                    <%= CsrfUtil.hiddenInput(request) %>
                    <input type="hidden" name="action" value="submitGrievance">
                    <textarea name="issue" placeholder="Describe your issue in detail..." rows="5" required style="resize: vertical; padding: 12px; border-radius: 8px; border: 1px solid var(--line); background: var(--bg-card); color: var(--text-color); font-family: inherit; font-size: 0.95rem;"></textarea>
                    <button class="btn btn-primary" type="submit">Submit Ticket</button>
                </form>
            </div>
            <div class="panel">
                <h3>Ticket Status</h3>
                <div class="ticket-list">
                    <% if (complaints != null && !complaints.isEmpty()) { for (Map<String, Object> ticket : complaints) { 
                        String ticketId = ticket.get("_id") != null ? ticket.get("_id").toString() : "";
                        String ticketStatus = ticket.get("status") != null ? ticket.get("status").toString() : "Open";
                        String ticketIssue = ticket.get("issue") != null ? ticket.get("issue").toString() : "";
                        String ticketReply = ticket.get("admin_reply") != null ? ticket.get("admin_reply").toString() : "";
                        String ticketShortId = ticketId.length() > 18 ? ticketId.substring(18) : ticketId;
                    %>
                    <div class="ticket-card">
                        <div style="display:flex;justify-content:space-between;gap:12px;align-items:center;">
                            <strong>Ticket #<%= ticketShortId %></strong>
                            <span class="status-pill <%= "Resolved".equals(ticketStatus) ? "active" : "inactive" %>"><%= ticketStatus %></span>
                        </div>
                        <p><%= ticketIssue %></p>
                        <% if (!ticketReply.trim().isEmpty()) { %>
                        <p class="muted"><strong>Admin reply:</strong> <%= ticketReply %></p>
                        <% } %>
                    </div>
                    <% }} else { %>
                    <p class="muted">No grievance tickets submitted yet.</p>
                    <% } %>
                </div>
            </div>
            </section>
        </div>

        <div id="section-rules" class="tab-section">
            <section class="panel" style="max-width: 900px;">
                <div class="panel-header-row" style="margin-bottom: 32px;">
                    <h3 style="margin: 0;">Official Election Rules</h3>
                    <span class="status-pill active">Security Protocol</span>
                </div>
                
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px;">
                    <div style="padding: 24px; background: rgba(99, 102, 241, 0.05); border-radius: 20px; border: 1px solid rgba(99, 102, 241, 0.1);">
                        <div style="width: 40px; height: 40px; background: var(--primary); color: #fff; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-weight: 800; margin-bottom: 16px;">01</div>
                        <h4 style="margin-bottom: 8px;">Identity Verification</h4>
                        <p class="muted" style="font-size: 0.9rem; line-height: 1.6;">You must have a complete profile and pass the <strong>Live Face Recognition</strong> scan before voting to verify your identity.</p>
                    </div>

                    <div style="padding: 24px; background: rgba(16, 185, 129, 0.05); border-radius: 20px; border: 1px solid rgba(16, 185, 129, 0.1);">
                        <div style="width: 40px; height: 40px; background: var(--secondary); color: #fff; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-weight: 800; margin-bottom: 16px;">02</div>
                        <h4 style="margin-bottom: 8px;">One Vote Per Citizen</h4>
                        <p class="muted" style="font-size: 0.9rem; line-height: 1.6;">Each verified voter can submit exactly one vote per election. Once a vote is securely committed, it cannot be modified or reversed.</p>
                    </div>

                    <div style="padding: 24px; background: rgba(255, 255, 255, 0.03); border-radius: 20px; border: 1px solid var(--line);">
                        <div style="width: 40px; height: 40px; background: #64748b; color: #fff; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-weight: 800; margin-bottom: 16px;">03</div>
                        <h4 style="margin-bottom: 8px;">Blockchain Security</h4>
                        <p class="muted" style="font-size: 0.9rem; line-height: 1.6;">All votes are encrypted and stored on a decentralized blockchain. Your identity is private, and your vote is immutable and verifiable.</p>
                    </div>

                    <div style="padding: 24px; background: rgba(255, 255, 255, 0.03); border-radius: 20px; border: 1px solid var(--line);">
                        <div style="width: 40px; height: 40px; background: #64748b; color: #fff; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-weight: 800; margin-bottom: 16px;">04</div>
                        <h4 style="margin-bottom: 8px;">Account Security</h4>
                        <p class="muted" style="font-size: 0.9rem; line-height: 1.6;">Do not share your credentials or security OTP with anyone. The Commission will never ask for your password via email or phone.</p>
                    </div>
                </div>
            </section>
        </div>

    </main>
</div>
<div class="modal-backdrop" id="candidateModal" aria-hidden="true">
    <div class="modal-panel">
        <div class="panel-header-row" style="margin-bottom: 32px;">
            <h2 id="candidateModalName" style="margin: 0; font-size: 1.8rem; font-weight: 800;">Candidate</h2>
            <button class="btn btn-sm btn-outline" type="button" id="candidateModalClose" style="border-radius: 50%; width: 40px; height: 40px; padding: 0; justify-content: center;">✕</button>
        </div>
        
        <div style="display: flex; gap: 24px; margin-bottom: 32px; align-items: flex-start;">
            <div id="candidateModalMedia">
                <img id="candidateModalPhoto" src="" alt=""
                     style="width: 100px; height: 100px; border-radius: 24px; object-fit: cover; border: 2px solid var(--primary); display: none;">
                <div id="candidateModalInitial"
                     style="width: 100px; height: 100px; border-radius: 24px; background: linear-gradient(135deg, var(--primary), var(--secondary)); display: flex; align-items: center; justify-content: center; font-size: 2.5rem; font-weight: 800; color: #fff;">
                </div>
            </div>
            <div style="flex: 1;">
                <span class="eyebrow" style="margin-bottom: 8px;">Campaign Manifesto</span>
                <p id="candidateModalManifesto" style="line-height: 1.6; color: var(--text-main); font-size: 1.05rem;"></p>
            </div>
        </div>

        <div style="padding-top: 24px; border-top: 1px solid var(--line); display: flex; justify-content: space-between; align-items: center;">
            <p class="muted" style="margin: 0;">Election ID: <strong id="candidateModalElection" style="color: var(--primary);"></strong></p>
            <span class="status-pill active">Verified Candidate</span>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/js/user.js?v=10"></script>
<script src="${pageContext.request.contextPath}/assets/js/theme.js"></script>
</body>
</html>
