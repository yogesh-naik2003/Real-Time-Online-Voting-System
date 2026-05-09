package com.voting.filter;

import com.voting.util.CsrfUtil;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(urlPatterns = {"/*"})
public class CsrfFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = contextPath == null || contextPath.isEmpty() ? uri : uri.substring(contextPath.length());

        // Skip CSRF check for GET requests
        if ("GET".equalsIgnoreCase(httpRequest.getMethod())) {
            if ("/login".equals(path) || "/register".equals(path)) {
                CsrfUtil.rotateToken(httpRequest);
            } else {
                CsrfUtil.getToken(httpRequest);
            }
            chain.doFilter(request, response);
            return;
        }

        if ("POST".equalsIgnoreCase(httpRequest.getMethod())) {
            if ("/login".equals(path)) {
                chain.doFilter(request, response);
                return;
            }

            String contentType = httpRequest.getContentType();
            boolean isMultipart = contentType != null && contentType.toLowerCase().contains("multipart/form-data");
            
            // Skip check for multipart for now (since getParameter doesn't work for them)
            if (isMultipart) {
                chain.doFilter(request, response);
                return;
            }

            if (!CsrfUtil.isValid(httpRequest)) {
                HttpSession session = httpRequest.getSession(false);
                String sessionToken = (session != null) ? (String) session.getAttribute("csrfToken") : "NO_SESSION";
                String paramToken = httpRequest.getParameter("csrfToken");
                String headerToken = httpRequest.getHeader("X-CSRF-Token");
                
                System.err.println("[CSRF] Validation failed for: " + httpRequest.getRequestURI());
                System.err.println("[CSRF] Session Token: " + sessionToken);
                System.err.println("[CSRF] Parameter Token: " + paramToken);
                System.err.println("[CSRF] Header Token: " + headerToken);
                
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token. Please refresh the page and try again.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
