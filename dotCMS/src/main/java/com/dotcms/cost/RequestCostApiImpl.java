package com.dotcms.cost;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.cost.RequestPrices.Price;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.Tuple2;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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


    private final AtomicLong requestCountForWindow = new AtomicLong(0);
    private final AtomicLong requestCostForWindow = new AtomicLong(0);
    //log an accounting every X seconds
    private int requestCostTimeWindowSeconds;
    private ScheduledExecutorService scheduler;

    // make the request cost points look like $$
    private float requestCostDenominator = 1;

    private final Optional<Boolean> enableForTests;
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
            Tuple2<Long, Long> load = totalLoadGetAndReset();

            long totalRequests = load._1;
            float totalCost = load._2 / getRequestCostDenominator();
            float costPerRequest = totalRequests == 0
                    ? 0
                    : totalCost / totalRequests;

            if (totalRequests == 0 && skipZeroRequests) {
                return;
            }

            skipZeroRequests = totalRequests == 0;

            Logger.info("REQUEST COST MONITOR >",
                    String.format(
                            "Window: %ds, Count: %d, Total Cost: %.2f, Cost/Request: %.2f",
                            requestCostTimeWindowSeconds,
                            totalRequests,
                            totalCost,
                            costPerRequest));
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error logging request cost", e);
        }
    }


    @Override
    public float getRequestCostDenominator() {
        return requestCostDenominator;
    }


    @Override
    public Tuple2<Long, Long> totalLoadGetAndReset() {
        return new Tuple2<>(this.requestCountForWindow.getAndSet(0), this.requestCostForWindow.getAndSet(0));
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
        return enableForTests.orElse(Config.getBooleanProperty("REQUEST_COST_ACCOUNTING_ENABLED", false));
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
        Map<String, Object> load = createCostEntry(price, clazz, method, args, accounting);

        getAccountList(request).add(load);
        requestCostForWindow.accumulateAndGet(price.price, Math::addExact);
    }

    private Map<String, Object> createCostEntry(Price price, Class clazz, String method,
            Object[] args, Accounting accounting) {
        if (accounting == Accounting.HTML) {
            return Map.of(COST, price.price, METHOD, method, CLASS, clazz.getCanonicalName(), ARGS, args);
        }
        return Map.of(COST, price.price);
    }



    /**
     * Get the current cost for the request.
     *
     * @return The current cost value
     */
    @Override
    public int getRequestCost(HttpServletRequest request) {
        return getAccountList(request).stream().mapToInt(m -> (int) m.get(COST)).sum();
    }


    @Override
    public void initAccounting(HttpServletRequest request) {
        if (!isAccountingEnabled()) {
            return;
        }

        // Increment request counter for monitoring window
        this.requestCountForWindow.incrementAndGet();

        Accounting accounting = resolveAccounting(request);
        Logger.debug(this.getClass(), "<Starting request cost accounting---");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        request.setAttribute(REQUEST_COST_ATTRIBUTE, new ArrayList<>());
        request.setAttribute(REQUEST_COST_ACCOUNTING_TYPE, accounting);

        this.incrementCost(Price.COSTING_INIT, RequestCostApiImpl.class, "initAccounting",
                new Object[]{request, accounting});
    }


    @Override
    public void endAccounting(HttpServletRequest request) {
        Logger.debug(this.getClass(), "</Ending request cost accounting  ---");
        request.removeAttribute(REQUEST_COST_ATTRIBUTE);
        request.removeAttribute(REQUEST_COST_ACCOUNTING_TYPE);
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
