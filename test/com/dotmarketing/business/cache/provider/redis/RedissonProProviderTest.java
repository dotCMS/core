package com.dotmarketing.business.cache.provider.redis;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.io.IOException;

/**
 * Created by jasontesser on 10/4/17.
 * The test assumes Redis is up and running
 */
public class RedissonProProviderTest {

    RedissonProProvider redissonProProvider = new RedissonProProvider();

    @Before
    public void setUp() throws Exception {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        config.setRegistrationKey("I/4PNQKRIUfGjmu3KnL2DwbuYMZ+pB0RWhQ/fg14W8rRfB2zrb3XAFhaN/ePnfhwaboDdpLVgQiu28lakhi0Lth1tWbzuEv9F3qqKka/zKV5NYs0wxG4+crM5NNUSlyNVvwvaPn7ng0TWlMuM3Bvq9HSVW1jLbC7kPmh5teQ8wA=\n");
        redissonProProvider.init(config);
    }

    @After
    public void tearDown() throws Exception {
        redissonProProvider.shutdown();
    }

    @Test
    public void put() throws Exception {
        redissonProProvider.put("group1", "key1", 1);
        Assert.assertEquals(redissonProProvider.get("group1", "key1"),1);
    }

    @Test
    public void get() throws Exception {
    }

    @Test
    public void remove() throws Exception {
    }

    @Test
    public void remove1() throws Exception {
    }

    @Test
    public void removeAll() throws Exception {
    }

    @Test
    public void getKeys() throws Exception {
    }

    @Test
    public void getGroups() throws Exception {
    }

}