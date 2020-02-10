package com.dotcms.cache.lettuce;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.CompressionCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.vavr.Function0;
import io.vavr.control.Try;

public class LettuceClient {
    private final List<RedisURI> redisUris = Arrays.asList(Config.getStringArrayProperty("redis.connection.uris", new String[0]))
                    .stream().map(u -> RedisURI.create(u)).collect(Collectors.toList());

    private int timeout = Config.getIntProperty("redis.server.timeout.ms", 10000);

    private final GenericObjectPool<StatefulRedisMasterReplicaConnection<String, Object>> pool;

    private LettuceClient() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(2);
        config.setMaxIdle(5);
        config.setMaxTotal(20);


        pool = ConnectionPoolSupport.createGenericObjectPool(() -> {
            RedisClient lettuceClient = RedisClient.create();
            
            StatefulRedisMasterReplicaConnection<String, Object> connection = MasterReplica.connect(lettuceClient,
                            CompressionCodec.valueCompressor(new DotObjectCodec(), CompressionCodec.CompressionType.GZIP),
                            redisUris);
            connection.setReadFrom(ReadFrom.MASTER_PREFERRED);
            if (timeout > 0) {
                connection.setTimeout(Duration.ofMillis(timeout));
            }
            return connection;
        }, config, true);

    }
    
    public static Function0<LettuceClient> getInstance = Function0.of(LettuceClient::new).memoized();


    StatefulRedisConnection<String, Object> get() {

        return Try.of(() -> pool.borrowObject()).getOrElseThrow(e -> new DotRuntimeException(e));

    }
}
