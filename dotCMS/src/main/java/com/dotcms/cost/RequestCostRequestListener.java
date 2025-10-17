package com.dotcms.cost;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

public class RequestCostRequestListener implements ServletRequestListener {

    private final RequestCostApi requestCostApi;


    public RequestCostRequestListener(RequestCostApi requestCostApi) {
        this.requestCostApi = requestCostApi;
    }

    public RequestCostRequestListener() {
        this.requestCostApi = new RequestCostApiImpl();
    }


    @Override
    public void requestInitialized(ServletRequestEvent servletRequestEvent) {
        HttpServletRequest request = (HttpServletRequest) servletRequestEvent.getServletRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        requestCostApi.initAccounting(request);
    }


    @Override
    public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
        HttpServletRequest request = (HttpServletRequest) servletRequestEvent.getServletRequest();
        requestCostApi.endAccounting(request);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);
    }


}
