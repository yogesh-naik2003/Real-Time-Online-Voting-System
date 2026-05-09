package com.voting.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.voting.util.ClientIpUtil;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced Rate Limiting Filter to prevent DDoS and Brute-Force attacks.
 * Tracks requests per IP address.
 */
@WebFilter("/*")
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 180;
    private final Map<String, IPRequestInfo> requestCounts = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String path = contextPath == null || contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
        if (isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        String ipAddress = getClientIp(httpRequest);
        long currentTime = System.currentTimeMillis();

        requestCounts.entrySet().removeIf(entry -> currentTime - entry.getValue().lastReset > 60000);

        IPRequestInfo info = requestCounts.computeIfAbsent(ipAddress, k -> new IPRequestInfo(currentTime));

        if (currentTime - info.lastReset > 60000) {
            info.count.set(0);
            info.lastReset = currentTime;
        }

        if (info.count.incrementAndGet() > MAX_REQUESTS_PER_MINUTE) {
            httpResponse.setStatus(429); // Too Many Requests
            httpResponse.getWriter().write("Too many requests. Please try again in a minute.");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        return ClientIpUtil.fromRequest(request);
    }

    private boolean isStaticResource(String path) {
        return path.startsWith("/assets/")
                || path.startsWith("/uploads/")
                || "/manifest.json".equals(path)
                || "/sw.js".equals(path)
                || path.endsWith(".ico");
    }

    @Override
    public void destroy() {}

    private static class IPRequestInfo {
        AtomicInteger count = new AtomicInteger(0);
        long lastReset;

        IPRequestInfo(long lastReset) {
            this.lastReset = lastReset;
        }
    }
}
