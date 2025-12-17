package com.dotcms.cost;

import com.dotcms.cost.RequestCostApi.Accounting;
import com.dotmarketing.business.APILocator;
import com.liferay.util.servlet.NullServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class RequestCostFilter implements Filter {


    private final RequestCostApi requestCostApi;
    private final LeakyTokenBucket bucket;

    public RequestCostFilter() {
        this(APILocator.getRequestCostAPI(), new LeakyTokenBucket());
    }

    RequestCostFilter(RequestCostApi requestCostApi) {
        this(requestCostApi, new LeakyTokenBucket());
    }


    RequestCostFilter(RequestCostApi requestCostApi, LeakyTokenBucket bucket) {
        this.requestCostApi = requestCostApi;
        this.bucket = bucket;

    }


    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        Accounting fullAccounting = requestCostApi.resolveAccounting(request);

        boolean allowed = bucket.allow();
        response.addHeader("X-dotRateLimit-Toks/Max", bucket.getTokenCount() + "/" + bucket.maximumBucketSize);

        if (!allowed) {
            response.sendError(429);
            return;
        }

        HttpServletResponse wrapper =
                fullAccounting == Accounting.HTML ? new NullServletResponse(response)
                        : new RequestCostResponseWrapper(request, response);
        requestCostApi.addCostHeader(request, wrapper);
        chain.doFilter(req, wrapper);
        requestCostApi.addCostHeader(request, wrapper);
        if (fullAccounting == Accounting.HTML) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.write(new RequestCostReport().writeAccounting(request));
        }
        bucket.drainFromBucket(requestCostApi.getRequestCost(request));


    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }


    class RequestCostResponseWrapper extends HttpServletResponseWrapper {

        HttpServletRequest request;

        public RequestCostResponseWrapper(HttpServletRequest request, HttpServletResponse response) throws IOException {
            super(response);
            this.request = request;

        }

        @Override
        public boolean containsHeader(String name) {
            if (RequestCostApi.REQUEST_COST_HEADER_NAME.equalsIgnoreCase(name)) {
                return true;
            }
            return super.containsHeader(name);
        }

        @Override
        public String getHeader(String name) {
            if (RequestCostApi.REQUEST_COST_HEADER_NAME.equalsIgnoreCase(name)) {
                return String.valueOf(requestCostApi.getRequestCost(request));
            }
            return super.getHeader(name);
        }

        @Override
        public Collection<String> getHeaders(String name) {
            if (RequestCostApi.REQUEST_COST_HEADER_NAME.equalsIgnoreCase(name)) {
                return List.of(String.valueOf(requestCostApi.getRequestCost(request)));
            }
            return super.getHeaders(name);
        }

        @Override
        public Collection<String> getHeaderNames() {
            Set<String> headers = new HashSet<>(super.getHeaderNames());
            headers.add(RequestCostApi.REQUEST_COST_HEADER_NAME);
            return headers;
        }


    }


}
