package com.dotcms.cost;


import com.dotcms.cost.RequestPrices.Price;
import io.vavr.Tuple2;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;


/**
 * API for interacting with the request cost tracking system. This API is implemented as a singleton to ensure
 * consistent access to the request cost functionality throughout the application.
 */
public interface RequestCostApi {

    // Reqye
    public final static String REQUEST_COST_HEADER_NAME = "X-Request-Cost";

    public final static String REQUEST_COST_ATTRIBUTE = "dotRequestCost";

    // this is the url parameter that needs to be set in order to show an accounting report
    public final static String REQUEST_COST_FULL_ACCOUNTING = "dotRequestAccounting";

    // Map Keys for full Accounting
    public final static String COST = "cost";
    public final static String METHOD = "method";
    public final static String CLASS = "class";
    public final static String ARGS = "args";


    float getRequestCostDenominator();

    /**
     * Returns the current load of the system. The first value is the current number of requests in the system. The
     * second value is the sum of the current cost for all the requests.
     *
     * @return
     */
    Tuple2<Long, Long> totalLoadGetAndReset();

    /**
     * Returns the current Accounting list for the request
     *
     * @param request
     * @return
     */
    List<Map<String, Object>> getAccountList(@NotNull HttpServletRequest request);

    /**
     * Returns true if the request is in full accounting mode
     *
     * @param request
     * @return
     */
    boolean isFullAccounting(@NotNull HttpServletRequest request);


    void incrementCost(Price price, Method method, Object[] args);


    /**
     * Increment the cost for the current request
     *
     * @param price
     * @param clazz
     * @param method
     * @param args
     */
    void incrementCost(Price price, @NotNull Class clazz, @NotNull String method, @NotNull Object[] args);

    /**
     * Returns the current cost for the current request.
     *
     * @return
     */
    int getRequestCost(@NotNull HttpServletRequest request);

    /**
     * Initializes the accounting system for the current request with fullAccounting set to false
     */
    void initAccounting(@NotNull HttpServletRequest request);

    /**
     * Initializes the accounting system for the current request.
     *
     * @param request
     * @param fullAccounting
     */
    void initAccounting(@NotNull HttpServletRequest request, boolean fullAccounting);

    /**
     * Clears the accounting system for the current request.
     */
    void endAccounting(@NotNull HttpServletRequest request);

    /**
     * Adds the current cost to the response header.
     *
     * @param response
     */
    void addCostHeader(@NotNull HttpServletRequest request, HttpServletResponse response);

}
