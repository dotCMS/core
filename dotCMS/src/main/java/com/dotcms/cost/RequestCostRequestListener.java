package com.dotcms.cost;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.business.APILocator;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

public class RequestCostRequestListener implements ServletRequestListener {

    private final RequestCostApi requestCostApi = APILocator.getRequestCostAPI();


    @Override
    public void requestInitialized(ServletRequestEvent servletRequestEvent) {
        HttpServletRequest request = (HttpServletRequest) servletRequestEvent.getServletRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        boolean fullAccounting = requestCostApi.isFullAccounting(request);

        requestCostApi.initAccounting(request, fullAccounting);
    }


    @Override
    public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
        HttpServletRequest request = (HttpServletRequest) servletRequestEvent.getServletRequest();
        requestCostApi.endAccounting(request);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }


}
