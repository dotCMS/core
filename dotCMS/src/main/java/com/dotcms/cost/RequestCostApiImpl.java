package com.dotcms.cost;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenAuthCredentialProcessorImpl;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.cost.RequestPrices.Price;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.PortalUtil;
import io.vavr.control.Try;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    // Cost incurred with no HttpServletRequest on the thread: site-search reindexing,
    // scheduled publishing, remote/push publishing, content indexing, embedding generation.
    // This is real, billable work - it just has no request to attach to. Before these
    // counters existed incrementCost returned early and the cost vanished from the totals
    // entirely, so none of it reached the collector.
    private final LongAdder jobCostForWindow = new LongAdder();
    private final LongAdder jobCostTotal = new LongAdder();
    private final Optional<Boolean> enableForTests;
    //log an accounting every X seconds
    private int requestCostTimeWindowSeconds;
    private ScheduledExecutorService scheduler;
    // make the request cost points look like $$
    private double requestCostDenominator = 1.0d;

    private final LeakyTokenBucket bucket = CDIUtils.getBeanThrows(LeakyTokenBucket.class);
    private final RequestCostPublisher publisher = CDIUtils.getBeanThrows(RequestCostPublisher.class);

    public RequestCostApiImpl() {
        enableForTests = Optional.empty();
    }


    public RequestCostApiImpl(Boolean enable) {
        enableForTests = Optional.ofNullable(enable);
    }

    @PostConstruct
    public void init() {
        this.requestCostTimeWindowSeconds = Config.getIntProperty("REQUEST_COST_TIME_WINDOW_SECONDS", 300);
        // Clamp to >= 1.0 so a misconfigured 0 doesn't produce Infinity/NaN in the snapshot —
        // those serialize as JSON-invalid literals and break strict parsers on the collector side.
        // Default of 10 keeps reported tokens in the range they were before the Price table
        // was re-based on resource-time: internally 1 unit is now an in-memory cache read and
        // one DB round trip is 10, so dividing by 10 makes a reported token ~= one DB query,
        // which is roughly what a token meant under the old table.
        this.requestCostDenominator = Math.max(1.0d,
                Config.getFloatProperty("REQUEST_COST_DENOMINATOR", 10.0f));

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

    private static String nullSafe(final String value) {
        return UtilMethods.isSet(value) ? value : "unknown";
    }

    private void logRequestCost() {
        try {
            if (!isAccountingEnabled()) {
                return;
            }

            // The four counter reads below are not atomic relative to each other. Increments
            // landing between them are counted in the next window's snapshot but already in
            // the lifetime totals — Σ(window) can briefly trail lifetime by a few requests.
            // Intentional: observational telemetry, atomic snapshot would need a lock.
            final long totalRequestsForDuration = this.requestCountForWindow.sumThenReset();
            // Token totals are rounded to whole numbers. They were always integral while the
            // denominator was 1, and a collector that has been parsing them as ints would break
            // on a fractional value. The per-request averages stay fractional — they always were.
            final double requestCostForDuration = Math.round(
                    this.requestCostForWindow.sumThenReset() / getRequestCostDenominator());
            final double jobCostForDuration = Math.round(
                    this.jobCostForWindow.sumThenReset() / getRequestCostDenominator());

            // windowTokens stays request-only, exactly as it has always been. Background work
            // is reported alongside it in windowJobTokens rather than folded in, so every
            // field keeps a single meaning and windowTokens / windowRequests still agrees with
            // windowAvgTokensPerRequest. Total cluster consumption is the sum of the two, and
            // the collector is where that sum belongs.
            final double totalCostForDuration = requestCostForDuration;

            final double costPerRequestForDuration = totalRequestsForDuration == 0
                    ? 0
                    : requestCostForDuration / totalRequestsForDuration;

            final long totalRequestsTotal = requestCountTotal.longValue();
            final double requestCostTotalValue = Math.round(
                    requestCostTotal.longValue() / getRequestCostDenominator());
            final double jobCostTotalValue = Math.round(
                    jobCostTotal.longValue() / getRequestCostDenominator());
            final double totalCostTotal = requestCostTotalValue;
            final double costPerRequestTotal = totalRequestsTotal == 0
                    ? 0
                    : requestCostTotalValue / totalRequestsTotal;

            // The log line is throttled on consecutive idle windows so dev consoles stay quiet.
            // The publisher is NOT throttled — telemetry must emit a point every tick so an idle
            // cluster and a downed cluster are distinguishable on the receiving side.
            // An idle window is one with no requests AND no background work - a node doing
            // nothing but reindexing is not idle and should still log.
            final boolean idleWindow = totalRequestsForDuration == 0 && jobCostForDuration == 0;
            final boolean suppressLog = idleWindow && skipZeroRequests;
            skipZeroRequests = idleWindow;

            if (!suppressLog) {
                Logger.info("REQUEST TOKEN MONITOR >",
                        String.format(
                                "Last %ds: Reqs: %d, Tokens: %.2f, Avg Tokens: %.2f, Job Tokens: %.2f | Totals: Reqs: %d, Tokens: %.2f, Avg Tokens: %.2f, Job Tokens: %.2f",
                                requestCostTimeWindowSeconds,
                                totalRequestsForDuration,
                                totalCostForDuration,
                                costPerRequestForDuration,
                                jobCostForDuration,
                                totalRequestsTotal,
                                totalCostTotal,
                                costPerRequestTotal,
                                jobCostTotalValue));
            }

            if (publisher.isEnabled()) {
                publisher.publish(new RequestCostSnapshot(
                        // Try.getOrElse only fires on throw — also coalesce null returns since
                        // these lookups can transiently return null during early startup.
                        nullSafe(Try.of(ClusterFactory::getClusterId).getOrNull()),
                        nullSafe(Try.of(ConfigUtils::getServerId).getOrNull()),
                        Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                        requestCostTimeWindowSeconds,
                        totalRequestsForDuration,
                        totalCostForDuration,
                        costPerRequestForDuration,
                        totalRequestsTotal,
                        totalCostTotal,
                        costPerRequestTotal,
                        jobCostForDuration,
                        jobCostTotalValue));
            }
        } catch (Exception e) {
            Logger.warnAndDebug(this.getClass(), "Error logging request tokens:" + e.getMessage(), e);
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

    @Override
    public boolean isAccountingEnabled() {
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
        incrementCost(price, clazz, method, args, 1);
    }


    @Override
    public void incrementCost(Price price, Class clazz, String method, Object[] args,
            final int times) {
        if (times <= 0) {
            return;
        }
        final int cost = price.price * times;
        HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        if (request == null) {
            // Background work - reindex, scheduled publish, push publish, embedding
            // generation. It has no request to attach to, but it is still the customer's
            // work and still consumes the cluster, so it is counted here rather than
            // discarded. Deliberately NOT drained from the rate-limit bucket: a reindex
            // must never be able to throttle live traffic into 429s.
            jobCostForWindow.add(cost);
            jobCostTotal.add(cost);
            Logger.debug(RequestCostApiImpl.class,
                    () -> "REQUESTCOST job cost:" + cost + " , thread:" + Thread.currentThread().getName()
                            + " , method:" + clazz.getSimpleName() + "." + method);
            return;
        }
        Accounting accounting = resolveAccounting(request);

        if (accounting == Accounting.HTML) {
            Map<String, Object> load = createAccountingEntry(cost, clazz, method, args, accounting);
            getAccountList(request).add(load);
        }


        // log requests if a fuller accounting is enabled
        // Note: Cannot use lambdas with inline=true due to synthetic method access issues
        if (accounting.ordinal() > Accounting.HEADER.ordinal()) {
            Logger.info(RequestCostAdvice.class, ()->{
               return "<--- REQUESTCOST price:" + cost + " , method:" + clazz.getSimpleName() + "." + method;
            });
        } else {
            Logger.debug(RequestCostAdvice.class, ()-> {
                return "<--- REQUESTCOST price:" + cost + " , method:" + clazz.getSimpleName() + "." + method;

            });
        }
        int currentCost = getRequestCost(request);
        if (currentCost == 0) {
            this.requestCountForWindow.increment();
            this.requestCountTotal.increment();
        }

        request.setAttribute(REQUEST_COST_RUNNING_TOTAL_ATTRIBUTE, currentCost + cost);
        requestCostForWindow.add(cost);
        requestCostTotal.add(cost);
        bucket.drainFromBucket(cost);
    }

    private Map<String, Object> createAccountingEntry(int cost, Class clazz, String method,
            Object[] args, Accounting accounting) {

        return Map.of(COST, cost, METHOD, method, CLASS, clazz.getCanonicalName(), ARGS, args);

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

        // Rounded, but still formatted "%.2f": the header has always looked like "23.00" and
        // has always been a whole number. Keeping both the format and the integrality means
        // nothing downstream has to change when the internal Price scale moves.
        //
        // Floored at 1 when the request cost anything at all. Without this, any request under
        // half the denominator rounds to "0.00" - at the default of 10 that is every request
        // reading a single warm contentlet - and a request that did real work would report as
        // free. Only a genuinely zero-cost request reports 0.00. Window and lifetime totals
        // are unaffected: they sum raw units and divide once, so no resolution is lost there.
        final long reported = currentCost > 0
                ? Math.max(1L, Math.round(currentCost.doubleValue() / requestCostDenominator))
                : 0L;
        response.setHeader(REQUEST_COST_HEADER_NAME, String.format("%.2f", (double) reported));

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
