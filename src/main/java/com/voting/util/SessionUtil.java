package com.voting.util;

import com.voting.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class SessionUtil {
    private SessionUtil() {
    }

    public static User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }

    public static boolean hasRole(HttpServletRequest request, String role) {
        User user = getLoggedInUser(request);
        if (user == null || user.getRole() == null || role == null) {
            return false;
        }
        if ("admin".equalsIgnoreCase(role)) {
            return isAdminRole(user.getRole());
        }
        return role.equalsIgnoreCase(user.getRole());
    }

    public static boolean isAdminRole(String role) {
        return "admin".equalsIgnoreCase(role)
                || "super_admin".equalsIgnoreCase(role)
                || "election_officer".equalsIgnoreCase(role)
                || "auditor".equalsIgnoreCase(role);
    }

    public static boolean canManageElections(HttpServletRequest request) {
        User user = getLoggedInUser(request);
        return user != null && ("admin".equalsIgnoreCase(user.getRole())
                || "super_admin".equalsIgnoreCase(user.getRole())
                || "election_officer".equalsIgnoreCase(user.getRole()));
    }

    public static boolean isSuperAdmin(HttpServletRequest request) {
        User user = getLoggedInUser(request);
        return user != null && "super_admin".equalsIgnoreCase(user.getRole());
    }

    public static boolean canManageVoters(HttpServletRequest request) {
        User user = getLoggedInUser(request);
        return user != null && ("super_admin".equalsIgnoreCase(user.getRole())
                || "admin".equalsIgnoreCase(user.getRole()));
    }

    public static boolean canManageSettings(HttpServletRequest request) {
        return isSuperAdmin(request);
    }

    public static boolean canResolveComplaints(HttpServletRequest request) {
        return canManageVoters(request);
    }

    public static boolean canReviewNominations(HttpServletRequest request) {
        return canManageElections(request);
    }

    public static boolean canManageEligibility(HttpServletRequest request) {
        return canManageElections(request);
    }

    public static boolean isAuditor(HttpServletRequest request) {
        User user = getLoggedInUser(request);
        return user != null && "auditor".equalsIgnoreCase(user.getRole());
    }

    public static String getClientIp(HttpServletRequest req) {
        return ClientIpUtil.fromRequest(req);
    }
}
