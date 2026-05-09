<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Security Alert | SEC Portal</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/img/sec-logo.svg">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    <style>
        .error-content {
            text-align: center;
            max-width: 600px;
            padding: 60px;
            background: rgba(15, 23, 42, 0.8);
            border: 1px solid rgba(185, 28, 28, 0.2);
            border-radius: 32px;
            backdrop-filter: blur(20px);
        }
        .error-icon {
            color: #ef4444;
            margin-bottom: 24px;
        }
    </style>
</head>
<body class="auth-body dark-theme">
    <div class="auth-background"></div>
    <div class="error-content">
        <div class="error-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
        </div>
        <h1 style="font-size: 2.5rem; margin-bottom: 8px;">Access Issue Detected</h1>
        <div style="font-weight: 700; color: #ef4444; margin-bottom: 24px;">Status: <%= request.getAttribute("javax.servlet.error.status_code") %></div>
        <p style="color: #94a3b8; font-size: 1.1rem; line-height: 1.6; margin-bottom: 32px;">
            <%= request.getAttribute("javax.servlet.error.message") != null ? request.getAttribute("javax.servlet.error.message") : "The system encountered an unexpected state. For security reasons, this action has been restricted." %>
        </p>
        <a href="${pageContext.request.contextPath}/" class="btn btn-primary">Return to Safety</a>
    </div>
</body>
</html>
