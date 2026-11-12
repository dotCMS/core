package com.dotcms.auth;

import com.dotcms.api.web.HttpServletRequestThreadLocal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Installs the request and session wrapper of {@link InteractiveIdentityWrapper} for the rest of the chain
 * and makes it the thread-bound request, so code that reads the request through
 * {@link HttpServletRequestThreadLocal} sees the same view.
 * <p>
 * It must be mapped before any filter or listener that resolves the user.
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
        if (!(request instanceof HttpServletRequest) || !InteractiveUserPolicy.isRuleOn()) {
            chain.doFilter(request, response);
            return;
        }
        final HttpServletRequest wrapped = InteractiveIdentityWrapper.wrap((HttpServletRequest) request);
        final HttpServletRequest previous = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(wrapped);
        try {
            chain.doFilter(wrapped, response);
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previous);
        }
    }
}
