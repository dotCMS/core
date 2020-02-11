package com.dotcms.cache.lettuce;

import java.io.Closeable;
import java.io.IOException;
import io.lettuce.core.api.StatefulRedisConnection;

public class RedisConnection implements Closeable {

    
    StatefulRedisConnection<String,Object> connection;
    

    
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

}
