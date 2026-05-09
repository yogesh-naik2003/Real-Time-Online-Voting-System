package com.voting.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/assets/*")
public class MimeTypeFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().toLowerCase();
        System.out.println("[MimeFilter] URI: " + httpRequest.getRequestURI());

        if (path.endsWith(".css")) {
            httpResponse.setContentType("text/css");
        } else if (path.endsWith(".js")) {
            httpResponse.setContentType("application/javascript");
        } else if (path.endsWith(".png")) {
            httpResponse.setContentType("image/png");
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            httpResponse.setContentType("image/jpeg");
        } else if (path.endsWith(".svg")) {
            httpResponse.setContentType("image/svg+xml");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
