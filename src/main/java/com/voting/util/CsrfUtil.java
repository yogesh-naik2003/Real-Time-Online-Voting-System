package com.voting.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfUtil {
    public static final String PARAMETER_NAME = "csrfToken";
    private static final String SESSION_ATTRIBUTE = "csrfToken";
    private static final String PREVIOUS_SESSION_ATTRIBUTE = "previousCsrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    public static String getToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String token = (String) session.getAttribute(SESSION_ATTRIBUTE);
        if (token == null) {
            token = newToken();
            session.setAttribute(SESSION_ATTRIBUTE, token);
        }
        return token;
    }

    public static String rotateToken(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String current = (String) session.getAttribute(SESSION_ATTRIBUTE);
        if (current != null) {
            session.setAttribute(PREVIOUS_SESSION_ATTRIBUTE, current);
        }
        String token = newToken();
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String expected = (String) session.getAttribute(SESSION_ATTRIBUTE);
        String previous = (String) session.getAttribute(PREVIOUS_SESSION_ATTRIBUTE);
        String actual = request.getParameter(PARAMETER_NAME);
        if (actual == null || actual.isEmpty()) {
            actual = request.getHeader("X-CSRF-Token");
        }
        return actual != null
                && ((expected != null && constantTimeEquals(expected, actual))
                || (previous != null && constantTimeEquals(previous, actual)));
    }

    public static String hiddenInput(HttpServletRequest request) {
        return "<input type=\"hidden\" name=\"" + PARAMETER_NAME + "\" value=\"" + getToken(request) + "\">";
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] left = expected.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int result = left.length ^ right.length;
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            byte a = i < left.length ? left[i] : 0;
            byte b = i < right.length ? right[i] : 0;
            result |= a ^ b;
        }
        return result == 0;
    }
}
