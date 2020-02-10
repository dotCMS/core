package com.dotcms.cache.lettuce;

import java.io.Closeable;
import java.io.IOException;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;

public class RedisConnection implements Closeable {

    
    StatefulRedisMasterReplicaConnection<String,Object> connection;
    
    RedisConnection()
    
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        
    }

}
