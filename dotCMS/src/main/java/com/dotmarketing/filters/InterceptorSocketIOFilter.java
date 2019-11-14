package com.dotmarketing.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class InterceptorSocketIOFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        httpResponse.sendRedirect("https://localhost:3000/" + httpRequest.getRequestURI() + "?" + httpRequest.getQueryString());
    }

    @Override
    public void destroy() {

    }
}
