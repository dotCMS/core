package org.apache.velocity.runtime.pool.commons;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.velocity.runtime.parser.Parser;

// based upon https://dzone.com/articles/creating-object-pool-java
public class CommonsParserPool extends GenericObjectPool<Parser> {

    public CommonsParserPool(
            PooledObjectFactory<Parser> factory) {
        super(factory);
    }

    public CommonsParserPool(
            PooledObjectFactory<Parser> factory,
            GenericObjectPoolConfig<Parser> config) {
        super(factory, config);
    }

    public CommonsParserPool(
            PooledObjectFactory<Parser> factory,
            GenericObjectPoolConfig<Parser> config,
            AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }
}
