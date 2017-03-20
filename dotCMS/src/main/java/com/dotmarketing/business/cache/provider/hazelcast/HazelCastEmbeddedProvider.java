package com.dotmarketing.business.cache.provider.hazelcast;

import com.dotmarketing.util.Logger;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;


/**
 * Created by jasontesser on 3/14/17.
 */
public class HazelCastEmbeddedProvider extends HazelCastClientProvider {

    HazelcastInstance hazel = null;
    private Boolean isInitialized = false;

    @Override
    public void init() throws Exception {
        Logger.info(this, "Setting Up HazelCast Config");
        XmlConfigBuilder builder = new XmlConfigBuilder("hazelcast-embedded.xml");
        hazel = Hazelcast.newHazelcastInstance(builder.build());
        isInitialized = true;
    }
}


