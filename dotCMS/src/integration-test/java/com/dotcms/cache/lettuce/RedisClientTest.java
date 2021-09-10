package com.dotcms.cache.lettuce;

import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for {@link RedisClient}
 * @author jsanca
 */
public class RedisClientTest {

    @BeforeClass
    public static void startup() throws Exception {
        final RedisClient<String, Object> redisClient = RedisClientFactory.getClient("test");

        if (redisClient.ping()) {

            redisClient.flushAll();
        }
    }

    /**
     * Method to test: {@link RedisClientFactory#getClient(String)}
     * Given Scenario: ask twice for the same client
     * ExpectedResult: both clients should be the same
     *
     */
    @Test
    public void test_client_is_singleton() throws Exception {

        final RedisClient<String, Object> redisClient1 = RedisClientFactory.getClient("test");
        final RedisClient<String, Object> redisClient2 = RedisClientFactory.getClient("test");

        Assert.assertEquals(redisClient1, redisClient2);
    }

    /**
     * Method to test: ping, get, set, delete
     * Given Scenario: test a crud on redis cache
     * ExpectedResult: a value can be inserted, updated, queried and deleted
     */
    @Test
    public void test_client_crud_if_running() throws Exception {

        final RedisClient<String, Object> redisClient = RedisClientFactory.getClient("test");

        if (redisClient.ping()) {

            final String key1 = "key1";
            final String value1 = "value1";

            // SET
            final SetResult result1 = redisClient.set(key1, value1);
            Assert.assertNotNull(result1);
            Assert.assertEquals(SetResult.SUCCESS, result1);

            final Object valueRecovery1 = redisClient.get(key1);
            Assert.assertEquals(value1, valueRecovery1);

            // OVERRIDE
            final String value2 = "value2";
            final SetResult result2 = redisClient.set(key1, value2);
            Assert.assertNotNull(result2);
            Assert.assertEquals(SetResult.SUCCESS, result2);

            final Object valueRecovery2 = redisClient.get(key1);
            Assert.assertEquals(value2, valueRecovery2);

            // DELETE
            final Object resultDelete = redisClient.delete(key1);
            Assert.assertNotNull(resultDelete);
            Assert.assertEquals(value2, resultDelete);

            // Check if already removed
            final Object valueRecovery3 = redisClient.get(key1);
            Assert.assertNull(valueRecovery3);

        } else {

            Logger.info(this, "Redis is not running skipping the test");
        }
    }

    /**
     * Method to test: ping, set (with ttl)
     * Given Scenario: test insert with ttl, to see if expires
     * ExpectedResult: test if the value is being removed after a three seconds
     */
    @Test
    public void test_client_set_ttl() throws Exception {

        final RedisClient<String, Object> redisClient = RedisClientFactory.getClient("test");

        if (redisClient.ping()) {

            final String key1 = "key1";
            final String value1 = "value1";

            // SET
            redisClient.set(key1, value1, DateUtil.TWO_SECOND_MILLIS);

            // read just now
            final Object valueRecovery1 = redisClient.get(key1);
            Assert.assertEquals(value1, valueRecovery1);

            DateUtil.sleep(DateUtil.THREE_SECOND_MILLIS);

            // after 3 seconds should be already removed
            final Object valueRecovery2 = redisClient.get(key1);
            Assert.assertNull(valueRecovery2);

        } else {

            Logger.info(this, "Redis is not running skipping the test");
        }
    }

    /**
     * Method to test: ping, setIfAbsent
     * Given Scenario: test insert a value twice (but the second time won't be overrided b/c the value is already added before)
     * ExpectedResult: the second call to setIfAbsent, does not overrides the value
     */
    @Test
    public void test_client_setIfAbsent() throws Exception {

        final RedisClient<String, Object> redisClient = RedisClientFactory.getClient("test");

        if (redisClient.ping()) {

            final String key1 = "key-absent1";
            final String value1 = "value1";

            // SET
            redisClient.setIfAbsent(key1, value1);

            final Object valueRecovery1 = redisClient.get(key1);
            Assert.assertEquals(value1, valueRecovery1);

            final String value2 = "value2";
            redisClient.setIfAbsent(key1, value2);
            final Object valueRecovery2 = redisClient.get(key1);
            Assert.assertNotNull(valueRecovery2);
            Assert.assertEquals(value1, valueRecovery1);
            Assert.assertNotEquals(value2, valueRecovery2);
        } else {

            Logger.info(this, "Redis is not running skipping the test");
        }
    }

    /**
     * Method to test: ping, setIfPresent
     * Given Scenario: the first set won't work since the key does not exists yet, the second call will work
     * ExpectedResult: the second call to setIfPresent, will work
     */
    @Test
    public void test_client_setIfPresent() throws Exception {

        final RedisClient<String, Object> redisClient = RedisClientFactory.getClient("test");

        if (redisClient.ping()) {

            final String key1 = "key-present1";
            final String value1 = "value1";

            // SET this should not works
            redisClient.setIfPresent(key1, value1);

            final Object valueRecovery1 = redisClient.get(key1);
            Assert.assertNull(valueRecovery1);

            redisClient.set(key1, value1);
            final Object valueRecovery2 = redisClient.get(key1);
            Assert.assertNotNull(valueRecovery2);
            Assert.assertEquals(value1, valueRecovery2);

            final String value2 = "value2";
            redisClient.setIfPresent(key1, value2);
            final Object valueRecovery3 = redisClient.get(key1);
            Assert.assertNotNull(valueRecovery3);
            Assert.assertEquals(value2, valueRecovery3);
            Assert.assertNotEquals(value1, valueRecovery3);
        } else {

            Logger.info(this, "Redis is not running skipping the test");
        }
    }
}
