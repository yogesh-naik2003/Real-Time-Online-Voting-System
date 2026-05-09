package com.voting.controller;

import com.voting.dao.MongoLogDao;
import com.voting.dao.UserDao;
import com.voting.model.User;
import com.voting.util.InputValidator;
import com.voting.util.LoginRateLimiter;
import com.voting.util.PasswordUtil;
import com.voting.util.SecurityAuditUtil;
import com.voting.util.SessionUtil;
import com.voting.util.SystemSettings;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession; 
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();
    private final MongoLogDao mongoLogDao = new MongoLogDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String captchaInput = req.getParameter("captcha");
        String normalizedEmail;

        if (!com.voting.util.CaptchaUtil.verifyCaptcha(req, captchaInput)) {
            req.setAttribute("error", "Invalid CAPTCHA result. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        try {
            normalizedEmail = InputValidator.email(email);
        } catch (IllegalArgumentException ex) {
            normalizedEmail = null;
        }

        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            req.setAttribute("error", "Please provide a valid email address.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        String rateLimitKey = SessionUtil.getClientIp(req) + ":" + normalizedEmail;

        try {
            if (LoginRateLimiter.isBlocked(rateLimitKey)) {
                mongoLogDao.logSecurityEvent("login_blocked", normalizedEmail, SessionUtil.getClientIp(req), "Too many failed login attempts.");
                req.setAttribute("error", "Too many failed login attempts. Please wait and try again.");
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
                return;
            }

            if (normalizedEmail != null && mongoLogDao.isUserPending(normalizedEmail)) {
                req.setAttribute("error", "Your account is pending verification by the State Election Commission.");
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
                return;
            }

            User user = userDao.findByEmail(normalizedEmail);
            if (user != null && user.getLockedUntil() != null && user.getLockedUntil().after(new java.sql.Timestamp(System.currentTimeMillis()))) {
                SecurityAuditUtil.log("LOGIN_LOCKED", normalizedEmail, SessionUtil.getClientIp(req), "Attempt to login to locked account");
                req.setAttribute("error", "Account locked due to 5+ failed attempts. Please try again after 15 minutes.");
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
                return;
            }

            if (user == null || !PasswordUtil.verifyPassword(password, user.getPassword())) {
                if (user != null) {
                    userDao.incrementFailedAttempts(user.getId());
                    if (user.getFailedAttempts() + 1 >= 5) {
                        SecurityAuditUtil.log("ACCOUNT_LOCKOUT", normalizedEmail, SessionUtil.getClientIp(req), "Account locked due to 5 failures");
                    }
                }
                LoginRateLimiter.recordFailure(rateLimitKey);
                mongoLogDao.logSecurityEvent("login_failed", normalizedEmail, SessionUtil.getClientIp(req), "Invalid credentials.");
                SecurityAuditUtil.log("LOGIN_FAILURE", normalizedEmail, SessionUtil.getClientIp(req), user == null ? "User not found" : "Incorrect password");
                req.setAttribute("error", "Invalid email or password.");
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
                return;
            }

            userDao.resetFailedAttempts(user.getId());

            if (SystemSettings.maintenanceMode && !SessionUtil.isAdminRole(user.getRole())) {
                req.setAttribute("error", "The system is currently in Maintenance Mode. Only administrators can log in.");
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
                return;
            }

            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(20 * 60);
            session.setAttribute("user", user);
            if (PasswordUtil.needsRehash(user.getPassword())) {
                userDao.updatePassword(user.getId(), PasswordUtil.hashPassword(password));
            }
            LoginRateLimiter.recordSuccess(rateLimitKey);
            mongoLogDao.saveActiveSession(session.getId(), user.getId());
            mongoLogDao.logSecurityEvent("login_success", user.getEmail(), SessionUtil.getClientIp(req), "User logged in.");

            if (SessionUtil.isAdminRole(user.getRole())) {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            } else {
                resp.sendRedirect(req.getContextPath() + "/user/dashboard");
            }
        } catch (SQLException ex) {
            throw new ServletException("Unable to login", ex);
        }
    }
}
