<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="javax.servlet.http.HttpServletRequest" %>
<%@ page import="com.voting.util.CsrfUtil" %>
<%!
    private String fieldValue(HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value == null ? "" : escapeHtml(String.valueOf(value));
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register | State Election Commission</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/img/sec-logo.svg">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=20">
</head>
<body class="auth-body dark-theme">
<header class="auth-site-header">
    <div class="brand-logo" style="margin:0; display:flex; align-items:center; gap:12px;">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="2.5">
            <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z"/>
            <path d="M9 12L11 14L15 10"/>
        </svg>
        <span style="font-weight: 800; font-size: 1.2rem; letter-spacing: -0.02em;">SEC PORTAL</span>
    </div>
    <nav class="auth-nav-links">
        <a href="${pageContext.request.contextPath}/">Home</a>
        <a href="${pageContext.request.contextPath}/#features">Security</a>
        <a href="${pageContext.request.contextPath}/#how-it-works">How it Works</a>
    </nav>
    <div class="status-indicator" style="display:flex; align-items:center; gap:8px; font-size:0.8rem; color:#10b981; font-weight:600;">
        <span style="width:8px; height:8px; background:#10b981; border-radius:50%; animation:pulse 2s infinite;"></span>
        Network Secure
    </div>
</header>
<div class="auth-background"></div>
<div class="auth-layout">
    <div class="auth-visual">
        <span class="visual-badge">Identity v4.0 Secure</span>
        <h2>Unified<br>Identity.</h2>
        <p>Your digital identity is hashed and secured using multi-layer biometric verification protocols.</p>
        
        <div class="quote-container">
            <svg class="quote-icon" viewBox="0 0 24 24" fill="currentColor" style="width: 48px !important; height: 48px !important;">
                <path d="M14.017 21L14.017 18C14.017 16.8954 14.9124 16 16.017 16H19.017C19.5693 16 20.017 15.5523 20.017 15V9C20.017 8.44772 19.5693 8 19.017 8H16.017C14.9124 8 14.017 7.10457 14.017 6V3L22.017 3V12C22.017 16.9706 17.9876 21 14.017 21ZM2 21L2 18C2 16.8954 2.89543 16 4 16H7C7.55228 16 8 15.5523 8 15V9C8 8.44772 7.55228 8 7 8H4C2.89543 8 2 7.10457 2 6V3L10 3V12C10 16.9706 5.97056 21 2 21Z"/>
            </svg>
            <blockquote class="modern-quote">
                "Voting is not only our right—it is our power."
                <cite>— Loung Ung</cite>
            </blockquote>
        </div>
    </div>
    <div class="auth-container">
        <div class="auth-card">
            <div class="auth-header">
                <a href="${pageContext.request.contextPath}/" class="back-link">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5">
                        <path d="M19 12H5M12 19l-7-7 7-7"/>
                    </svg>
                    Back to Terminal
                </a>
                <h1>Create Identity</h1>
                <p>Register your profile on the secure network</p>
            </div>

            <% if (request.getAttribute("error") != null) { %>
            <div class="alert error"><%= request.getAttribute("error") %></div>
            <% } %>

            <form method="post" action="${pageContext.request.contextPath}/register" class="auth-form register-grid">
                <%= CsrfUtil.hiddenInput(request) %>
                <div class="input-group">
                    <label>Full Name</label>
                    <input type="text" name="name" placeholder="John Doe" value="<%= fieldValue(request, "nameValue") %>" required>
                </div>
                <div class="input-group">
                    <label>Email Address</label>
                    <input type="email" name="email" placeholder="john@example.com" value="<%= fieldValue(request, "emailValue") %>" required>
                </div>
                <div class="input-group">
                    <label>Mobile Number</label>
                    <input type="tel" name="mobileNumber" placeholder="10 Digit Number" pattern="[0-9]{10}" value="<%= fieldValue(request, "mobileNumberValue") %>" required>
                </div>
                <div class="input-group">
                    <label>Voter ID Number</label>
                    <input type="text" name="voterIdNumber" placeholder="XYZ1234567" value="<%= fieldValue(request, "voterIdNumberValue") %>" required>
                </div>
                <div class="input-group">
                    <label>Date of Birth</label>
                    <input type="date" name="dateOfBirth" value="<%= fieldValue(request, "dateOfBirthValue") %>" required>
                </div>
                <div class="input-group">
                    <label>Age</label>
                    <input type="number" name="age" placeholder="Min 18" min="18" max="120" value="<%= fieldValue(request, "ageValue") %>" required>
                </div>
                <div class="input-group">
                    <label>Election Center</label>
                    <input type="text" name="electionCenter" placeholder="Your Center" value="<%= fieldValue(request, "electionCenterValue") %>" required>
                </div>
                <div class="input-group">
                    <label>City</label>
                    <input type="text" name="city" placeholder="City" value="<%= fieldValue(request, "cityValue") %>" required>
                </div>
                <div class="input-group">
                    <label>State</label>
                    <input type="text" name="state" placeholder="State" value="<%= fieldValue(request, "stateValue") %>" required>
                </div>
                <div class="input-group">
                    <label>Password</label>
                    <input type="password" name="password" placeholder="Strong Password" required minlength="8">
                </div>
                <div class="input-group captcha-field">
                    <label>Security Question: <%= request.getAttribute("captchaQuestion") %></label>
                    <input type="number" name="captcha" placeholder="Answer" required>
                </div>
                <button class="btn btn-primary" type="submit">Complete Registration</button>
            </form>

            <div class="auth-footer">
                <p>Already have an account? <a href="${pageContext.request.contextPath}/login">Sign In instead</a></p>
            </div>
        </div>
    </div>
</div>
</body>
</html>
