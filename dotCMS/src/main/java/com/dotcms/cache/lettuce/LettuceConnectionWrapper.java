package com.dotcms.cache.lettuce;

import java.io.Closeable;
import java.util.Optional;
import com.dotmarketing.business.DotStateException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * this wraps our lettuce connection so we can check for connectivity and still use the closable
 * interface
 * 
 * @author will
 *
 */
public class LettuceConnectionWrapper implements Closeable {

    private final Optional<StatefulRedisConnection<String, Object>> conn;

    LettuceConnectionWrapper(StatefulRedisConnection<String, Object> conn) {
        this.conn = Optional.ofNullable(conn);
    }

    LettuceConnectionWrapper() {
        this.conn = Optional.empty();
    }

    StatefulRedisConnection<String, Object> getConnection() {
        checkConnection();
        return conn.get();
    }

    RedisAsyncCommands<String, Object> async() {
        checkConnection();
        return conn.get().async();
    }

    RedisCommands<String, Object> sync() {
        checkConnection();
        return conn.get().sync();
    }

    void checkConnection() {
        if (!conn.isPresent()) {
            throw new DotStateException("redis is not connected");
        }
    }

    boolean connected() {
        return this.conn.isPresent();
    }

    @Override
    public void close() {
        if (connected()) {
            conn.get().closeAsync();
        }

    }
    

    

}
