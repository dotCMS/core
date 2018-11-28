package com.dotcms.filters.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Encapsulates the Result for a filter.
 * @author jsanca
 */
public class Result {

    private final Type                type;
    private final HttpServletRequest  request;
    private final HttpServletResponse response;

    /**
     * If the current interceptor wants to allow the call of the next interceptor, return NEXT.
     */
    public static final Result NEXT = new Builder().next().build();

    /**
     * If the current interceptor want to skip the next interceptors but wants to execute the filter chain call, return SKIP.
     */
    public static final Result SKIP = new Builder().skip().build();

    /**
     * If the current interceptor want to skip the next interceptor and in addition want to avoid the filter chain call, return SKIP_NO_CHAIN
     */
    public static final Result SKIP_NO_CHAIN = new Builder().skipNoChain().build();

    private Result(final Builder builder) {

        this.type     = builder.type;
        this.request  = builder.request;
        this.response = builder.response;
    }

    public Type getType() {
        return type;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public static final class Builder {
        private Type                type;
        private HttpServletRequest  request;
        private HttpServletResponse response;

        public Result.Builder next() {
            this.type = Type.NEXT;
            return this;
        }

        public Result.Builder skip() {
            this.type = Type.SKIP;
            return this;
        }

        public Result.Builder skipNoChain() {
            this.type = Type.SKIP_NO_CHAIN;
            return this;
        }

        public Result.Builder wrap(final HttpServletRequest  request) {
            this.request = request;
            return this;
        }

        public Result.Builder wrap(final HttpServletResponse response) {
            this.response = response;
            return this;
        }

        public Result build() {
            return new Result(this);
        }
    }

    enum Type {

        /**
         * If the current interceptor wants to allow the call of the next interceptor, return NEXT.
         */
        NEXT,
        /**
         * If the current interceptor want to skip the next interceptors but wants to execute the filter chain call, return SKIP.
         */
        SKIP,
        /**
         * If the current interceptor want to skip the next interceptor and in addition want to avoid the filter chain call, return SKIP_NO_CHAIN
         */
        SKIP_NO_CHAIN;

    }

} // E:O:F:Result.
