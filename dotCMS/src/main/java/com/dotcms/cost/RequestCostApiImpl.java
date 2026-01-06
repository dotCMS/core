package com.dotcms.cost;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.cost.RequestPrices.Price;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * API for interacting with the request cost tracking system. This API is implemented as a singleton to ensure
 * consistent access to the request cost functionality throughout the application.
 */
@ApplicationScoped
public class RequestCostApiImpl implements RequestCostApi {


    final LongAdder requestCountForWindow = new LongAdder();
    final LongAdder requestCostForWindow = new LongAdder();
    private final LongAdder requestCountTotal = new LongAdder();
    private final LongAdder requestCostTotal = new LongAdder();
    private final Optional<Boolean> enableForTests;
    //log an accounting every X seconds
    private int requestCostTimeWindowSeconds;
    private ScheduledExecutorService scheduler;
    // make the request cost points look like $$
    private double requestCostDenominator = 1.0d;

    private final LeakyTokenBucket bucket = CDIUtils.getBeanThrows(LeakyTokenBucket.class);

    public RequestCostApiImpl() {
        enableForTests = Optional.empty();
    }


    public RequestCostApiImpl(Boolean enable) {
        enableForTests = Optional.ofNullable(enable);
    }

    @PostConstruct
    public void init() {
        this.requestCostTimeWindowSeconds = Config.getIntProperty("REQUEST_COST_TIME_WINDOW_SECONDS", 60);
        this.requestCostDenominator = Config.getFloatProperty("REQUEST_COST_DENOMINATOR", 1.0f);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread t = new Thread(r, "RequestCostMonitor");
                    t.setDaemon(true);
                    return t;
                }
        );
        // Start scheduled task to log current load
        scheduler.scheduleAtFixedRate(this::logRequestCost, requestCostTimeWindowSeconds,
                requestCostTimeWindowSeconds, TimeUnit.SECONDS);
    }

    private volatile boolean skipZeroRequests = false;
    private void logRequestCost() {
        try {
            if (!isAccountingEnabled()) {
                return;
            }

            long totalRequestsForDuration = this.requestCountForWindow.sumThenReset();
            double totalCostForDuration = this.requestCostForWindow.sumThenReset() / getRequestCostDenominator();
            double costPerRequestForDuration = totalRequestsForDuration == 0
                    ? 0
                    : totalCostForDuration / totalRequestsForDuration;

            if (totalRequestsForDuration == 0 && skipZeroRequests) {
                return;
            }
            skipZeroRequests = totalRequestsForDuration == 0;

            long totalRequestsTotal = requestCountTotal.longValue();
            double totalCostTotal = requestCostTotal.longValue() / getRequestCostDenominator();
            double costPerRequestTotal = requestCountTotal.longValue() == 0
                    ? 0
                    : requestCostTotal.longValue() / totalCostTotal;


            Logger.info("REQUEST COST MONITOR >",
                    String.format(
                            "Last %ds: Reqs: %d, Cost: %.2f, Avg Cost: %.2f | Totals: Reqs: %d, Cost: %.2f, Avg Cost: %.2f",
                            requestCostTimeWindowSeconds,
                            totalRequestsForDuration,
                            totalCostForDuration,
                            costPerRequestForDuration,
                            totalRequestsTotal,
                            totalCostTotal,
                            costPerRequestTotal));
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "Error logging request cost:" + e.getMessage(), e);
        }
    }


    @Override
    public double getRequestCostDenominator() {
        return requestCostDenominator;
    }



    @Override
    public List<Map<String, Object>> getAccountList(HttpServletRequest request) {

        List<Map<String, Object>> myList = (List<Map<String, Object>>) request.getAttribute(REQUEST_COST_ATTRIBUTE);
        if (myList == null) {
            myList = new ArrayList<>();
            request.setAttribute(REQUEST_COST_ATTRIBUTE, myList);
        }
        return myList;
    }


    private boolean isAccountingEnabled() {
        return enableForTests.orElse(Config.getBooleanProperty("REQUEST_COST_ACCOUNTING_ENABLED", true));
    }

    @Override
    public Accounting resolveAccounting() {
        return resolveAccounting(HttpServletRequestThreadLocal.INSTANCE.getRequest());
    }


    /**
     * resolves the Accounting mode from the request Accounting.Header is the default accounting mode. Can also be
     * Accounting.None   <-- No accounting Accounting.Log    <-- spits out accounting in the log Accounting.HTML   <--
     * skips request and spits out an html report
     *
     * @param request
     * @return
     */
    @Override
    public Accounting resolveAccounting(HttpServletRequest request) {
        if (request == null) {
            return Accounting.NONE;
        }
        Accounting optAccounting = (Accounting) request.getAttribute(REQUEST_COST_ACCOUNTING_TYPE);
        if (optAccounting != null && optAccounting instanceof Accounting) {
            return optAccounting;
        }
        Accounting accounting = _resolveAccounting(request);
        request.setAttribute(REQUEST_COST_ACCOUNTING_TYPE, accounting);
        return accounting;
    }


    /**
     * Internal method to resolve the accounting mode from the request
     *
     * @param request
     * @return
     */
    private Accounting _resolveAccounting(HttpServletRequest request) {
        if (!isAccountingEnabled()) {
            return Accounting.NONE;
        }
        if (request == null) {
            return Accounting.HEADER;
        }

        Accounting finalAccounting = request.getParameter(REQUEST_COST_ACCOUNTING_TYPE) != null
                ? Accounting.fromString(request.getParameter(REQUEST_COST_ACCOUNTING_TYPE))
                : request.getAttribute(REQUEST_COST_ACCOUNTING_TYPE) != null
                        ? (Accounting) request.getAttribute(REQUEST_COST_ACCOUNTING_TYPE)
                        : Accounting.HEADER;

        if (finalAccounting.ordinal() <= Accounting.HEADER.ordinal()) {
            return finalAccounting;
        }

        // only admins can get a full accounting report
        User user = PortalUtil.getUser(request) != null
                ? PortalUtil.getUser(request)
                : JsonWebTokenAuthCredentialProcessorImpl.getInstance().processAuthHeaderFromJWT(request);

        return user != null && user.isAdmin()
                ? finalAccounting
                : Accounting.HEADER;

    }


    @Override
    public void incrementCost(Price price, Method method, Object[] args) {
        Class clazz = method.getDeclaringClass();
        incrementCost(price, clazz, method.getName(), args);
    }


    @Override
    public void incrementCost(Price price, Class clazz, String method, Object[] args) {
        HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (request == null) {
            return;
        }
        Accounting accounting = resolveAccounting(request);

        if (accounting == Accounting.HTML) {
            Map<String, Object> load = createAccountingEntry(price, clazz, method, args, accounting);
            getAccountList(request).add(load);
        }

        String logMessage =
                "<--- REQUESTCOST price:" + price.price + " , method:" + clazz.getSimpleName() + "." + method;

        // log requests if a fuller accounting is enabled
        // Note: Cannot use lambdas with inline=true due to synthetic method access issues
        if (accounting.ordinal() > Accounting.HEADER.ordinal()) {
            Logger.info(RequestCostAdvice.class, logMessage);
        } else {
            Logger.debug(RequestCostAdvice.class, logMessage);
        }
        int currentCost = getRequestCost(request);
        if (currentCost == 0) {
            this.requestCountForWindow.increment();
            this.requestCountTotal.increment();
        }

        request.setAttribute(REQUEST_COST_RUNNING_TOTAL_ATTRIBUTE, currentCost + price.price);
        requestCostForWindow.add(price.price);
        requestCostTotal.add(price.price);
        bucket.drainFromBucket(price.price);
    }

    private Map<String, Object> createAccountingEntry(Price price, Class clazz, String method,
            Object[] args, Accounting accounting) {

        return Map.of(COST, price.price, METHOD, method, CLASS, clazz.getCanonicalName(), ARGS, args);

    }

    /**
     * Get the current cost for the request.
     *
     * @return The current cost value
     */
    @Override
    public int getRequestCost(HttpServletRequest request) {
        Integer runningTotal = (Integer) request.getAttribute(REQUEST_COST_RUNNING_TOTAL_ATTRIBUTE);

        return runningTotal == null ? 0 : runningTotal;
    }

    @Override
    public void initAccounting(HttpServletRequest request) {
        if (!isAccountingEnabled()) {
            return;
        }

        Accounting accounting = resolveAccounting(request);
        if (accounting.ordinal() > Accounting.HEADER.ordinal()) {
            Logger.info(this.getClass(), "<--- REQUESTCOST --- : " + request.getRequestURI());
        }

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        request.setAttribute(REQUEST_COST_ACCOUNTING_TYPE, accounting);


    }


    @Override
    public void endAccounting(HttpServletRequest request) {
        Accounting accounting = resolveAccounting(request);
        if (accounting.ordinal() > Accounting.HEADER.ordinal()) {
            Logger.info(this.getClass(),
                    "</--- REQUESTCOST TOTAL : " + request.getAttribute(REQUEST_COST_RUNNING_TOTAL_ATTRIBUTE));
        }
        request.removeAttribute(REQUEST_COST_ATTRIBUTE);
        request.removeAttribute(REQUEST_COST_ACCOUNTING_TYPE);
        request.removeAttribute(REQUEST_COST_RUNNING_TOTAL_ATTRIBUTE);
    }

    @Override
    public void addCostHeader(HttpServletRequest request, HttpServletResponse response) {
        if (!isAccountingEnabled()) {
            return;
        }
        Integer currentCost = getRequestCost(request);

        response.setHeader(REQUEST_COST_HEADER_NAME,
                String.format("%.2f", currentCost.doubleValue() / requestCostDenominator));

    }


    /**
     * Shutdown the scheduled executor service when the bean is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        Logger.debug(this.getClass(), "Shutting down request cost monitor");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
