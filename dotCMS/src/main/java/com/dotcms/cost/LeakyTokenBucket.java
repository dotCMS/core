package com.dotcms.cost;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.concurrent.atomic.AtomicLong;

public class LeakyTokenBucket {


    final long refreshPerSecond;
    final long maximumBucketSize;
    final boolean enabled;
    private final AtomicLong lastRefill = new AtomicLong(0);
    private final AtomicLong tokenCount = new AtomicLong(0);


    LeakyTokenBucket(boolean enabled, long refreshPerSecond, long maximumBucketSize) {
        this.enabled = enabled;
        this.maximumBucketSize = maximumBucketSize;
        this.refreshPerSecond = refreshPerSecond;
    }

    LeakyTokenBucket() {
        this(
                Config.getBooleanProperty("RATE_LIMIT_ENABLED", false),
                Config.getLongProperty("RATE_LIMIT_REFRESH_PER_SECOND", 100),
                Config.getLongProperty("RATE_LIMIT_MAX_BUCKET_SIZE", 10000)
        );
    }


    boolean allow() {
        refillTokens();
        long currentCount = getTokenCount();

        if (enabled && currentCount <= 0) {
            Logger.debug(this.getClass(),
                    "Rate limited - no request tokens, refreshing" + refreshPerSecond + " per second");
            return false;
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
            long tokensToAdd = elapsedTimeSecs * refreshPerSecond;
            tokenCount.updateAndGet(current ->
                    Math.min(maximumBucketSize, current + tokensToAdd));
        }
    }


    long getTokenCount() {

        return Math.min(tokenCount.get(), maximumBucketSize);

    }

    long getLastRefillTime() {

        return lastRefill.get();


    }


    void drainFromBucket(long drainTokens) {

        tokenCount.set(Math.max(getTokenCount() - drainTokens, 0));

    }


}
