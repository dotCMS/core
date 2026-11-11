package com.dotcms.auth;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Installs the request and session wrapper. Stub for the Red gate: no behavior yet.
 */
public class InteractiveIdentityFilter implements Filter {

    @Override
    public void init(final javax.servlet.FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(request, response);
    }
}
