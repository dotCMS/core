package com.dotcms.cost;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private int requestCostDenominator;

    public RequestCostApiImpl() {
        // CDI requires a no-arg constructor
    }

    @PostConstruct
    public void init() {
        this.requestCostTimeWindowSeconds = Config.getIntProperty("REQUEST_COST_TIME_WINDOW_SECONDS", 60);
        this.requestCostDenominator = Config.getIntProperty("REQUEST_COST_DENOMINATOR", 100);

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


    private void logRequestCost() {
        try {
            if (!isAccountingEnabled()) {
                return;
            }
            Tuple2<Long, Long> load = totalLoadGetAndReset();
            if (load._1 == 0) {
                return;
            }
            Logger.info(this.getClass(),
                    String.format(
                            "Request Cost Monitor: Window: %ds, Count: %d, Total Cost: %.2f¢, Cost/Request: %.2f¢",
                            requestCostTimeWindowSeconds,
                            load._1, load._2.doubleValue() / requestCostDenominator,
                            load._2.doubleValue() / load._1 / requestCostDenominator));
        } catch (Exception e) {
            Logger.error(this.getClass(), "Error logging request cost", e);
        }
    }


    @Override
    public int getRequestCostDenominator() {
        return requestCostDenominator;
    }


    @Override
    public Tuple2<Long, Long> totalLoadGetAndReset() {
        return new Tuple2<>(requestCountForWindow.getAndSet(0), requestCostForWindow.getAndSet(0));
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


    @Override
    public boolean isFullAccounting(HttpServletRequest request) {
        if (request.getAttribute(REQUEST_COST_FULL_ACCOUNTING) == null) {
            request.setAttribute(REQUEST_COST_FULL_ACCOUNTING, fullAccounting(request));
        }
        return request.getAttribute(REQUEST_COST_FULL_ACCOUNTING) == null
                ? false
                : (Boolean) request.getAttribute(REQUEST_COST_FULL_ACCOUNTING);


    }

    private boolean fullAccounting(HttpServletRequest request) {

        if ("true".equalsIgnoreCase(request.getParameter(RequestCostApi.REQUEST_COST_FULL_ACCOUNTING))) {

            User user = Try.of(() -> new InitBuilder(request, null).requireAdmin(true).init().getUser()).getOrNull();
            return (user != null);

        }
        return false;
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
        boolean fullAccounting = isFullAccounting(request);
        Map<String, Object> load = fullAccounting
                ? Map.of(COST, price.price, METHOD, method, CLASS, clazz, ARGS, args)
                : Map.of(COST, price.price);
        getAccountList(request).add(load);
        requestCostForWindow.accumulateAndGet(price.price, Math::addExact);

    }

    boolean isAccountingEnabled() {
        return Config.getBooleanProperty("REQUEST_COST_ACCOUNTING_ENABLED", false);

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
        this.initAccounting(request, false);
    }

    @Override
    public void initAccounting(HttpServletRequest request, boolean fullAccounting) {
        if (!isAccountingEnabled()) {
            return;
        }
        Logger.debug(this.getClass(), "<Starting request cost accounting---");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        request.setAttribute(REQUEST_COST_ATTRIBUTE, new ArrayList<>());
        if (fullAccounting) {
            request.setAttribute(REQUEST_COST_FULL_ACCOUNTING, true);
        }
        requestCountForWindow.incrementAndGet();
        this.incrementCost(Price.COSTING_INIT, RequestCostApi.class, "initAccounting",
                new Object[]{request, fullAccounting});
    }


    @Override
    public void endAccounting(HttpServletRequest request) {
        Logger.debug(this.getClass(), "</Ending request cost accounting  ---");
        request.removeAttribute(REQUEST_COST_ATTRIBUTE);
        request.removeAttribute(REQUEST_COST_FULL_ACCOUNTING);
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
