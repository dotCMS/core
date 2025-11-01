package com.dotcms.cost;



import com.dotcms.cost.RequestPrices.Price;
import io.vavr.Tuple2;
import io.vavr.control.Try;
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

    enum Accounting {
        NONE,
        HEADER,
        LOG,
        HTML;

        public static Accounting fromString(String text) {
            return Try.of(() -> Accounting.valueOf(text.toUpperCase())).getOrElse(Accounting.HEADER);
        }
    }

    // Reqye
    String REQUEST_COST_HEADER_NAME = "X-Request-Cost";

    String REQUEST_COST_ATTRIBUTE = "dotRequestCost";

    // this is the url parameter that needs to be set in order to show an accounting report
    String REQUEST_COST_ACCOUNTING_TYPE = "dotAccounting";

    // Map Keys for full Accounting
    String COST = "cost";
    String METHOD = "method";
    String CLASS = "class";
    String ARGS = "args";


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
     * Returns the mode set for accounting
     *
     * @param request
     * @return
     */
    Accounting resolveAccounting(@NotNull HttpServletRequest request);

    /**
     * Reolves Accounting using the ThreadLocal request
     *
     * @return
     */
    Accounting resolveAccounting();

    /**
     * Adds Price to the requests cost
     *
     * @param price
     * @param method
     * @param args
     */
    void incrementCost(@NotNull Price price, @NotNull Method method, @NotNull Object[] args);


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
     * Clears the accounting system for the current request.
     */
    void endAccounting(@NotNull HttpServletRequest request);

    /**
     * Adds the current cost to the response header.
     *
     * @param response
     */
    void addCostHeader(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response);

}
