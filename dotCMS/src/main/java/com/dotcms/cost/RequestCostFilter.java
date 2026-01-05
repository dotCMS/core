package com.dotcms.cost;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.cost.RequestCostApi.Accounting;
import com.dotmarketing.business.APILocator;
import com.liferay.util.servlet.NullServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestCostFilter implements Filter {


    private final RequestCostApi requestCostApi;
    private final LeakyTokenBucket bucket = CDIUtils.getBeanThrows(LeakyTokenBucket.class);






    public RequestCostFilter() {
        this(APILocator.getRequestCostAPI());
    }


    RequestCostFilter(RequestCostApi requestCostApi) {
        this.requestCostApi = requestCostApi;
    }


    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        Accounting fullAccounting = requestCostApi.resolveAccounting(request);

        boolean allowed = bucket.allow();
        response.addHeader(RequestCostApi.REQUEST_COST_HEADER_TOKEN_MAX,
                bucket.getTokenCount() + "/" + bucket.getMaximumBucketSize());

        if (!allowed) {
            response.sendError(429);
            return;
        }

        HttpServletResponse wrapper = fullAccounting == Accounting.HTML
                ? new NullServletResponse(response)
                : response;
        requestCostApi.addCostHeader(request, wrapper);
        chain.doFilter(req, wrapper);
        requestCostApi.addCostHeader(request, wrapper);
        if (fullAccounting == Accounting.HTML) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter out = response.getWriter();
            out.write(new RequestCostReport().writeAccounting(request));
        }


    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }




}
