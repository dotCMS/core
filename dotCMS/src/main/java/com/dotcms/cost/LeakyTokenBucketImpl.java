package com.dotcms.cost;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.concurrent.atomic.AtomicLong;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LeakyTokenBucketImpl implements LeakyTokenBucket {


    final long refillPerSecond;
    final long maximumBucketSize;
    final boolean enabled;
    private final AtomicLong lastRefill = new AtomicLong(0);
    private final AtomicLong tokenCount = new AtomicLong(0);


    LeakyTokenBucketImpl(boolean enabled, long refillPerSecond, long maximumBucketSize) {
        this.enabled = enabled;
        this.maximumBucketSize = maximumBucketSize;
        this.refillPerSecond = refillPerSecond;
    }

    LeakyTokenBucketImpl() {
        this(
                Config.getBooleanProperty("RATE_LIMIT_ENABLED", false),
                Config.getLongProperty("RATE_LIMIT_REFILL_PER_SECOND", 100),
                Config.getLongProperty("RATE_LIMIT_MAX_BUCKET_SIZE", 10000)
        );
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

        if (enabled && currentCount <= 0) {
            Logger.debug(this.getClass(),
                    "Rate limited - no request tokens, refilling @ " + refillPerSecond + " per second");
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
