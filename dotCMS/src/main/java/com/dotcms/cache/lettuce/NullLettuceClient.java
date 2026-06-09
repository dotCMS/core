package com.dotcms.cache.lettuce;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Null Object implementation of {@link RedisClient}: every operation is a no-op that returns a safe,
 * non-null default. Used when Redis is unavailable/disabled so callers never NPE on Futures or collections.
 */
@SuppressWarnings("deprecation") // implements deprecated (but retained) RedisClient methods
public class NullLettuceClient<K, V> implements RedisClient {

    @Override
    public Object getConnection() {
        return null;
    }

    @Override
    public boolean isOpen(StatefulConnection connection) {
        return false;
    }

    @Override
    public boolean ping() {
        return false;
    }

    @Override
    public Object echo(Object msg) {
        return null;
    }

    @Override
    public SetResult set(Object key, Object value) {
        return SetResult.NO_CONN;
    }

    @Override
    public void set(Object key, Object value, long ttlMillis) {

    }

    @Override
    public Future<String> setAsync(Object key, Object value) {
        return ConcurrentUtils.constantFuture("ERROR");
    }

    @Override
    public Future<String> setAsync(Object key, Object value, long ttlMillis) {
        return ConcurrentUtils.constantFuture("ERROR");
    }

    @Override
    public long addMembers(Object key, Object... values) {
        return 0;
    }

    @Override
    public Future<Long> addAsyncMembers(Object key, Object... values) {
        return ConcurrentUtils.constantFuture(0L);
    }

    @Override
    public SetResult setIfAbsent(Object key, Object value) {
        return SetResult.NO_CONN;
    }

    @Override
    public Object setIfPresent(Object key, Object value) {
        return null;
    }

    @Override
    public Object get(Object key) {
        return null;
    }

    @Override
    public Set getMembers(Object key) {
        return Collections.emptySet();
    }

    @Override
    public long ttlMillis(Object key) {
        return 0;
    }

    @Override
    public Object delete(Object key) {
        return null;
    }

    @Override
    public long delete(Object... keys) {
        return 0;
    }

    @Override
    public Future<Long> deleteNonBlocking(Object... keys) {
        return ConcurrentUtils.constantFuture(0L);
    }

    @Override
    public void scanEachKey(String matchesPattern, int keyBatchingSize, Consumer keyConsumer) {

    }

    @Override
    public boolean existsHash(Object key, Object field) {
        return false;
    }

    @Override
    public Object getHash(Object key, Object field) {
        return null;
    }

    @Override
    public Map getHash(Object key) {
        return Collections.emptyMap();
    }

    @Override
    public Set fieldsHash(Object key) {
        return Collections.emptySet();
    }

    @Override
    public List<Map.Entry> getHash(Object key, Object... fields) {
        return Collections.emptyList();
    }

    @Override
    public SetResult setHash(Object key, Map map) {
        return SetResult.NO_CONN;
    }

    @Override
    public SetResult setHash(Object key, Object field, Object value) {
        return SetResult.NO_CONN;
    }

    @Override
    public long deleteHash(Object key, Object... fields) {
        return 0;
    }

    @Override
    public long incrementOne(Object key) {
        return 0;
    }

    @Override
    public long increment(Object key, long amount) {
        return 0;
    }

    @Override
    public Future<Long> incrementOneAsync(Object key) {
        return ConcurrentUtils.constantFuture(-1L);
    }

    @Override
    public Future<Long> incrementAsync(Object key, long amount) {
        return ConcurrentUtils.constantFuture(-1L);
    }

    @Override
    public long getIncrement (final Object key) {
        return -1;
    }

    @Override
    public void deleteFromPattern(String pattern) {

    }


    @Override
    public void scanKeys(String matchesPattern, int keyBatchingSize, Consumer keyConsumer) {

    }

}
