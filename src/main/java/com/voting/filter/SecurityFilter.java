package com.voting.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Advanced Security Filter to enforce modern security headers and policies.
 * Protects against XSS, Clickjacking, and MIME-sniffing.
 */
@WebFilter("/*")
public class SecurityFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Content Security Policy (CSP)
        // Restricts where scripts, styles, and images can be loaded from.
        // Allowing 'unsafe-inline' for now due to the high amount of inline styles/scripts in JSPs.
        // In a production app, we would move all scripts to external files and use a nonce.
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net; " +
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
            "font-src 'self' https://fonts.gstatic.com; " +
            "img-src 'self' data: blob:; " +
            "media-src 'self' blob:; " +
            "connect-src 'self' ws: wss: https://cdn.jsdelivr.net; " +
            "frame-ancestors 'none';");

        // 2. Strict-Transport-Security (HSTS)
        // Disabled for localhost development to prevent automatic HTTPS redirection.
        // httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // 3. X-Frame-Options
        // Prevents Clickjacking by disallowing the site to be embedded in an iframe.
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 4. X-Content-Type-Options
        // Temporarily disabled to allow browser to sniff MIME types while server config is fixed.
        // httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 5. X-XSS-Protection
        // Enables the browser's built-in XSS filter.
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 6. Referrer-Policy
        // Controls how much referrer information is passed to other sites.
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 7. Permissions-Policy
        // Camera is required for voter face verification. Keep other unused features disabled.
        httpResponse.setHeader("Permissions-Policy", "camera=(self), microphone=(), geolocation=(), payment=()");

        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = contextPath == null || contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
        if (!isStaticResource(path)) {
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setDateHeader("Expires", 0);
        }

        chain.doFilter(request, response);
    }

    private boolean isStaticResource(String path) {
        return path.startsWith("/assets/")
                || path.startsWith("/uploads/")
                || "/manifest.json".equals(path)
                || "/sw.js".equals(path);
    }

    @Override
    public void destroy() {}
}
