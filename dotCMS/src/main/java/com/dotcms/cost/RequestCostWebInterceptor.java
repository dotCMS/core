package com.dotcms.cost;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestCostWebInterceptor implements WebInterceptor {

  private final RequestCostApi requestCostApi = RequestCostApi.getInstance();

  @Override
  public boolean afterIntercept(HttpServletRequest req, HttpServletResponse res) {

    requestCostApi.addCostHeader(res);

    return true;
  }

  @Override
  public Result intercept(HttpServletRequest req, HttpServletResponse res) throws IOException {
    requestCostApi.initAccounting();
    return Result.NEXT;
  }
}
