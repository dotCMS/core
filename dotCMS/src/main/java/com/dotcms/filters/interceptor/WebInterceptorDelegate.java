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
    DelegateResult intercept(final HttpServletRequest request,
                      final HttpServletResponse response) throws IOException;

    /**
     * Executes all interceptors, if some of them fails, stop the execution and returns false.
     * Otherwise true.
     *
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletResponse}
     */
    void after(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException;

    /**
     * Remove a {@link WebInterceptor}
     *
     * @param webInterceptorName name of the WebInterceptor to be remove
     * @param destroy if true the {@link WebInterceptor#destroy()} method is called also
     */
    void remove(final String webInterceptorName, final boolean destroy);

    /**
     * Change the {@link WebInterceptor} chain order.
     *
     * @param webInterceptorName {@link WebInterceptor} to be move
     * @param index index where the {@link WebInterceptor} has to be place
     */
    void move(final String webInterceptorName, int index);

    /**
     * Mpve a {@link WebInterceptor} to the first in the interceptors chain
     *
     * @param webInterceptorName webInterceptorName {@link WebInterceptor} to be move
     */
    void moveToFirst(final String webInterceptorName);

    /**
     * Mpve a {@link WebInterceptor} to the last in the interceptors chain
     *
     * @param webInterceptorName webInterceptorName {@link WebInterceptor} to be move
     */
    void moveToLast(final String webInterceptorName);

    /**
     * Set the order desire for the filter pipeline, see {@link OrderMode}
     * By default uses FILO
     * @param orderMode {@link OrderMode}
     */
    void orderMode(final OrderMode orderMode);

    /**
     * Encapsulates the delegate result, if shouldContinue
     * and also the request and the response (in case it was wrapped)
     */
    public class DelegateResult {

        private final boolean             shouldContinue;
        private final HttpServletRequest  request;
        private final HttpServletResponse response;

        public DelegateResult(final boolean shouldContinue,
                      final HttpServletRequest request,
                      final HttpServletResponse response) {

            this.shouldContinue = shouldContinue;
            this.request = request;
            this.response = response;
        }

        public boolean isShouldContinue() {
            return shouldContinue;
        }

        public HttpServletRequest getRequest() {
            return request;
        }

        public HttpServletResponse getResponse() {
            return response;
        }
    }
} // E:O:F:WebInterceptorDelegate.
