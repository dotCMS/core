package com.dotcms.cost;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LeakyTokenBucketImpl implements LeakyTokenBucket {


    final Long testingRefillPerSecond;
    final Long testingMaximumBucketSize;
    final Boolean testingEnabled;

    private final AtomicLong lastRefill = new AtomicLong(0);
    private final AtomicLong tokenCount = new AtomicLong(0);
    String REQUEST_COST_HEADER_TOKEN_MAX = "x-dotratelimit-toks-max";

    @VisibleForTesting
    LeakyTokenBucketImpl(boolean enabled, long refillPerSecond, long maximumBucketSize) {
        this.testingEnabled = enabled;
        this.testingMaximumBucketSize = maximumBucketSize;
        this.testingRefillPerSecond = refillPerSecond;

        Logger.info(this.getClass(),
                "Rate limiting enabled: " + enabled + ", refill per second: " + refillPerSecond + ", max bucket size: "
                        + maximumBucketSize);
    }

    LeakyTokenBucketImpl() {
        this.testingEnabled = null;
        this.testingMaximumBucketSize = null;
        this.testingRefillPerSecond = null;
    }


    @Override
    public Optional<Tuple2<String, String>> getHeaderInfo() {

        if (!Config.getBooleanProperty("RATE_LIMIT_ADD_HEADER_INFO", true)) {
            return Optional.empty();
        }
        return Optional.of(Tuple.of(REQUEST_COST_HEADER_TOKEN_MAX, getTokenCount() + "/" + getMaximumBucketSize()));

    }


    @Override
    public boolean isEnabled() {
        return testingEnabled != null
                ? testingEnabled
                : Config.getBooleanProperty("RATE_LIMIT_ENABLED", false);
    }

    @Override
    public long getMaximumBucketSize() {
        return testingMaximumBucketSize != null
                ? testingMaximumBucketSize
                : Config.getLongProperty("RATE_LIMIT_MAX_BUCKET_SIZE", 10000);
    }

    @Override
    public long getRefillPerSecond() {
        return testingRefillPerSecond != null
                ? testingRefillPerSecond
                : Config.getLongProperty("RATE_LIMIT_REFILL_PER_SECOND", 500);
    }

    @Override
    public boolean allow() {
        refillTokens();
        long currentCount = getTokenCount();

        if (currentCount <= 0) {
            if (isEnabled()) {
                Logger.warnEvery(this.getClass(), "RATE_LIMIT_HIT",
                        "Rate limit (enabled) - max tokens:" + getMaximumBucketSize() + ", refilling @ "
                                + getRefillPerSecond()
                                + "/sec", 10000);
                return false;
            } else {
                Logger.warnEvery(this.getClass(), "RATE_LIMIT_HIT",
                        "Rate limit (disabled) - max tokens:" + getMaximumBucketSize() + ", refilling @ "
                                + getRefillPerSecond()
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
            long tokensToAdd = elapsedTimeSecs * getRefillPerSecond();
            tokenCount.updateAndGet(current ->
                    Math.min(getMaximumBucketSize(), current + tokensToAdd));
        }
    }

    @Override
    public long getTokenCount() {

        return Math.min(tokenCount.get(), getMaximumBucketSize());

    }

    @Override
    public long getLastRefillTime() {

        return lastRefill.get();


    }

    @Override
    public void drainFromBucket(long drainTokens) {

        long tokensRemaining = tokenCount.updateAndGet(current ->
                Math.max(Math.min(current, getMaximumBucketSize()) - drainTokens, 0)
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
