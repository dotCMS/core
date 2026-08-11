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

    /**
     * Increments the request cost by {@code price * times} from a call site that charges
     * directly rather than through the {@code @RequestCost} annotation.
     * <p>
     * Use this rather than calling {@code APILocator.getRequestCostAPI().incrementCost(..)}
     * inline. The annotation path is protected — {@code RequestCostAdvice.enter} is declared
     * {@code @Advice.OnMethodEnter(suppress = Throwable.class)} — so a failure in the cost API
     * can never break the method being metered. A direct call has no such protection, and
     * these charge points sit on page rendering and content loading: metering must never be
     * able to take down serving.
     *
     * @param price  the unit price
     * @param clazz  calling class
     * @param method calling method
     * @param args   arguments, for the HTML accounting report
     * @param times  how many units of work were done
     */
    public static void incrementCost(final Price price, final Class clazz, final String method,
                                     final Object[] args, final int times) {
        try {
            APILocator.getRequestCostAPI().incrementCost(price, clazz, method, args, times);
        } catch (Throwable t) {
            Logger.warnAndDebug(RequestCostHandler.class,
                    "Error in RequestCostHandler.incrementCost(): " + t.getMessage(), t);
        }
    }

    /**
     * Convenience overload charging a single unit. See
     * {@link #incrementCost(Price, Class, String, Object[], int)}.
     */
    public static void incrementCost(final Price price, final Class clazz, final String method,
                                     final Object[] args) {
        incrementCost(price, clazz, method, args, 1);
    }
}