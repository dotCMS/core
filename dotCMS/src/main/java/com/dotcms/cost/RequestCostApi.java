package com.dotcms.cost;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import io.vavr.Lazy;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * API for interacting with the request cost tracking system. This API is implemented as a singleton to ensure
 * consistent access to the request cost functionality throughout the application.
 */
public class RequestCostApi {

  private static final String COST_HEADER_NAME = "X-Request-Cost";
  private static final ThreadLocal<Integer> requestCost = ThreadLocal.withInitial(() -> -1);

  // Singleton instance
  private static final Lazy<RequestCostApi> INSTANCE = Lazy.of(RequestCostApi::new);

  /**
   * Private constructor to prevent direct instantiation.
   */
  private RequestCostApi() {
    // Private constructor to enforce singleton pattern
  }

  /**
   * Get the singleton instance of the RequestCostApi.
   *
   * @return The singleton instance
   */
  public static RequestCostApi getInstance() {
    return INSTANCE.get();
  }

  /**
   * Increment the current request's cost in a thread-safe manner.
   *
   * @param delta The cost to add
   */
  public void incrementCost(int delta) {
    if (requestCost.get() < 0) {
      return;
    }

    requestCost.set(requestCost.get() + delta);

    // set as a request attribute as well.
    if (HttpServletRequestThreadLocal.INSTANCE.getRequest() != null) {
      HttpServletRequestThreadLocal.INSTANCE.getRequest().setAttribute("dotRequestCost", requestCost.get());
    }

  }

  /**
   * Get the current cost for the request.
   *
   * @return The current cost value
   */
  public int getCurrentCost() {
    return requestCost.get();
  }

  public void initAccounting() {
    requestCost.set(0);
  }

  public void removeCost() {
    requestCost.remove();
  }

  public void addCostHeader(ServletResponse response) {
    if (!HttpServletResponse.class.isAssignableFrom(response.getClass())) {
      return;
    }
    try {
      if (requestCost.get() < 0 || response.isCommitted()
          || ((HttpServletResponse) response).getHeader(COST_HEADER_NAME) != null) {
        return;
      }
      ((HttpServletResponse) response).setHeader(COST_HEADER_NAME, String.valueOf(getCurrentCost()));
    } finally {
      removeCost();
    }

  }


}
