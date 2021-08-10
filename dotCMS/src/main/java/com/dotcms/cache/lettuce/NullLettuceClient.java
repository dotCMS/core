package com.dotcms.cache.lettuce;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class NullLettuceClient<K, V> implements RedisClient {

    @Override
    public StatefulRedisConnection<K, V> getConn() {
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
        return null;
    }

    @Override
    public void set(Object key, Object value, long ttlMillis) {

    }

    @Override
    public Future<String> setAsync(Object key, Object value) {
        return null;
    }

    @Override
    public Future<String> setAsync(Object key, Object value, long ttlMillis) {
        return null;
    }

    @Override
    public long addMembers(Object key, Object[] values) {
        return 0;
    }

    @Override
    public Future<Long> addAsyncMembers(Object key, Object[] values) {
        return null;
    }

    @Override
    public SetResult setIfAbsent(Object key, Object value) {
        return null;
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
        return null;
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
    public long delete(Object[] keys) {
        return 0;
    }

    @Override
    public Future<Long> deleteNonBlocking(Object[] keys) {
        return null;
    }

    @Override
    public String flushAll() {
        return null;
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
        return null;
    }

    @Override
    public Set fieldsHash(Object key) {
        return null;
    }

    @Override
    public List<Map.Entry> getHash(Object key, Object[] fields) {
        return null;
    }

    @Override
    public SetResult setHash(Object key, Map map) {
        return null;
    }

    @Override
    public SetResult setHash(Object key, Object field, Object value) {
        return null;
    }

    @Override
    public long delete(Object key, Object[] fields) {
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
        return null;
    }

    @Override
    public Future<Long> incrementAsync(Object key, long amount) {
        return null;
    }

    @Override
    public void scanKeys(String matchesPattern, int keyBatchingSize, Consumer keyConsumer) {

    }

}
