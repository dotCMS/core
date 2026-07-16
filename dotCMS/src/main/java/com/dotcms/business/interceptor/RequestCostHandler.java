package com.dotcms.business.interceptor;

import com.dotcms.cost.RequestCostApi;
import com.dotcms.cost.RequestPrices.Price;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;

import java.lang.reflect.Method;

/**
 * Shared handler for {@code @RequestCost} logic. Used by both the ByteBuddy advice and the
 * CDI interceptor to keep the implementation DRY.
 */
public final class RequestCostHandler {

    private RequestCostHandler() { }

    /**
     * Increments the request cost for the given method invocation.
     *
     * @param price  the cost to add
     * @param method the method being invoked
     * @param args   the method arguments
     */
    public static void incrementCost(final Price price, final Method method,
                                     final Object[] args) {
        try {
            final RequestCostApi api = APILocator.getRequestCostAPI();
            api.incrementCost(price, method, args);
        } catch (Throwable t) {
            Logger.warnAndDebug(RequestCostHandler.class,
                    "Error in RequestCostHandler.incrementCost(): " + t.getMessage(), t);
        }
    }
}