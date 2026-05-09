<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SEC Online Voting Portal</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/assets/img/sec-logo.svg">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=20">
    <link rel="manifest" href="${pageContext.request.contextPath}/manifest.json">
    <script>
        if ('serviceWorker' in navigator) {
            window.addEventListener('load', () => {
                navigator.serviceWorker.register('${pageContext.request.contextPath}/sw.js')
                    .then(reg => console.log('SW Registered', reg))
                    .catch(err => console.log('SW Failed', err));
            });
        }
    </script>
</head>
<body class="landing-body dark-theme"> <!-- Forcing dark theme for premium feel -->
<header class="site-header">
    <div class="brand">
        <svg style="width: 24px; vertical-align: middle; margin-right: 8px;" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z"/>
            <path d="M9 12L11 14L15 10"/>
        </svg>
        State Election Commission
    </div>
    <nav class="landing-nav">
        <a href="#features">Features</a>
        <a href="#how-it-works">How it Works</a>
        <a href="${pageContext.request.contextPath}/login" class="btn btn-outline">Login</a>
        <a href="${pageContext.request.contextPath}/register" class="btn btn-primary">Get Started</a>
    </nav>
</header>

<main>
    <section class="hero-section">
        <div class="hero-container">
            <div class="hero-content">
                <div class="badge">SEC DIGITAL PORTAL 2.0</div>
                <h1>The Future of <span>Democratic</span> Participation</h1>
                <p>Experience a transparent, secure, and tamper-proof voting system powered by modern cryptographic signatures and real-time blockchain-inspired auditing.</p>
                <div class="hero-btns">
                    <a href="${pageContext.request.contextPath}/register" class="btn btn-primary btn-lg">Register to Vote</a>
                    <a href="#features" class="btn btn-outline btn-lg">Learn More</a>
                </div>
                <div class="hero-stats">
                    <div class="h-stat">
                        <strong>100%</strong>
                        <span>Encrypted</span>
                    </div>
                    <div class="h-stat">
                        <strong>Instant</strong>
                        <span>Results</span>
                    </div>
                    <div class="h-stat">
                        <strong>Zero</strong>
                        <span>Downtime</span>
                    </div>
                </div>
            </div>
            <div class="hero-visual">
                <img src="${pageContext.request.contextPath}/assets/images/hero.png" alt="Digital Voting Illustration" class="floating">
            </div>
        </div>
    </section>

    <section id="features" class="info-section">
        <div class="section-header">
            <span class="eyebrow">Advanced Security</span>
            <h2>Hardened Core Features</h2>
            <p>Our platform is built with multiple layers of security to ensure every vote counts and remains anonymous.</p>
        </div>
        <div class="feature-cards">
            <div class="f-card">
                <div class="icon">🔒</div>
                <h3>Cryptographic Signatures</h3>
                <p>Every ballot is signed with a unique SHA-256 HMAC, ensuring data integrity and preventing manual database tampering.</p>
            </div>
            <div class="f-card">
                <div class="icon">🤖</div>
                <h3>Anti-Bot Protection</h3>
                <p>Advanced math-based CAPTCHA challenges and rate-limiting protect against automated credential stuffing and bot registration.</p>
            </div>
            <div class="f-card">
                <div class="icon">⏱️</div>
                <h3>Real-time Updates</h3>
                <p>Powered by WebSockets, our dashboard provides live voter turnout and election results without ever needing a refresh.</p>
            </div>
            <div class="f-card">
                <div class="icon">📱</div>
                <h3>Mobile Ready</h3>
                <p>Vote from any device. Our responsive design ensures a seamless experience on smartphones, tablets, and desktops.</p>
            </div>
        </div>
    </section>

    <section id="how-it-works" class="how-it-works">
        <div class="container">
            <h2>Voting Made Simple</h2>
            <div class="steps-grid">
                <div class="step">
                    <div class="step-num">01</div>
                    <h4>Register</h4>
                    <p>Create your secure account using your Voter ID and mobile number for verification.</p>
                </div>
                <div class="step">
                    <div class="step-num">02</div>
                    <h4>Verify</h4>
                    <p>Log in securely with CAPTCHA protection and verify your identity via OTP during election time.</p>
                </div>
                <div class="step">
                    <div class="step-num">03</div>
                    <h4>Cast Vote</h4>
                    <p>Choose your representative from the candidate list and cast your encrypted ballot.</p>
                </div>
                <div class="step">
                    <div class="step-num">04</div>
                    <h4>Audit</h4>
                    <p>Get a unique receipt and verify your vote's digital signature in your personal history tab.</p>
                </div>
            </div>
        </div>
    </section>

    <section class="cta-section">
        <div class="cta-card">
            <h2>Ready to exercise your right?</h2>
            <p>Join thousands of citizens participating in a fair and transparent election process.</p>
            <a href="${pageContext.request.contextPath}/register" class="btn btn-primary btn-lg">Create Your Account Today</a>
        </div>
    </section>
</main>

<footer class="main-footer">
    <div class="footer-grid">
        <div class="footer-info">
            <h3>State Election Commission</h3>
            <p>Ensuring free, fair, and transparent elections for all citizens through digital innovation.</p>
        </div>
        <div class="footer-links">
            <h4>Quick Links</h4>
            <a href="${pageContext.request.contextPath}/">Home</a>
            <a href="${pageContext.request.contextPath}/login">Login</a>
            <a href="${pageContext.request.contextPath}/register">Register</a>
        </div>
        <div class="footer-contact">
            <h4>Help & Support</h4>
            <p>Email: support@sec-portal.gov</p>
            <p>Phone: 1800-VOTE-SEC</p>
        </div>
    </div>
    <div class="footer-bottom">
        &copy; 2026 State Election Commission. Securely powered by Advanced Agentic Coding.
    </div>
</footer>
<script>
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            document.querySelector(this.getAttribute('href')).scrollIntoView({
                behavior: 'smooth'
            });
        });
    });
</script>
</body>
</html>
