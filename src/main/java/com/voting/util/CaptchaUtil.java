package com.voting.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.SecureRandom;

public class CaptchaUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateCaptcha(HttpServletRequest request) {
        int a = RANDOM.nextInt(10) + 1;
        int b = RANDOM.nextInt(10) + 1;
        int result = a + b;
        
        HttpSession session = request.getSession();
        session.setAttribute("captchaResult", String.valueOf(result));
        
        return a + " + " + b;
    }

    public static boolean verifyCaptcha(HttpServletRequest request, String input) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        
        String actual = (String) session.getAttribute("captchaResult");
        session.removeAttribute("captchaResult"); // Use once
        
        return actual != null && actual.equals(input);
    }
}
