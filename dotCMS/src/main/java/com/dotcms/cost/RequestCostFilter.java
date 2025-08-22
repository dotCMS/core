
package com.dotcms.cost;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class RequestCostFilter implements Filter {

  private final RequestCostApi requestCostApi = RequestCostApi.getInstance();

  /**
   * Increment the current request's cost in a thread-safe manner.
   */
  public static void incrementCost(int delta) {
    RequestCostApi.getInstance().incrementCost(delta);
  }

  /**
   * Optionally use to get the current cost mid-request.
   */
  public static int getCurrentCost() {
    return RequestCostApi.getInstance().getCurrentCost();
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    try {
      // Ensure cost is reset at the start of each request
      requestCostApi.initAccounting();
      chain.doFilter(req, res);
      requestCostApi.addCostHeader((HttpServletResponse) res);

    } finally {
      // Prevent memory leaks
      requestCostApi.removeCost();
    }
  }

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void destroy() {
  }
}
