package com.voting.filter;

import com.voting.util.SessionUtil;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(urlPatterns = {"/admin/*", "/user/*"})
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        if (path.startsWith("/admin/")) {
            if (SessionUtil.getLoggedInUser(httpRequest) == null) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            } else if (!SessionUtil.hasRole(httpRequest, "admin")) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: You do not have administrator privileges.");
            } else {
                chain.doFilter(request, response);
            }
            return;
        }
        if (path.startsWith("/user/")) {
            if (SessionUtil.getLoggedInUser(httpRequest) == null) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            } else if (!SessionUtil.hasRole(httpRequest, "user")) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Voter role required.");
            } else {
                chain.doFilter(request, response);
            }
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
