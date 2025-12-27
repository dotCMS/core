package com.dotcms.cost;

public interface LeakyTokenBucket {


    /**
     * Is the LeakyTokenBucket rate limiter enabled? If so, requests will be limited based upon the bucket parameters
     * Configure by setting: RATE_LIMIT_ENABLED Defaults to false
     *
     * @return
     */
    boolean isEnabled();

    /**
     * How big a bucket does an installation get? Configure by setting: RATE_LIMIT_MAX_BUCKET_SIZE Defaults to: 10,000
     *
     * @return
     */
    long getMaximumBucketSize();

    /**
     * How quickly does an installation refill its bucket? Configure by setting: RATE_LIMIT_REFILL_PER_SECOND Defaults
     * to: 100 (tokens per sec)
     *
     * @return
     */
    long getRefillPerSecond();

    /**
     * Does token calculations and returns whether to allow a request.
     *
     * @return
     */
    boolean allow();

    /**
     * Returns the number of tokens remaining in the bucket
     *
     * @return
     */
    long getTokenCount();

    /**
     * Returns the last time the tokens were refilled
     *
     * @return
     */
    long getLastRefillTime();

    /**
     * decrements the number of tokens in the bucket
     *
     * @param drainTokens
     */
    void drainFromBucket(long drainTokens);
}
