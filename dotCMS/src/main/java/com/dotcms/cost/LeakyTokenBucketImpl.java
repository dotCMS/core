package com.dotcms.cost;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LeakyTokenBucketImpl implements LeakyTokenBucket {


    final long refillPerSecond;
    final long maximumBucketSize;
    final boolean enabled;
    private final AtomicLong lastRefill = new AtomicLong(0);
    private final AtomicLong tokenCount = new AtomicLong(0);
    String REQUEST_COST_HEADER_TOKEN_MAX = "x-dotratelimit-toks-max";

    LeakyTokenBucketImpl(boolean enabled, long refillPerSecond, long maximumBucketSize) {
        this.enabled = enabled;
        this.maximumBucketSize = maximumBucketSize;
        this.refillPerSecond = refillPerSecond;

        Logger.info(this.getClass(),
                "Rate limiting enabled: " + enabled + ", refill per second: " + refillPerSecond + ", max bucket size: "
                        + maximumBucketSize);
    }

    LeakyTokenBucketImpl() {
        this(
                Config.getBooleanProperty("RATE_LIMIT_ENABLED", false) && APILocator.getRequestCostAPI()
                        .isAccountingEnabled(),
                Config.getLongProperty("RATE_LIMIT_REFILL_PER_SECOND", 500),
                Config.getLongProperty("RATE_LIMIT_MAX_BUCKET_SIZE", 10000)
        );
    }

    @Override
    public Optional<Tuple2<String, String>> getHeaderInfo() {
        if (!enabled) {
            return Optional.empty();
        }
        return Optional.of(Tuple.of(REQUEST_COST_HEADER_TOKEN_MAX, getTokenCount() + "/" + getMaximumBucketSize()));


    }




    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public long getMaximumBucketSize() {
        return maximumBucketSize;
    }

    @Override
    public long getRefillPerSecond() {
        return refillPerSecond;
    }

    @Override
    public boolean allow() {
        refillTokens();
        long currentCount = getTokenCount();

        if (currentCount <= 0) {
            if (enabled) {
                Logger.warnEvery(this.getClass(), "RATE_LIMIT_HIT",
                        "Rate limit (enabled) - max tokens:" + maximumBucketSize + ", refilling @ " + refillPerSecond
                                + "/sec", 10000);
                return false;
            } else {
                Logger.warnEvery(this.getClass(), "RATE_LIMIT_HIT",
                        "Rate limit (disabled) - max tokens:" + maximumBucketSize + ", refilling @ " + refillPerSecond
                                + "/sec", 10000);
            }
        }

        Logger.debug(this.getClass(), "Request tokens available: " + currentCount);
        return true;
    }


    private void refillTokens() {
        long currentTime = System.currentTimeMillis();
        long lastRefillTime = getLastRefillTime();

        long elapsedTimeSecs = (currentTime - lastRefillTime) / 1000;
        if (elapsedTimeSecs <= 0) {
            return; // Not enough time passed
        }

        // Only update if we win the race
        if (lastRefill.compareAndSet(lastRefillTime, currentTime)) {
            long tokensToAdd = elapsedTimeSecs * refillPerSecond;
            tokenCount.updateAndGet(current ->
                    Math.min(maximumBucketSize, current + tokensToAdd));
        }
    }

    @Override
    public long getTokenCount() {

        return Math.min(tokenCount.get(), maximumBucketSize);

    }

    @Override
    public long getLastRefillTime() {

        return lastRefill.get();


    }

    @Override
    public void drainFromBucket(long drainTokens) {

        long tokensRemaining = tokenCount.updateAndGet(current ->
                Math.max(Math.min(current, maximumBucketSize) - drainTokens, 0)
        );

        // we could throw an OutOfTokensException runtime exception here and
        // catch it higher up in the endpoints to deliver a custom error, killing the current request
        // otherwise, the next request will be blocked.
        /*

        if (tokensRemaining == 0 && enabled) {
            throw new OutOfTokensException();
        }
        */


    }


}
