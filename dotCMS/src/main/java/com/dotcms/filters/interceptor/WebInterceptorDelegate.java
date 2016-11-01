package com.dotcms.filters.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Encapsulates the functionality for a Web Interceptor delegate
 * @author jsanca
 */
public interface WebInterceptorDelegate extends WebInterceptorAware {

    /**
     * Call me on destroy
     */
    void destroy();


    /**
     * Call me on init
     */
    void init();

    /**
     * Executes all interceptors, if some of them fails, stop the execution and returns false.
     * Otherwise true.
     *
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     * @return boolean If the filter chain needs to continue after the execution
     *         of the interceptor, returns {@code true}. Otherwise, returns,
     *         {@code false}.
     */
    boolean intercept(final HttpServletRequest request,
                      final HttpServletResponse response) throws IOException;

} // E:O:F:WebInterceptorDelegate.
