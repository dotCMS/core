package com.dotcms.cost;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.Lazy;
import io.vavr.control.Try;
import java.util.concurrent.atomic.AtomicLong;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Transaction;

public class LeakyTokenBucket {


    final long refreshPerSecond;
    final long maximumBucketSize;
    final boolean enabled;
    private final String KEY_COUNT, LAST_REFRESH_TIME;
    private final AtomicLong lastRefill = new AtomicLong(0);
    private final AtomicLong tokenCount = new AtomicLong(0);


    LeakyTokenBucket(boolean enabled, long refreshPerSecond, long maximumBucketSize) {
        this.enabled = enabled;
        this.maximumBucketSize = maximumBucketSize;
        this.refreshPerSecond = refreshPerSecond;
        String clusterId = Try.of(ClusterFactory::getClusterId).getOrElse("unknown");
        KEY_COUNT = "KEY_COUNT_" + clusterId;
        LAST_REFRESH_TIME = "LAST_REFRESH_TIME_" + clusterId;


    }

    LeakyTokenBucket() {
        this(
                Config.getBooleanProperty("RATE_LIMIT_ENABLED", true),
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
        if (jedisPool.get() == null) {
            return Math.min(tokenCount.get(), maximumBucketSize);
        }
        try (Jedis jedis = jedisPool.get().getResource()) {
            return Try.of(() -> Long.parseLong(jedis.get(KEY_COUNT))).getOrElse(maximumBucketSize);
        }
    }

    long getLastRefillTime() {
        if (jedisPool.get() == null) {
            return lastRefill.get();
        }
        try (Jedis jedis = jedisPool.get().getResource()) {
            return Try.of(() -> Long.parseLong(jedis.get(LAST_REFRESH_TIME))).getOrElse(System.currentTimeMillis());
        }
    }


    void drainFromBucket(long drainTokens) {
        if (jedisPool.get() == null) {
            tokenCount.set(Math.max(getTokenCount() - drainTokens, 0));
            return;
        }
        try (Transaction trans = jedisPool.get().getResource().multi()) {
            trans.set(LAST_REFRESH_TIME, String.valueOf(System.currentTimeMillis()));
            trans.set(KEY_COUNT, String.valueOf(tokenCount));
            trans.exec();
        }


    }

    final Lazy<String> tokenBucketCountKey = "tokenBucketCount" + APILocator.getServerAPI().

    final Lazy<JedisPool> jedisPool = Lazy.of(() -> {

        Logger.info(this.getClass(), "*** Initializing Redis[" + this.getClass().getSimpleName() + "].");

        //Reading the configuration settings
        String host = Config.getStringProperty("RATE_LIMIT_REDIS_HOST",
                System.getenv("TOMCAT_REDIS_SESSION_HOST"));
        if (!UtilMethods.isSet(host)) {
            return null;
        }

        int port = Config.getIntProperty("RATE_LIMIT_REDIS_PORT",
                Try.of(() -> Integer.parseInt(System.getenv("TOMCAT_REDIS_SESSION_PORT")))
                        .getOrElse(Protocol.DEFAULT_PORT));

        int timeout = Config.getIntProperty("RATE_LIMIT_REDIS_TIMEOUT",
                Try.of(() -> Integer.parseInt(System.getenv("TOMCAT_REDIS_SESSION_PORT"))).getOrElse(100));
        int maxClients = Config.getIntProperty("RATE_LIMIT_REDIS_CONNECTIONS",
                Try.of(() -> Integer.parseInt(System.getenv("TOMCAT_REDIS_MAX_CONNECTIONS"))).getOrElse(100));
        int maxIdle = Config.getIntProperty("RATE_LIMIT_REDIS_MAX_IDLE",
                Try.of(() -> Integer.parseInt(System.getenv("TOMCAT_REDIS_MAX_IDLE_CONNECTIONS"))).getOrElse(20));
        int minIdle = Config.getIntProperty("RATE_LIMIT_REDIS_MIN_IDLE",
                Try.of(() -> Integer.parseInt(System.getenv("TOMCAT_REDIS_MIN_IDLE_CONNECTIONS"))).getOrElse(10));

        boolean blockExhausted = Config.getBooleanProperty("RATE_LIMIT_REDIS_BLOCK_WHEN_EXHAUSTED", false);
        String redisPass = Config.getStringProperty("RATE_LIMIT_REDIS_PASSWORD",
                System.getenv("TOMCAT_REDIS_SESSION_PASSWORD"));

        //Set the read configuration
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(maxClients);
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMinIdle(minIdle);
        jedisPoolConfig.setBlockWhenExhausted(blockExhausted);

        return UtilMethods.isSet(redisPass)
                ? new JedisPool(jedisPoolConfig, host, port, timeout, redisPass)
                : new JedisPool(jedisPoolConfig, host, port, timeout);


    });

}
