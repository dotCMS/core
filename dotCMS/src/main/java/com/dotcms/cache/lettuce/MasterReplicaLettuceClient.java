package com.dotcms.cache.lettuce;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.CompressionCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DirContextDnsResolver;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.vavr.control.Try;

public enum MasterReplicaLettuceClient implements LettuceClient{



    INSTANCE;

    private final List<RedisURI> redisUris =
                    Arrays.asList(Config.getStringArrayProperty("REDIS_LETTUCECLIENT_URLS", new String[] {"redis://password@oboxturbo"}))
                                    .stream().map(u -> RedisURI.create(u)).collect(Collectors.toList());

    private final int timeout        = Config.getIntProperty("REDIS_LETTUCECLIENT_TIMEOUT_MS", 3000);

    

    
    public LettuceConnectionWrapper get() {
        return new LettuceConnectionWrapper(getConnection());

    }
    
    private Optional<StatefulRedisConnection<String, Object>> connection=Optional.empty();

    
    private StatefulRedisConnection<String, Object> getConnection() {

        
        if(connection.isPresent()) {
            return connection.get();
        }
        
        
        
        final DefaultClientResources clientResources =
                        DefaultClientResources.builder().dnsResolver(new DirContextDnsResolver()) // Does not cache DNS
                                                                                                  // lookups
                                        .build();

        final RedisClient lettuceClient = RedisClient.create(clientResources);

        StatefulRedisMasterReplicaConnection<String, Object> connection = MasterReplica.connect(lettuceClient,
                        CompressionCodec.valueCompressor(new DotObjectCodec(), CompressionCodec.CompressionType.GZIP),
                        redisUris);
        connection.setReadFrom(ReadFrom.REPLICA_PREFERRED);
        if (timeout > 0) {
            connection.setTimeout(Duration.ofMillis(timeout));
        }
        return connection;

    }
}


